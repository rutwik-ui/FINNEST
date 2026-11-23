create policy "Users upload own documents" on storage.objects for insert to authenticated
  with check (bucket_id = 'finnest-private-documents' and (storage.foldername(name))[1] = (select auth.uid())::text);
create policy "Users read own documents" on storage.objects for select to authenticated
  using (bucket_id = 'finnest-private-documents' and (storage.foldername(name))[1] = (select auth.uid())::text);
create policy "Support admins read documents" on storage.objects for select to authenticated
  using (bucket_id = 'finnest-private-documents' and public.finnest_has_admin_role(array['super_admin','support_manager']));