-- Private, per-account and per-device draft storage. Clients do not write here yet.
-- A future migration/sync flow must preserve guest data and resolve conflicts explicitly.
create table public.finnest_user_drafts (
  user_id uuid not null references auth.users(id) on delete cascade,
  device_id uuid not null,
  journey_type text not null check (journey_type in ('new_property', 'resale_property')),
  payload jsonb not null default '{}'::jsonb check (jsonb_typeof(payload) = 'object' and pg_column_size(payload) <= 1048576),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  primary key (user_id, device_id, journey_type)
);

alter table public.finnest_user_drafts enable row level security;
revoke all on public.finnest_user_drafts from anon, authenticated;
grant select, insert, update, delete on public.finnest_user_drafts to authenticated;

create policy "Users read their own drafts" on public.finnest_user_drafts for select to authenticated using ((select auth.uid()) = user_id);
create policy "Users create their own drafts" on public.finnest_user_drafts for insert to authenticated with check ((select auth.uid()) = user_id);
create policy "Users update their own drafts" on public.finnest_user_drafts for update to authenticated using ((select auth.uid()) = user_id) with check ((select auth.uid()) = user_id);
create policy "Users delete their own drafts" on public.finnest_user_drafts for delete to authenticated using ((select auth.uid()) = user_id);

create function public.finnest_touch_updated_at() returns trigger language plpgsql set search_path = '' as $$
begin
  new.updated_at = now();
  return new;
end;
$$;
create trigger finnest_drafts_touch_updated_at before update on public.finnest_user_drafts for each row execute function public.finnest_touch_updated_at();

-- Private bucket: future uploads must be placed under the authenticated user's UUID folder.
insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('finnest-private-documents', 'finnest-private-documents', false, 10485760, array['application/pdf','image/jpeg','image/png']);
create policy "Users read their own FINNEST documents" on storage.objects for select to authenticated using (bucket_id = 'finnest-private-documents' and (storage.foldername(name))[1] = (select auth.uid())::text);
create policy "Users upload their own FINNEST documents" on storage.objects for insert to authenticated with check (bucket_id = 'finnest-private-documents' and (storage.foldername(name))[1] = (select auth.uid())::text);
create policy "Users update their own FINNEST documents" on storage.objects for update to authenticated using (bucket_id = 'finnest-private-documents' and (storage.foldername(name))[1] = (select auth.uid())::text) with check (bucket_id = 'finnest-private-documents' and (storage.foldername(name))[1] = (select auth.uid())::text);
create policy "Users delete their own FINNEST documents" on storage.objects for delete to authenticated using (bucket_id = 'finnest-private-documents' and (storage.foldername(name))[1] = (select auth.uid())::text);
