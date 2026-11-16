-- Cross-device draft sync, admin-managed project catalogue, and buyer enquiries.
--
-- Why a separate table instead of reusing finnest_user_drafts?
--   finnest_user_drafts is keyed by (user_id, device_id, journey_type), so two
--   devices create two unrelated rows. Last-write-wins on a whole document would
--   silently destroy work done on the other device.
--
-- finnest_draft_fields is keyed by (user_id, journey_type, field_key) — one row
--   per answer. Merge is per field, so editing the budget on a phone and the
--   visit notes on a laptop keeps both. This is the only shape that makes web
--   and Android genuinely interoperable, because Android already stores flat
--   string keys scoped by journey.

-- ---------------------------------------------------------------- draft fields
create table public.finnest_draft_fields (
  user_id uuid not null references auth.users(id) on delete cascade,
  journey_type text not null check (journey_type in ('new_property', 'resale_property')),
  field_key text not null check (length(field_key) between 1 and 120),
  value text not null default '' check (length(value) <= 20000),
  client_updated_at timestamptz not null,
  updated_at timestamptz not null default now(),
  primary key (user_id, journey_type, field_key)
);

comment on table public.finnest_draft_fields is
  'One row per journey answer, shared by the website and Android. Merge is per field.';
comment on column public.finnest_draft_fields.client_updated_at is
  'Timestamp from the writing device. Used for per-field last-write-wins; server updated_at is authoritative for ordering only.';

create index finnest_draft_fields_user_journey_idx
  on public.finnest_draft_fields (user_id, journey_type, updated_at desc);

alter table public.finnest_draft_fields enable row level security;
revoke all on public.finnest_draft_fields from public, anon, authenticated;
grant select, insert, update, delete on public.finnest_draft_fields to authenticated;
grant all on public.finnest_draft_fields to service_role;

create policy "Users read their own draft fields" on public.finnest_draft_fields
  for select to authenticated using ((select auth.uid()) = user_id);
create policy "Users create their own draft fields" on public.finnest_draft_fields
  for insert to authenticated with check ((select auth.uid()) = user_id);
create policy "Users update their own draft fields" on public.finnest_draft_fields
  for update to authenticated using ((select auth.uid()) = user_id)
  with check ((select auth.uid()) = user_id);
create policy "Users delete their own draft fields" on public.finnest_draft_fields
  for delete to authenticated using ((select auth.uid()) = user_id);

-- Visible to the writing user in realtime, but also to super admins supporting them.
-- Deliberately the narrowest admin surface: super_admin only, and it inherits the
-- AAL2 requirement inside finnest_has_admin_role.
create policy "Super admins read draft fields" on public.finnest_draft_fields
  for select to authenticated using (public.finnest_has_admin_role(array['super_admin']));

-- Server keeps updated_at honest so client clocks cannot reorder history.
create trigger finnest_draft_fields_touch before update on public.finnest_draft_fields
  for each row execute function public.finnest_touch_updated_at();

-- Merge guard: an older write must not overwrite a newer one. Clients send their
-- client_updated_at; a losing write is dropped by the trigger instead of raising,
-- so a slow offline device cannot fail loudly in the background sync loop.
create or replace function public.finnest_draft_fields_merge_guard() returns trigger
language plpgsql set search_path = '' as $$
begin
  if new.client_updated_at < old.client_updated_at then
    return null;
  end if;
  return new;
end;
$$;
create trigger finnest_draft_fields_merge_before
  before update on public.finnest_draft_fields
  for each row execute function public.finnest_draft_fields_merge_guard();

