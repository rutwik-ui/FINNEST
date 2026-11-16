-- Cross-device draft fields, project catalogue and enquiries.
begin;

insert into auth.users(id, email) values
  ('10000000-0000-4000-8000-000000000001', 'a@example.test'),
  ('10000000-0000-4000-8000-000000000002', 'b@example.test');

-- --------------------------------------------------------------- seeded content
-- Seeded as the owner so the anon role has nothing to write. Anonymous visitors
-- must only ever read the published row.
insert into public.finnest_projects(name, status) values ('Published sample', 'published'), ('Hidden sample', 'draft');

-- ---------------------------------------------------------------- anonymous deny
set local role anon;
do $$ begin
  begin perform * from public.finnest_draft_fields; raise exception 'anon read draft fields';
  exception when insufficient_privilege then null; end;
  begin insert into public.finnest_draft_fields(user_id, journey_type, field_key, value, client_updated_at)
    values ('10000000-0000-4000-8000-000000000001', 'new_property', 'budget', 'x', now());
    raise exception 'anon inserted draft field';
  exception when insufficient_privilege then null; end;
end $$;

-- Anonymous visitors may read published projects only.
do $$ begin
  if (select count(*) from public.finnest_projects) <> 1 then raise exception 'anon sees non-published projects'; end if;
  begin insert into public.finnest_projects(name) values ('anon project'); raise exception 'anon created project';
  exception when insufficient_privilege then null; end;
end $$;

-- --------------------------------------------------------------- buyer isolation
set local role authenticated;
select set_config('request.jwt.claims', '{"sub":"10000000-0000-4000-8000-000000000001","aal":"aal1"}', true);

insert into public.finnest_draft_fields(user_id, journey_type, field_key, value, client_updated_at) values
  ('10000000-0000-4000-8000-000000000001', 'new_property', 'budget', '6000000', now() - interval '2 minutes'),
  ('10000000-0000-4000-8000-000000000001', 'new_property', 'visit_parking_note', 'two level basement', now() - interval '2 minutes'),
  ('10000000-0000-4000-8000-000000000001', 'resale_property', 'resale_seller_name', 'Mr Rao', now() - interval '2 minutes');

do $$ begin
  if (select count(*) from public.finnest_draft_fields) <> 3 then raise exception 'owner cannot read all own fields'; end if;
  begin insert into public.finnest_draft_fields(user_id, journey_type, field_key, value, client_updated_at)
    values ('10000000-0000-4000-8000-000000000002', 'new_property', 'budget', '1', now());
    raise exception 'cross-user insert allowed';
  exception when insufficient_privilege then null; end;
  begin update public.finnest_draft_fields set user_id = '10000000-0000-4000-8000-000000000002';
    raise exception 'ownership transfer allowed';
  exception when insufficient_privilege then null; end;
  begin insert into public.finnest_draft_fields(user_id, journey_type, field_key, value, client_updated_at)
    values ('10000000-0000-4000-8000-000000000001', 'new_property', repeat('k', 121), 'x', now());
    raise exception 'over-long field key allowed';
  exception when check_violation then null; end;
  begin insert into public.finnest_draft_fields(user_id, journey_type, field_key, value, client_updated_at)
    values ('10000000-0000-4000-8000-000000000001', 'unknown_journey', 'k', 'x', now());
    raise exception 'unknown journey allowed';
  exception when check_violation then null; end;
end $$;

-- A normal buyer cannot read another user's fields.
do $$ begin
  if (select count(*) from public.finnest_draft_fields where user_id = '10000000-0000-4000-8000-000000000002') <> 0 then
    raise exception 'buyer can read another user fields';
  end if;
end $$;

-- ------------------------------------------------- per-field merge is monotonic
-- An older write must not overwrite a newer one; a newer write must win.
update public.finnest_draft_fields
   set value = 'newer budget', client_updated_at = now()
 where field_key = 'budget';
update public.finnest_draft_fields
   set value = 'stale budget', client_updated_at = now() - interval '10 minutes'
 where field_key = 'budget';
do $$ begin
  if (select value from public.finnest_draft_fields where field_key = 'budget') <> 'newer budget' then
    raise exception 'stale write overwrote newer value';
  end if;
end $$;
update public.finnest_draft_fields
   set value = 'newest budget', client_updated_at = now() + interval '1 minute'
 where field_key = 'budget';
do $$ begin
  if (select value from public.finnest_draft_fields where field_key = 'budget') <> 'newest budget' then
    raise exception 'newer write did not win';
  end if;
end $$;

-- Editing one field must never touch a sibling field written by another device.
do $$ begin
  if (select value from public.finnest_draft_fields where field_key = 'visit_parking_note') <> 'two level basement' then
    raise exception 'per-field isolation broken';
  end if;
end $$;

