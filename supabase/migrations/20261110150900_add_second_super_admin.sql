-- Grants the second super administrator role (digitalrazorpay@gmail.com).
-- Applied to the connected Supabase project on 2026-11-10; recorded here so the
-- repository's migration history matches the live database.
--
-- Guarded the same way as the first grant: it is a no-op unless the account
-- already exists, so a fresh database can be built from these migrations alone.
insert into public.finnest_admin_roles (user_id, role)
select u.id, 'super_admin' from auth.users u where u.id = 'acb89a0c-e3ca-4664-9c30-e4710ad424b8'
on conflict do nothing;
