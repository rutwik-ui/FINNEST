-- Grants the first super administrator role (rutwik@paarami.com).
-- Applied to the connected Supabase project on 2026-11-10; recorded here so the
-- repository's migration history matches the live database.
insert into public.finnest_admin_roles (user_id, role)
values ('dffa3478-33fb-4e8f-9604-fa333590d314', 'super_admin')
on conflict do nothing;
