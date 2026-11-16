-- Reconcile three tables that exist in the production project but were created by
-- Lovable's editor (2026-11-12) and never committed back to this repository.
--
-- Why this file exists:
--   Anyone rebuilding the database from `supabase/migrations/` alone got a schema
--   that was missing finnest_journeys, finnest_documents and finnest_activity, so
--   code referencing them would have worked only against production. CI's
--   disposable-Postgres harness could not see them either, so their RLS was never
--   exercised.
--
-- Everything here is idempotent and matches what production already has, so running
-- it against the live project is a no-op. It also tightens grants: Lovable's default
-- handed `anon` and `authenticated` full DML (ALL) on all three tables. RLS still
-- gated the rows, but the repository's posture elsewhere is to grant only the verbs
-- the client actually needs, so this aligns them.

-- ------------------------------------------------------------------ journeys
-- One row per (buyer, journey type). Version bumping lives in the trigger below so
-- two devices cannot both write version = old + 1 and lose an increment.
create table if not exists public.finnest_journeys (
  user_id uuid not null references auth.users(id) on delete cascade,
  journey_type text not null check (journey_type in ('new', 'resale')),
  payload jsonb not null default '{}'::jsonb,
  current_step text,
  completed_steps text[] not null default '{}'::text[],
  version integer not null default 1,
  updated_by_device text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  primary key (user_id, journey_type)
);

create or replace function public.finnest_bump_journey_version() returns trigger
language plpgsql set search_path = '' as $$
begin
  new.version = old.version + 1;
  new.updated_at = now();
  return new;
end
$$;

drop trigger if exists finnest_journeys_bump on public.finnest_journeys;
create trigger finnest_journeys_bump before update on public.finnest_journeys
  for each row execute function public.finnest_bump_journey_version();

alter table public.finnest_journeys enable row level security;
revoke all on public.finnest_journeys from public, anon, authenticated;
grant select, insert, update, delete on public.finnest_journeys to authenticated;
grant all on public.finnest_journeys to service_role;

drop policy if exists "Users manage own journeys" on public.finnest_journeys;
create policy "Users manage own journeys" on public.finnest_journeys
  for all to authenticated
  using (user_id = (select auth.uid()))
  with check (user_id = (select auth.uid()));

drop policy if exists "Admins read journeys" on public.finnest_journeys;
create policy "Admins read journeys" on public.finnest_journeys
  for select to authenticated
  using (public.finnest_has_admin_role(array['super_admin', 'content_manager', 'support_manager', 'expert_manager']));

-- ------------------------------------------------------------------ documents
-- Metadata for files the buyer uploaded. The file itself lives in Storage; the
-- Storage policies in 20_storage_rls.sql are what actually protect the bytes.
create table if not exists public.finnest_documents (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  journey_type text not null default 'new' check (journey_type in ('new', 'resale')),
  path text not null,
  name text not null,
  uploaded_at timestamptz not null default now()
);

alter table public.finnest_documents enable row level security;
revoke all on public.finnest_documents from public, anon, authenticated;
grant select, insert, update, delete on public.finnest_documents to authenticated;
grant all on public.finnest_documents to service_role;

drop policy if exists "Users manage own documents" on public.finnest_documents;
create policy "Users manage own documents" on public.finnest_documents
  for all to authenticated
  using (user_id = (select auth.uid()))
  with check (user_id = (select auth.uid()));

drop policy if exists "Admins read documents" on public.finnest_documents;
create policy "Admins read documents" on public.finnest_documents
  for select to authenticated
  using (public.finnest_has_admin_role(array['super_admin', 'support_manager']));

-- -------------------------------------------------------------------- activity
-- Append-only event log. There is deliberately no INSERT-visible admin policy and
-- no UPDATE/DELETE policy: history must not be rewritable, not even by staff.
create table if not exists public.finnest_activity (
  id bigint generated always as identity primary key,
  user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  event text not null check (length(event) between 1 and 60),
  detail jsonb not null default '{}'::jsonb,
  platform text not null default 'web' check (platform in ('web', 'android')),
  occurred_at timestamptz not null default now()
);

create index if not exists finnest_activity_user_idx
  on public.finnest_activity (user_id, occurred_at desc);

alter table public.finnest_activity enable row level security;
revoke all on public.finnest_activity from public, anon, authenticated;
grant select, insert on public.finnest_activity to authenticated;
grant all on public.finnest_activity to service_role;

drop policy if exists "Users log own activity" on public.finnest_activity;
create policy "Users log own activity" on public.finnest_activity
  for insert to authenticated with check (user_id = (select auth.uid()));

drop policy if exists "Users read own activity" on public.finnest_activity;
create policy "Users read own activity" on public.finnest_activity
  for select to authenticated using (user_id = (select auth.uid()));

drop policy if exists "Admins read activity" on public.finnest_activity;
create policy "Admins read activity" on public.finnest_activity
  for select to authenticated
  using (public.finnest_has_admin_role(array['super_admin', 'content_manager', 'support_manager', 'expert_manager']));

-- ---------------------------------------------------------------- realtime
-- finnest_activity and finnest_journeys are already in the publication on
-- production. Adding them again is guarded so the disposable test database (which
-- has no supabase_realtime publication) is unaffected.
do $$
begin
  if exists (select 1 from pg_publication where pubname = 'supabase_realtime') then
    begin
      alter publication supabase_realtime add table public.finnest_journeys;
    exception when duplicate_object then null;
    end;
    begin
      alter publication supabase_realtime add table public.finnest_activity;
    exception when duplicate_object then null;
    end;
  end if;
end $$;

alter table public.finnest_journeys replica identity full;
