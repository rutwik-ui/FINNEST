-- Grants the second super administrator role (digitalrazorpay@gmail.com).
-- Applied to the connected Supabase project on 2026-11-10; recorded here so the
-- repository's migration history matches the live database.
insert into public.finnest_admin_roles (user_id, role)
values ('acb89a0c-e3ca-4664-9c30-e4710ad424b8', 'super_admin')
on conflict do nothing;
