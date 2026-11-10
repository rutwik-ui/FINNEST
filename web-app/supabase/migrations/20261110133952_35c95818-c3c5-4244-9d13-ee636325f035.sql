create or replace function public.finnest_has_admin_role(_roles text[])
returns boolean language sql stable security definer set search_path = public as $$
  select coalesce((auth.jwt() ->> 'aal') = 'aal2', false) and exists (
    select 1 from public.finnest_admin_roles r where r.user_id = auth.uid() and r.role = any(_roles));
$$;
revoke all on function public.finnest_has_admin_role(text[]) from public, anon;
grant execute on function public.finnest_has_admin_role(text[]) to authenticated;

create table public.finnest_profiles (
  user_id uuid primary key references auth.users(id) on delete cascade,
  email text,
  full_name text,
  last_seen_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
grant select, update on public.finnest_profiles to authenticated;
grant all on public.finnest_profiles to service_role;
alter table public.finnest_profiles enable row level security;
create policy "Users read own profile" on public.finnest_profiles for select to authenticated using (user_id = auth.uid());
create policy "Users update own profile" on public.finnest_profiles for update to authenticated using (user_id = auth.uid()) with check (user_id = auth.uid());
create policy "Admins read profiles" on public.finnest_profiles for select to authenticated using (public.finnest_has_admin_role(array['super_admin','content_manager','support_manager','expert_manager']));
create trigger finnest_profiles_touch before update on public.finnest_profiles for each row execute function public.finnest_touch_updated_at();

create or replace function public.finnest_handle_new_user() returns trigger
language plpgsql security definer set search_path = public as $$
begin
  insert into public.finnest_profiles(user_id, email) values (new.id, new.email) on conflict (user_id) do nothing;
  return new;
end $$;
create trigger finnest_on_auth_user_created after insert on auth.users for each row execute function public.finnest_handle_new_user();
insert into public.finnest_profiles(user_id, email) select id, email from auth.users on conflict do nothing;

create table public.finnest_experts (
  id uuid primary key default gen_random_uuid(),
  name text not null check (char_length(name) between 1 and 120),
  specialty text not null,
  city text,
  phone text,
  email text,
  bio text,
  status text not null default 'draft' check (status in ('draft','published')),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
grant select on public.finnest_experts to anon;
grant select, insert, update, delete on public.finnest_experts to authenticated;
grant all on public.finnest_experts to service_role;
alter table public.finnest_experts enable row level security;
create policy "Anyone reads published experts" on public.finnest_experts for select to anon, authenticated using (status = 'published');
create policy "Expert managers manage experts" on public.finnest_experts for all to authenticated using (public.finnest_has_admin_role(array['super_admin','expert_manager'])) with check (public.finnest_has_admin_role(array['super_admin','expert_manager']));
create trigger finnest_experts_touch before update on public.finnest_experts for each row execute function public.finnest_touch_updated_at();

create table public.finnest_support_tickets (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null default auth.uid(),
  subject text not null check (char_length(subject) between 1 and 150),
  message text not null check (char_length(message) between 1 and 5000),
  status text not null default 'open' check (status in ('open','in_progress','resolved')),
  admin_note text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
grant select, insert, update on public.finnest_support_tickets to authenticated;
grant all on public.finnest_support_tickets to service_role;
alter table public.finnest_support_tickets enable row level security;
create policy "Users create own tickets" on public.finnest_support_tickets for insert to authenticated with check (user_id = auth.uid() and status = 'open' and admin_note is null);
create policy "Users read own tickets" on public.finnest_support_tickets for select to authenticated using (user_id = auth.uid());
create policy "Support managers read tickets" on public.finnest_support_tickets for select to authenticated using (public.finnest_has_admin_role(array['super_admin','support_manager']));
create policy "Support managers update tickets" on public.finnest_support_tickets for update to authenticated using (public.finnest_has_admin_role(array['super_admin','support_manager'])) with check (public.finnest_has_admin_role(array['super_admin','support_manager']));
create trigger finnest_tickets_touch before update on public.finnest_support_tickets for each row execute function public.finnest_touch_updated_at();

grant select, insert, delete on public.finnest_admin_roles to authenticated;
create policy "Super admins read roles" on public.finnest_admin_roles for select to authenticated using (public.finnest_has_admin_role(array['super_admin']));
create policy "Super admins grant roles" on public.finnest_admin_roles for insert to authenticated with check (public.finnest_has_admin_role(array['super_admin']));
create policy "Super admins remove other roles" on public.finnest_admin_roles for delete to authenticated using (public.finnest_has_admin_role(array['super_admin']) and user_id <> auth.uid());