-- ------------------------------------------------------------------ user B denies
select set_config('request.jwt.claims', '{"sub":"10000000-0000-4000-8000-000000000002","aal":"aal1"}', true);
do $$ declare changed integer; begin
  if (select count(*) from public.finnest_draft_fields) <> 0 then raise exception 'user B reads user A fields'; end if;
  update public.finnest_draft_fields set value = 'stolen' where user_id = '10000000-0000-4000-8000-000000000001';
  get diagnostics changed = row_count;
  if changed <> 0 then raise exception 'user B updated user A fields'; end if;
  delete from public.finnest_draft_fields where user_id = '10000000-0000-4000-8000-000000000001';
  get diagnostics changed = row_count;
  if changed <> 0 then raise exception 'user B deleted user A fields'; end if;
  if (select count(*) from public.finnest_projects) <> 1 then raise exception 'buyer sees unpublished projects'; end if;
end $$;

-- ---------------------------------------------------------------------- enquiries
select set_config('request.jwt.claims', '{"sub":"10000000-0000-4000-8000-000000000001","aal":"aal1"}', true);
insert into public.finnest_enquiries(user_id, subject, message, consent_contact)
values ('10000000-0000-4000-8000-000000000001', 'Callback about Parkside', 'Please call in the evening.', true);
do $$ begin
  if (select count(*) from public.finnest_enquiries) <> 1 then raise exception 'owner cannot read own enquiry'; end if;
  begin insert into public.finnest_enquiries(user_id, subject, message, status)
    values ('10000000-0000-4000-8000-000000000001', 'x', 'y', 'closed');
    raise exception 'buyer set enquiry status';
  exception when insufficient_privilege then null; end;
  begin insert into public.finnest_enquiries(user_id, subject, message, admin_note)
    values ('10000000-0000-4000-8000-000000000001', 'x', 'y', 'internal');
    raise exception 'buyer wrote admin note';
  exception when insufficient_privilege then null; end;
  begin insert into public.finnest_enquiries(user_id, subject, message)
    values ('10000000-0000-4000-8000-000000000002', 'x', 'y');
    raise exception 'buyer spoofed another user enquiry';
  exception when insufficient_privilege then null; end;
end $$;

select set_config('request.jwt.claims', '{"sub":"10000000-0000-4000-8000-000000000002","aal":"aal1"}', true);
do $$ begin
  if (select count(*) from public.finnest_enquiries) <> 0 then raise exception 'buyer reads another enquiry'; end if;
end $$;

-- ---------------------------------------------------------------- admin surfaces
-- AAL1 admins are refused; AAL2 admins are allowed. Same guarantee as content.
-- Roles are granted by a trusted operator (never by the browser), so this insert
-- runs as the owning superuser rather than as the signed-in buyer.
reset role;
insert into public.finnest_admin_roles(user_id, role) values ('10000000-0000-4000-8000-000000000002', 'support_manager');
set local role authenticated;
select set_config('request.jwt.claims', '{"sub":"10000000-0000-4000-8000-000000000002","aal":"aal1"}', true);
do $$ begin
  if (select count(*) from public.finnest_enquiries) <> 0 then raise exception 'aal1 support manager read enquiries'; end if;
end $$;
select set_config('request.jwt.claims', '{"sub":"10000000-0000-4000-8000-000000000002","aal":"aal2"}', true);
do $$ begin
  if (select count(*) from public.finnest_enquiries) <> 1 then raise exception 'aal2 support manager cannot read enquiries'; end if;
end $$;
update public.finnest_enquiries set status = 'contacted', admin_note = 'Called back.';

-- A support manager must not be able to publish projects (content_manager only).
do $$ begin
  begin insert into public.finnest_projects(name, status) values ('support project', 'published');
    raise exception 'support manager created project';
  exception when insufficient_privilege then null; end;
end $$;

-- A super admin may read buyer draft fields for support, but only at AAL2.
reset role;
update public.finnest_admin_roles set role = 'super_admin';
set local role authenticated;
do $$ begin
  if (select count(*) from public.finnest_draft_fields) < 1 then raise exception 'super admin cannot read draft fields'; end if;
end $$;
select set_config('request.jwt.claims', '{"sub":"10000000-0000-4000-8000-000000000002","aal":"aal1"}', true);
do $$ begin
  if (select count(*) from public.finnest_draft_fields) <> 0 then raise exception 'aal1 super admin read draft fields'; end if;
end $$;

-- ------------------------------------------------------------------- project admin
select set_config('request.jwt.claims', '{"sub":"10000000-0000-4000-8000-000000000002","aal":"aal2"}', true);
reset role;
update public.finnest_admin_roles set role = 'content_manager';
set local role authenticated;
update public.finnest_projects set status = 'published', summary = 'Now visible.' where name = 'Hidden sample';
delete from public.finnest_projects where name = 'Hidden sample';
do $$ begin
  if (select count(*) from public.finnest_projects) <> 1 then raise exception 'content manager project lifecycle failed'; end if;
end $$;

rollback;
