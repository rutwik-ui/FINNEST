\set ON_ERROR_STOP on
begin;
insert into auth.users(id) values ('10000000-0000-4000-8000-000000000001'), ('10000000-0000-4000-8000-000000000002');
set local role anon;
do $$ begin
  begin perform * from public.finnest_user_drafts; raise exception 'anon could read drafts';
  exception when insufficient_privilege then null; end;
  begin insert into public.finnest_user_drafts(user_id, device_id, journey_type) values ('10000000-0000-4000-8000-000000000001', '20000000-0000-4000-8000-000000000001', 'new_property'); raise exception 'anon could insert drafts';
  exception when insufficient_privilege then null; end;
end $$;
set local role authenticated;
select set_config('request.jwt.claims', '{}', true);
do $$ begin
  if (select count(*) from public.finnest_user_drafts) <> 0 then raise exception 'missing user identity read drafts'; end if;
  begin insert into public.finnest_user_drafts(user_id,device_id,journey_type) values ('10000000-0000-4000-8000-000000000001','20000000-0000-4000-8000-000000000001','new_property'); raise exception 'missing identity inserted draft';
  exception when insufficient_privilege then null; end;
end $$;
select set_config('request.jwt.claims', '{"sub":"10000000-0000-4000-8000-000000000001","aal":"aal1"}', true);
insert into public.finnest_user_drafts(user_id,device_id,journey_type,payload) values
 ('10000000-0000-4000-8000-000000000001','20000000-0000-4000-8000-000000000001','new_property','{"budget":"100"}'),
 ('10000000-0000-4000-8000-000000000001','20000000-0000-4000-8000-000000000001','resale_property','{"budget":"200"}');
do $$ begin
  if (select count(*) from public.finnest_user_drafts) <> 2 then raise exception 'owner cannot read both own journeys'; end if;
  begin insert into public.finnest_user_drafts(user_id,device_id,journey_type) values ('10000000-0000-4000-8000-000000000002','20000000-0000-4000-8000-000000000001','new_property'); raise exception 'cross-user insert allowed';
  exception when insufficient_privilege then null; end;
  begin update public.finnest_user_drafts set user_id='10000000-0000-4000-8000-000000000002' where journey_type='new_property'; raise exception 'draft ownership transfer allowed';
  exception when insufficient_privilege then null; end;
  begin insert into public.finnest_user_drafts(user_id,device_id,journey_type,payload) values ('10000000-0000-4000-8000-000000000001','20000000-0000-4000-8000-000000000002','new_property','[]'); raise exception 'non-object payload allowed';
  exception when check_violation then null; end;
end $$;
select set_config('request.jwt.claims', '{"sub":"10000000-0000-4000-8000-000000000002","aal":"aal1"}', true);
do $$ declare changed integer; begin
  if (select count(*) from public.finnest_user_drafts) <> 0 then raise exception 'user B can read user A drafts'; end if;
  update public.finnest_user_drafts set payload='{"stolen":true}' where user_id='10000000-0000-4000-8000-000000000001'; get diagnostics changed = row_count;
  if changed <> 0 then raise exception 'user B updated user A drafts'; end if;
  delete from public.finnest_user_drafts where user_id='10000000-0000-4000-8000-000000000001'; get diagnostics changed = row_count;
  if changed <> 0 then raise exception 'user B deleted user A drafts'; end if;
end $$;
insert into public.finnest_user_drafts(user_id,device_id,journey_type,payload) values ('10000000-0000-4000-8000-000000000002','20000000-0000-4000-8000-000000000001','new_property','{"own":true}');
do $$ begin if (select count(*) from public.finnest_user_drafts) <> 1 then raise exception 'user B own draft isolation failed'; end if; end $$;
select set_config('request.jwt.claims', '{"sub":"10000000-0000-4000-8000-000000000001","aal":"aal1"}', true);
update public.finnest_user_drafts set payload='{"budget":"150"}' where journey_type='new_property';
do $$ begin if (select payload->>'budget' from public.finnest_user_drafts where journey_type='new_property') <> '150' then raise exception 'owner update failed'; end if; end $$;
delete from public.finnest_user_drafts where journey_type='resale_property';
do $$ begin if (select count(*) from public.finnest_user_drafts) <> 1 then raise exception 'owner deletion failed'; end if; end $$;
rollback;
