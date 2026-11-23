-- finnest_journeys / finnest_documents / finnest_activity.
--
-- These three tables were created directly in production by Lovable's editor and
-- only later committed to the repository (migration 20261112132646). Until now no
-- test ever touched them, so their policies were never exercised. This suite pins
-- the behaviour the rest of the app relies on:
--   * a buyer can only see and change their own rows,
--   * the version counter is bumped by the database, not the client,
--   * staff can read journeys and documents but nobody can rewrite activity,
--   * anonymous visitors get nothing at all.
begin;

insert into auth.users(id, email) values
  ('40000000-0000-4000-8000-000000000001', 'j1@example.test'),
  ('40000000-0000-4000-8000-000000000002', 'j2@example.test');

insert into public.finnest_admin_roles(user_id, role) values
  ('40000000-0000-4000-8000-000000000002', 'support_manager');

-- ------------------------------------------------------------ least privilege
-- Behaviour alone cannot pin this: with DELETE granted and RLS on, an anonymous
-- delete affects zero rows without raising, so a row-count check passes either way.
-- The grant itself is the regression to catch, so assert on the catalog.
do $$
declare
  excess text;
begin
  select string_agg(table_name || ':' || privilege_type, ', ' order by table_name, privilege_type)
    into excess
    from information_schema.role_table_grants
   where table_schema = 'public'
     and table_name in ('finnest_journeys', 'finnest_documents', 'finnest_activity')
     and grantee = 'anon';
  if excess is not null then
    raise exception 'anon holds privileges it should not: %', excess;
  end if;

  -- authenticated needs DML, but never TRUNCATE: that bypasses RLS entirely.
  select string_agg(table_name || ':' || privilege_type, ', ' order by table_name)
    into excess
    from information_schema.role_table_grants
   where table_schema = 'public'
     and table_name in ('finnest_journeys', 'finnest_documents', 'finnest_activity')
     and grantee = 'authenticated'
     and privilege_type in ('TRUNCATE', 'REFERENCES', 'TRIGGER');
  if excess is not null then
    raise exception 'authenticated holds dangerous privileges: %', excess;
  end if;
end $$;

-- ------------------------------------------------------------ owner seeds rows
-- Seed first so the anonymous check below has rows it could destroy. Deleting from
-- an empty table would prove nothing.
set local role authenticated;
select set_config('request.jwt.claims', '{"sub":"40000000-0000-4000-8000-000000000001","aal":"aal1"}', true);

insert into public.finnest_journeys(user_id, journey_type, payload, current_step, completed_steps)
values ('40000000-0000-4000-8000-000000000001', 'new', '{"budget":"6cr"}', 'visit', array['basics']);
insert into public.finnest_documents(user_id, journey_type, path, name)
values ('40000000-0000-4000-8000-000000000001', 'new', 'docs/aadhaar.pdf', 'Aadhaar');
insert into public.finnest_activity(user_id, event, platform)
values ('40000000-0000-4000-8000-000000000001', 'journey_saved', 'android');

-- -------------------------------------------------------------- anonymous deny
-- Every check here asserts *both* that nothing is visible and that nothing changes.
-- An RLS-filtered write affects zero rows without raising, so asserting only on an
-- exception would pass even if a stray grant let the statement through.
set local role anon;
do $$
declare
  touched int;
begin
  begin perform * from public.finnest_journeys; raise exception 'anon read journeys';
  exception when insufficient_privilege then null; end;
  begin perform * from public.finnest_documents; raise exception 'anon read documents';
  exception when insufficient_privilege then null; end;
  begin perform * from public.finnest_activity; raise exception 'anon read activity';
  exception when insufficient_privilege then null; end;

  begin
    insert into public.finnest_activity(user_id, event) values ('40000000-0000-4000-8000-000000000001', 'anon');
  exception when insufficient_privilege then null;
  end;

  begin delete from public.finnest_journeys; exception when insufficient_privilege then null; end;
  get diagnostics touched = row_count;
  if touched <> 0 then raise exception 'anon deleted % journey row(s)', touched; end if;

  begin delete from public.finnest_documents; exception when insufficient_privilege then null; end;
  get diagnostics touched = row_count;
  if touched <> 0 then raise exception 'anon deleted % document row(s)', touched; end if;

  begin delete from public.finnest_activity; exception when insufficient_privilege then null; end;
  get diagnostics touched = row_count;
  if touched <> 0 then raise exception 'anon deleted % activity row(s)', touched; end if;

end $$;

-- Counts can only be read as the owner, so verify from that side afterwards. The
-- fixture above seeds exactly one journey, one document and one activity row.
set local role authenticated;
select set_config('request.jwt.claims', '{"sub":"40000000-0000-4000-8000-000000000001","aal":"aal1"}', true);
do $$ begin
  if (select count(*) from public.finnest_journeys) <> 1 then raise exception 'anonymous delete changed journeys'; end if;
  if (select count(*) from public.finnest_documents) <> 1 then raise exception 'anonymous delete changed documents'; end if;
  if (select count(*) from public.finnest_activity) <> 1 then raise exception 'anonymous delete changed activity'; end if;
