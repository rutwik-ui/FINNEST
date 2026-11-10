\set ON_ERROR_STOP on
begin;
insert into auth.users(id) values ('10000000-0000-4000-8000-000000000001'), ('10000000-0000-4000-8000-000000000002'), ('10000000-0000-4000-8000-000000000003');
insert into public.finnest_admin_roles(user_id,role) values ('10000000-0000-4000-8000-000000000001','content_manager'), ('10000000-0000-4000-8000-000000000003','support_manager');
insert into public.finnest_guidance(id,title,body,category,status) values
 ('30000000-0000-4000-8000-000000000001','Public item','Public text','general','published'),
 ('30000000-0000-4000-8000-000000000002','Private draft','Draft text','general','draft');
set local role anon;
do $$ begin
  if (select count(*) from public.finnest_guidance) <> 1 then raise exception 'anon published visibility wrong'; end if;
  if exists(select 1 from public.finnest_guidance where status='draft') then raise exception 'anon saw draft'; end if;
  begin insert into public.finnest_guidance(title,body,category) values ('bad','bad','general'); raise exception 'anon could write guidance';
  exception when insufficient_privilege then null; end;
  begin perform * from public.finnest_admin_audit; raise exception 'anon can read audit';
  exception when insufficient_privilege then null; end;
end $$;
set local role authenticated;
select set_config('request.jwt.claims', '{"sub":"10000000-0000-4000-8000-000000000002","aal":"aal2"}', true);
do $$ declare changed integer; begin
  if (select count(*) from public.finnest_guidance) <> 1 then raise exception 'buyer saw nonpublic content'; end if;
  begin insert into public.finnest_admin_roles(user_id,role) values ('10000000-0000-4000-8000-000000000002','super_admin'); raise exception 'user self-granted admin';
  exception when insufficient_privilege then null; end;
  begin insert into public.finnest_guidance(title,body,category) values ('bad','bad','general'); raise exception 'buyer inserted guidance';
  exception when insufficient_privilege then null; end;
  update public.finnest_guidance set title='buyer edit'; get diagnostics changed=row_count;
  if changed <> 0 then raise exception 'buyer updated guidance'; end if;
end $$;
select set_config('request.jwt.claims', '{"sub":"10000000-0000-4000-8000-000000000003","aal":"aal2"}', true);
do $$ begin
  if (select count(*) from public.finnest_guidance) <> 1 then raise exception 'support role read drafts'; end if;
  begin insert into public.finnest_guidance(title,body,category) values ('bad','bad','general'); raise exception 'support role inserted guidance';
  exception when insufficient_privilege then null; end;
end $$;
select set_config('request.jwt.claims', '{"sub":"10000000-0000-4000-8000-000000000001","aal":"aal1"}', true);
do $$ declare changed integer; begin
  if (select count(*) from public.finnest_guidance) <> 1 then raise exception 'aal1 editor read drafts'; end if;
  if (select count(*) from public.finnest_admin_audit) <> 0 then raise exception 'aal1 editor read audit'; end if;
  begin insert into public.finnest_guidance(title,body,category) values ('bad','bad','general'); raise exception 'aal1 editor inserted guidance';
  exception when insufficient_privilege then null; end;
  update public.finnest_guidance set title='bad'; get diagnostics changed=row_count;
  if changed <> 0 then raise exception 'aal1 editor updated guidance'; end if;
end $$;
select set_config('request.jwt.claims', '{"sub":"10000000-0000-4000-8000-000000000001","aal":"aal2"}', true);
do $$ begin
  if (select count(*) from public.finnest_guidance) <> 2 then raise exception 'aal2 editor cannot read draft'; end if;
  begin insert into public.finnest_admin_audit(actor_id,item_id,action) values ('10000000-0000-4000-8000-000000000001','30000000-0000-4000-8000-000000000001','UPDATE'); raise exception 'editor wrote audit directly';
  exception when insufficient_privilege then null; end;
end $$;
do $$ begin
  if (select count(*) from public.finnest_admin_roles) <> 1 then raise exception 'editor cannot see own role or sees another'; end if;
  begin insert into public.finnest_guidance(title,body,category) values ('   ','Body','general'); raise exception 'blank title allowed';
  exception when check_violation then null; end;
  begin insert into public.finnest_guidance(title,body,category) values ('Title','Body','invalid'); raise exception 'invalid category allowed';
  exception when check_violation then null; end;
end $$;
insert into public.finnest_guidance(id,title,body,category,status) values ('30000000-0000-4000-8000-000000000003','Editor draft','Written by editor','new_property','draft');
update public.finnest_guidance set status='published' where id='30000000-0000-4000-8000-000000000003';
do $$ begin
  if not exists(select 1 from public.finnest_admin_audit where actor_id='10000000-0000-4000-8000-000000000001' and item_id='30000000-0000-4000-8000-000000000003' and action='INSERT') then raise exception 'editor insertion not audited'; end if;
  if not exists(select 1 from public.finnest_admin_audit where actor_id='10000000-0000-4000-8000-000000000001' and item_id='30000000-0000-4000-8000-000000000003' and action='UPDATE') then raise exception 'editor update not audited'; end if;
end $$;
set local role anon;
do $$ begin
  if (select count(*) from public.finnest_guidance) <> 2 then raise exception 'published guidance unavailable'; end if;
  if exists(select 1 from public.finnest_guidance where status='draft') then raise exception 'draft leaked after publishing'; end if;
end $$;
set local role authenticated;
select set_config('request.jwt.claims', '{"sub":"10000000-0000-4000-8000-000000000001","aal":"aal2"}', true);
update public.finnest_guidance set status='draft' where id='30000000-0000-4000-8000-000000000003';
set local role anon;
do $$ begin
  if (select count(*) from public.finnest_guidance) <> 1 then raise exception 'unpublished guidance remains visible'; end if;
end $$;
reset role;
delete from public.finnest_admin_roles where user_id='10000000-0000-4000-8000-000000000001';
set local role authenticated;
select set_config('request.jwt.claims', '{"sub":"10000000-0000-4000-8000-000000000001","aal":"aal2"}', true);
do $$ begin
  if (select count(*) from public.finnest_guidance) <> 1 then raise exception 'revoked editor retained draft access'; end if;
  begin insert into public.finnest_guidance(title,body,category) values ('bad','bad','general'); raise exception 'revoked editor retained write access';
  exception when insufficient_privilege then null; end;
end $$;
rollback;