-- ------------------------------------------------------------- project catalogue
-- Admin-managed. Shown on the website and in Android as a starting catalogue that
-- buyers can shortlist from; buyer shortlists stay private in their own draft.
create table public.finnest_projects (
  id uuid primary key default gen_random_uuid(),
  name text not null check (length(trim(name)) between 1 and 120),
  developer text check (developer is null or length(developer) <= 120),
  city text check (city is null or length(city) <= 80),
  locality text check (locality is null or length(locality) <= 120),
  configuration text check (configuration is null or length(configuration) <= 120),
  price_from_label text check (price_from_label is null or length(price_from_label) <= 60),
  possession_label text check (possession_label is null or length(possession_label) <= 60),
  approval_note text check (approval_note is null or length(approval_note) <= 200),
  summary text check (summary is null or length(summary) <= 2000),
  status text not null default 'draft' check (status in ('draft', 'published', 'archived')),
  sort_order integer not null default 100,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
create index finnest_projects_published_idx on public.finnest_projects (status, sort_order, name);

alter table public.finnest_projects enable row level security;
revoke all on public.finnest_projects from public, anon, authenticated;
grant select on public.finnest_projects to anon, authenticated;
grant insert, update, delete on public.finnest_projects to authenticated;
grant all on public.finnest_projects to service_role;

create policy "Anyone reads published projects" on public.finnest_projects
  for select to anon, authenticated using (status = 'published');
create policy "Content editors read all projects" on public.finnest_projects
  for select to authenticated using (public.finnest_has_admin_role(array['super_admin', 'content_manager']));
create policy "Content editors create projects" on public.finnest_projects
  for insert to authenticated with check (public.finnest_has_admin_role(array['super_admin', 'content_manager']));
create policy "Content editors update projects" on public.finnest_projects
  for update to authenticated using (public.finnest_has_admin_role(array['super_admin', 'content_manager']))
  with check (public.finnest_has_admin_role(array['super_admin', 'content_manager']));
create policy "Content editors delete projects" on public.finnest_projects
  for delete to authenticated using (public.finnest_has_admin_role(array['super_admin', 'content_manager']));

create trigger finnest_projects_touch before update on public.finnest_projects
  for each row execute function public.finnest_touch_updated_at();

-- ------------------------------------------------------------------- enquiries
-- A buyer asking FINNEST to be contacted about a specific project or a topic.
-- Distinct from support tickets: support is "something is wrong", enquiry is
-- "I want to buy / I want a callback", with an explicit consent record.
create table public.finnest_enquiries (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  project_id uuid references public.finnest_projects(id) on delete set null,
  topic text not null default 'general' check (topic in ('general', 'project', 'expert', 'loan', 'legal')),
  subject text not null check (length(trim(subject)) between 1 and 150),
  message text not null check (length(trim(message)) between 1 and 5000),
  contact_name text check (contact_name is null or length(contact_name) <= 120),
  contact_phone text check (contact_phone is null or length(contact_phone) <= 20),
  contact_email text check (contact_email is null or length(contact_email) <= 254),
  consent_contact boolean not null default false,
  status text not null default 'new' check (status in ('new', 'in_progress', 'contacted', 'closed', 'spam')),
  admin_note text check (admin_note is null or length(admin_note) <= 2000),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
create index finnest_enquiries_status_idx on public.finnest_enquiries (status, created_at desc);
create index finnest_enquiries_user_idx on public.finnest_enquiries (user_id, created_at desc);

alter table public.finnest_enquiries enable row level security;
revoke all on public.finnest_enquiries from public, anon, authenticated;
grant select, insert on public.finnest_enquiries to authenticated;
grant update, delete on public.finnest_enquiries to authenticated;
grant all on public.finnest_enquiries to service_role;

create policy "Users read their own enquiries" on public.finnest_enquiries
  for select to authenticated using (user_id = (select auth.uid()));
-- A buyer cannot set status, admin_note or spoof another user's identity.
create policy "Users create their own enquiries" on public.finnest_enquiries
  for insert to authenticated with check (
    user_id = (select auth.uid())
    and status = 'new'
    and admin_note is null
  );
create policy "Support managers read enquiries" on public.finnest_enquiries
  for select to authenticated using (public.finnest_has_admin_role(array['super_admin', 'support_manager']));
create policy "Support managers update enquiries" on public.finnest_enquiries
  for update to authenticated using (public.finnest_has_admin_role(array['super_admin', 'support_manager']))
  with check (public.finnest_has_admin_role(array['super_admin', 'support_manager']));

create trigger finnest_enquiries_touch before update on public.finnest_enquiries
  for each row execute function public.finnest_touch_updated_at();

-- --------------------------------------------------------------- realtime setup
-- Realtime is what removes the need to refresh. Adding a table to the publication
-- is idempotent-safe here and skipped on the disposable test database, which has
-- no supabase_realtime publication.
do $$
begin
  if exists (select 1 from pg_publication where pubname = 'supabase_realtime') then
    begin
      alter publication supabase_realtime add table public.finnest_draft_fields;
    exception when duplicate_object then null;
    end;
    begin
      alter publication supabase_realtime add table public.finnest_projects;
    exception when duplicate_object then null;
    end;
    begin
      alter publication supabase_realtime add table public.finnest_enquiries;
    exception when duplicate_object then null;
    end;
    begin
      alter publication supabase_realtime add table public.finnest_guidance;
    exception when duplicate_object then null;
    end;
    begin
      alter publication supabase_realtime add table public.finnest_experts;
    exception when duplicate_object then null;
    end;
  end if;
end $$;

-- Realtime needs UPDATE payloads to carry the full row for per-field merge on the
-- client. REPLICA IDENTITY FULL is the documented way to do that.
alter table public.finnest_draft_fields replica identity full;
alter table public.finnest_projects replica identity full;
alter table public.finnest_enquiries replica identity full;
