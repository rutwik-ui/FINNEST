-- Administrative roles can only be provisioned by a trusted server/database administrator.
create table public.finnest_admin_roles (
  user_id uuid primary key references auth.users(id) on delete cascade,
  role text not null check (role in ('super_admin','content_manager','support_manager','expert_manager')),
  created_at timestamptz not null default now()
);
alter table public.finnest_admin_roles enable row level security;
revoke all on public.finnest_admin_roles from public, anon, authenticated;
grant select on public.finnest_admin_roles to authenticated;
create policy "Users can read own admin role" on public.finnest_admin_roles for select to authenticated using (user_id = (select auth.uid()));

-- This is guidance content, never private buyer data. Published rows are public.
create table public.finnest_guidance (
  id uuid primary key default gen_random_uuid(),
  title text not null check (length(trim(title)) between 1 and 150),
  body text not null check (length(trim(body)) between 1 and 10000),
  category text not null check (category in ('new_property','resale_property','general')),
  status text not null default 'draft' check (status in ('draft','published')),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
alter table public.finnest_guidance enable row level security;
revoke all on public.finnest_guidance from public, anon, authenticated;
grant select on public.finnest_guidance to anon, authenticated;
grant insert, update on public.finnest_guidance to authenticated;
create policy "Anyone reads published guidance" on public.finnest_guidance for select to anon, authenticated using (status = 'published');
create policy "Content editors read all guidance" on public.finnest_guidance for select to authenticated using (exists (select 1 from public.finnest_admin_roles r where r.user_id = (select auth.uid()) and r.role in ('super_admin','content_manager')));
create policy "Content editors create guidance" on public.finnest_guidance for insert to authenticated with check (exists (select 1 from public.finnest_admin_roles r where r.user_id = (select auth.uid()) and r.role in ('super_admin','content_manager')));
create policy "Content editors update guidance" on public.finnest_guidance for update to authenticated using (exists (select 1 from public.finnest_admin_roles r where r.user_id = (select auth.uid()) and r.role in ('super_admin','content_manager'))) with check (exists (select 1 from public.finnest_admin_roles r where r.user_id = (select auth.uid()) and r.role in ('super_admin','content_manager')));
create trigger finnest_guidance_touch_updated_at before update on public.finnest_guidance for each row execute function public.finnest_touch_updated_at();

-- Append-only to clients; stores metadata rather than content or buyer data.
create table public.finnest_admin_audit (
  id bigint generated always as identity primary key,
  actor_id uuid references auth.users(id) on delete set null,
  item_id uuid not null,
  action text not null check (action in ('INSERT','UPDATE')),
  occurred_at timestamptz not null default now()
);
alter table public.finnest_admin_audit enable row level security;
revoke all on public.finnest_admin_audit from public, anon, authenticated;
grant select on public.finnest_admin_audit to authenticated;
create policy "Content editors read audit" on public.finnest_admin_audit for select to authenticated using (exists (select 1 from public.finnest_admin_roles r where r.user_id = (select auth.uid()) and r.role in ('super_admin','content_manager')));
create function public.finnest_audit_guidance() returns trigger language plpgsql security definer set search_path = '' as $$
begin
  insert into public.finnest_admin_audit(actor_id,item_id,action) values ((select auth.uid()), new.id, tg_op);
  return new;
end;
$$;
revoke all on function public.finnest_audit_guidance() from public, anon, authenticated;
create trigger finnest_guidance_audit after insert or update on public.finnest_guidance for each row execute function public.finnest_audit_guidance();
