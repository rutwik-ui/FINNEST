\set ON_ERROR_STOP on
-- Disposable shim only: claims are fabricated, not real Supabase sessions.
begin;
insert into auth.users(id) values ('10000000-0000-4000-8000-000000000001'), ('10000000-0000-4000-8000-000000000002');
insert into public.finnest_admin_roles(user_id,role) values ('10000000-0000-4000-8000-000000000001','super_admin');
insert into public.finnest_guidance(id,title,body,category,status) values ('30000000-0000-4000-8000-000000000001','Draft','Text','general','draft');
set local role authenticated;
select set_config('request.jwt.claims', '{"sub":"10000000-0000-4000-8000-000000000001","aal":"aal2"}', true);
do $$ begin
  begin update public.finnest_admin_roles set role='content_manager'; raise exception 'client changed role'; exception when insufficient_privilege then null; end;
  begin delete from public.finnest_admin_roles; raise exception 'client deleted role'; exception when insufficient_privilege then null; end;
  begin delete from public.finnest_guidance; raise exception 'client deleted guidance'; exception when insufficient_privilege then null; end;
  begin update public.finnest_admin_audit set action='UPDATE'; raise exception 'client modified audit'; exception when insufficient_privilege then null; end;
  begin delete from public.finnest_admin_audit; raise exception 'client deleted audit'; exception when insufficient_privilege then null; end;
  begin perform public.finnest_audit_guidance(); raise exception 'client called audit trigger'; exception when insufficient_privilege then null; end;
  begin insert into public.finnest_user_drafts(user_id,device_id,journey_type) values ('10000000-0000-4000-8000-000000000001','20000000-0000-4000-8000-000000000001','invalid'); raise exception 'invalid journey allowed'; exception when check_violation then null; end;
  begin insert into public.finnest_user_drafts(user_id,device_id,journey_type,payload)
    select '10000000-0000-4000-8000-000000000001','20000000-0000-4000-8000-000000000001','new_property',jsonb_build_object('large',string_agg(md5(i::text),'')) from generate_series(1,50000) i;
    raise exception 'oversized payload allowed'; exception when check_violation then null; end;
end $$;
insert into public.finnest_user_drafts(user_id,device_id,journey_type) values ('10000000-0000-4000-8000-000000000001','20000000-0000-4000-8000-000000000001','new_property');
do $$ begin
 begin insert into public.finnest_user_drafts(user_id,device_id,journey_type) values ('10000000-0000-4000-8000-000000000001','20000000-0000-4000-8000-000000000001','new_property'); raise exception 'duplicate draft allowed'; exception when unique_violation then null; end;
end $$;
-- A role by itself, or an AAL2 claim without a subject, grants no private access.
select set_config('request.jwt.claims', '{"aal":"aal2"}', true);
do $$ begin
 if exists(select 1 from public.finnest_guidance) or exists(select 1 from public.finnest_admin_roles) or exists(select 1 from public.finnest_admin_audit) or exists(select 1 from public.finnest_user_drafts) then raise exception 'missing subject saw private data'; end if;
 begin insert into storage.objects(bucket_id,name) values ('finnest-private-documents','10000000-0000-4000-8000-000000000001/file.pdf'); raise exception 'missing subject uploaded object'; exception when insufficient_privilege then null; end;
end $$;
rollback;
