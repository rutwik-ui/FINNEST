-- Grants the first super administrator role (rutwik@paarami.com).
-- Applied to the connected Supabase project on 2026-11-10; recorded here so the
-- repository's migration history matches the live database.
--
-- This only inserts when that account already exists in auth.users. On a fresh
-- project or the disposable test database the id is unknown, and a bare insert
-- would abort every later migration with a foreign-key violation. Real grants are
-- made by a trusted operator following docs/vercel-deployment.md, never guessed.
insert into public.finnest_admin_roles (user_id, role)
select u.id, 'super_admin' from auth.users u where u.id = 'dffa3478-33fb-4e8f-9604-fa333590d314'
on conflict do nothing;
