-- TEST ONLY: minimal auth/storage interfaces for running FINNEST migrations against
-- an isolated PostgreSQL service. This is not the Supabase Auth or Storage implementation.
-- Never execute this file against a real Supabase project.
create role anon nologin;
create role authenticated nologin;
create schema auth;
create table auth.users (id uuid primary key);
create function auth.jwt() returns jsonb language sql stable as $$
  select coalesce(nullif(current_setting('request.jwt.claims', true), '')::jsonb, '{}'::jsonb)
$$;
create function auth.uid() returns uuid language sql stable as $$
  select nullif(auth.jwt() ->> 'sub', '')::uuid
$$;
grant usage on schema auth to anon, authenticated;
grant execute on function auth.jwt(), auth.uid() to anon, authenticated;

create schema storage;
create table storage.buckets (id text primary key, name text not null, public boolean, file_size_limit bigint, allowed_mime_types text[]);
create table storage.objects (id uuid primary key default gen_random_uuid(), bucket_id text references storage.buckets(id), name text not null);
create function storage.foldername(name text) returns text[] language sql immutable as $$
  select (string_to_array(name, '/'))[1:array_length(string_to_array(name, '/'), 1)-1]
$$;
alter table storage.objects enable row level security;
grant usage on schema storage to anon, authenticated;
grant select, insert, update, delete on storage.objects to authenticated;
grant select on storage.objects to anon;
grant execute on function storage.foldername(text) to anon, authenticated;
