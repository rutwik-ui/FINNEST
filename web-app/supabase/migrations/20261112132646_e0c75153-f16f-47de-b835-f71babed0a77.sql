create table public.finnest_journeys (
  user_id uuid not null,
  journey_type text not null check (journey_type in ('new','resale')),
  payload jsonb not null default '{}'::jsonb,
  current_step text,
  completed_steps text[] not null default '{}',
  version integer not null default 1,
  updated_by_device text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  primary key (user_id, journey_type)
);
grant select, insert, update, delete on public.finnest_journeys to authenticated;
grant all on public.finnest_journeys to service_role;
alter table public.finnest_journeys enable row level security;
create policy "Users manage own journeys" on public.finnest_journeys for all to authenticated
  using (user_id = (select auth.uid())) with check (user_id = (select auth.uid()));
create policy "Admins read journeys" on public.finnest_journeys for select to authenticated
  using (public.finnest_has_admin_role(array['super_admin','content_manager','support_manager','expert_manager']));
create or replace function public.finnest_bump_journey_version() returns trigger language plpgsql set search_path to '' as $$
begin new.version = old.version + 1; new.updated_at = now(); return new; end $$;
create trigger finnest_journeys_bump before update on public.finnest_journeys for each row execute function public.finnest_bump_journey_version();

create table public.finnest_activity (
  id bigint generated always as identity primary key,
  user_id uuid not null default auth.uid(),
  event text not null check (char_length(event) between 1 and 60),
  detail jsonb not null default '{}'::jsonb,
  platform text not null default 'web' check (platform in ('web','android')),
  occurred_at timestamptz not null default now()
);
create index finnest_activity_user_idx on public.finnest_activity(user_id, occurred_at desc);
grant select, insert on public.finnest_activity to authenticated;
grant all on public.finnest_activity to service_role;
alter table public.finnest_activity enable row level security;
create policy "Users log own activity" on public.finnest_activity for insert to authenticated with check (user_id = (select auth.uid()));
create policy "Users read own activity" on public.finnest_activity for select to authenticated using (user_id = (select auth.uid()));
create policy "Admins read activity" on public.finnest_activity for select to authenticated
  using (public.finnest_has_admin_role(array['super_admin','content_manager','support_manager','expert_manager']));

create table public.finnest_documents (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null default auth.uid(),
  journey_type text not null default 'new',
  path text not null,
  name text not null,
  uploaded_at timestamptz not null default now()
);
grant select, insert, delete on public.finnest_documents to authenticated;
grant all on public.finnest_documents to service_role;
alter table public.finnest_documents enable row level security;
create policy "Users manage own documents" on public.finnest_documents for all to authenticated
  using (user_id = (select auth.uid())) with check (user_id = (select auth.uid()));
create policy "Admins read documents" on public.finnest_documents for select to authenticated
  using (public.finnest_has_admin_role(array['super_admin','support_manager']));

alter publication supabase_realtime add table public.finnest_journeys;
alter publication supabase_realtime add table public.finnest_activity;