end $$;

-- ------------------------------------------------------------- buyer isolation
set local role authenticated;
select set_config('request.jwt.claims', '{"sub":"40000000-0000-4000-8000-000000000001","aal":"aal1"}', true);

do $$ begin
  if (select count(*) from public.finnest_journeys) <> 1 then raise exception 'owner cannot read own journey'; end if;
  if (select count(*) from public.finnest_documents) <> 1 then raise exception 'owner cannot read own document'; end if;

  -- Defaults must come from the database, not from whatever the client sends.
  if (select version from public.finnest_journeys) <> 1 then raise exception 'journey version did not start at 1'; end if;

  begin insert into public.finnest_journeys(user_id, journey_type)
    values ('40000000-0000-4000-8000-000000000002', 'new');
    raise exception 'cross-user journey insert allowed';
  exception when insufficient_privilege then null; end;

  begin insert into public.finnest_documents(user_id, path, name)
    values ('40000000-0000-4000-8000-000000000002', 'docs/x.pdf', 'x');
    raise exception 'cross-user document insert allowed';
  exception when insufficient_privilege then null; end;

  begin insert into public.finnest_activity(user_id, event)
    values ('40000000-0000-4000-8000-000000000002', 'spoof');
    raise exception 'cross-user activity insert allowed';
  exception when insufficient_privilege then null; end;

  begin insert into public.finnest_journeys(user_id, journey_type)
    values ('40000000-0000-4000-8000-000000000001', 'unknown');
    raise exception 'unknown journey type allowed';
  exception when check_violation then null; end;
end $$;

-- The version counter is the database's job: a client that sends its own value
-- must not be able to skip or repeat a revision.
update public.finnest_journeys
   set payload = '{"budget":"7cr"}', version = 99
 where user_id = '40000000-0000-4000-8000-000000000001' and journey_type = 'new';

do $$ begin
  if (select version from public.finnest_journeys) <> 2 then
    raise exception 'client overrode version (expected 2, got %)',
      (select version from public.finnest_journeys);
  end if;
end $$;

-- Activity is append-only: a buyer may log an event but never edit history.
insert into public.finnest_activity(user_id, event, platform)
values ('40000000-0000-4000-8000-000000000001', 'step_completed', 'web');
do $$ begin
  if (select count(*) from public.finnest_activity) <> 2 then raise exception 'own activity not visible'; end if;
  begin update public.finnest_activity set event = 'rewritten';
    raise exception 'activity was editable';
  exception when insufficient_privilege then null; end;
  begin delete from public.finnest_activity;
    raise exception 'activity was deletable';
  exception when insufficient_privilege then null; end;
  begin insert into public.finnest_activity(user_id, event, platform)
    values ('40000000-0000-4000-8000-000000000001', 'bad_platform', 'ios');
    raise exception 'unknown platform allowed';
  exception when check_violation then null; end;
end $$;

-- ------------------------------------------------------------ second buyer
select set_config('request.jwt.claims', '{"sub":"40000000-0000-4000-8000-000000000002","aal":"aal2"}', true);

do $$ begin
  -- AAL2 + support_manager: may read the first buyer's journey and documents.
  if (select count(*) from public.finnest_journeys) <> 1 then
    raise exception 'staff cannot read buyer journeys';
  end if;
  if (select count(*) from public.finnest_documents) <> 1 then
    raise exception 'staff cannot read buyer documents';
  end if;
  if (select count(*) from public.finnest_activity) <> 2 then
    raise exception 'staff cannot read activity';
  end if;
  -- ...but must not be able to write to any of them.
  begin insert into public.finnest_journeys(user_id, journey_type)
    values ('40000000-0000-4000-8000-000000000001', 'resale');
    raise exception 'staff wrote a buyer journey';
  exception when insufficient_privilege then null; end;
  begin update public.finnest_activity set event = 'by staff';
    raise exception 'staff rewrote activity';
  exception when insufficient_privilege then null; end;
end $$;

-- Staff without AAL2 lose the cross-user read. This is the MFA gate that the rest
-- of the admin surface already relies on.
select set_config('request.jwt.claims', '{"sub":"40000000-0000-4000-8000-000000000002","aal":"aal1"}', true);
do $$ begin
  if (select count(*) from public.finnest_journeys) <> 0 then
    raise exception 'aal1 staff read buyer journeys';
  end if;
  if (select count(*) from public.finnest_documents) <> 0 then
    raise exception 'aal1 staff read buyer documents';
  end if;
  if (select count(*) from public.finnest_activity) <> 0 then
    raise exception 'aal1 staff read buyer activity';
  end if;
end $$;

reset role;
rollback;
