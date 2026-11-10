\set ON_ERROR_STOP on
begin;
insert into auth.users(id) values ('10000000-0000-4000-8000-000000000001'), ('10000000-0000-4000-8000-000000000002');
do $$ begin
  if not exists (select 1 from storage.buckets where id='finnest-private-documents' and public=false and file_size_limit=10485760 and allowed_mime_types = array['application/pdf','image/jpeg','image/png']) then
    raise exception 'private bucket configuration mismatch';
  end if;
end $$;
set local role authenticated;
select set_config('request.jwt.claims', '{"sub":"10000000-0000-4000-8000-000000000001","aal":"aal1"}', true);
insert into storage.objects(bucket_id,name) values ('finnest-private-documents','10000000-0000-4000-8000-000000000001/document.pdf');
do $$ begin
  begin insert into storage.objects(bucket_id,name) values ('finnest-private-documents','10000000-0000-4000-8000-000000000002/document.pdf'); raise exception 'cross-user upload allowed';
  exception when insufficient_privilege then null; end;
  begin insert into storage.objects(bucket_id,name) values ('finnest-private-documents','10000000-0000-4000-8000-000000000001-extra/document.pdf'); raise exception 'similar prefix upload allowed';
  exception when insufficient_privilege then null; end;
  begin insert into storage.objects(bucket_id,name) values ('finnest-private-documents','document.pdf'); raise exception 'root upload allowed';
  exception when insufficient_privilege then null; end;
  begin update storage.objects set name='10000000-0000-4000-8000-000000000002/document.pdf'; raise exception 'cross-owner move allowed';
  exception when insufficient_privilege then null; end;
end $$;
select set_config('request.jwt.claims', '{"sub":"10000000-0000-4000-8000-000000000002","aal":"aal1"}', true);
do $$ declare changed integer; begin
  if (select count(*) from storage.objects) <> 0 then raise exception 'user B read user A objects'; end if;
  update storage.objects set name='10000000-0000-4000-8000-000000000002/stolen.pdf'; get diagnostics changed=row_count;
  if changed <> 0 then raise exception 'user B updated user A objects'; end if;
  delete from storage.objects; get diagnostics changed=row_count;
  if changed <> 0 then raise exception 'user B deleted user A objects'; end if;
end $$;
set local role anon;
do $$ begin if (select count(*) from storage.objects) <> 0 then raise exception 'anon read private object'; end if; end $$;
set local role authenticated;
select set_config('request.jwt.claims', '{"sub":"10000000-0000-4000-8000-000000000001","aal":"aal1"}', true);
delete from storage.objects;
do $$ begin if (select count(*) from storage.objects) <> 0 then raise exception 'owner deletion failed'; end if; end $$;
rollback;
