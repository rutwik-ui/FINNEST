CREATE OR REPLACE FUNCTION public.finnest_has_admin_role(_roles text[])
RETURNS boolean
LANGUAGE sql STABLE SECURITY DEFINER
SET search_path = ''
AS $$
  SELECT (SELECT auth.uid()) IS NOT NULL AND EXISTS (
    SELECT 1 FROM public.finnest_admin_roles r
    WHERE r.user_id = (SELECT auth.uid()) AND r.role = ANY(_roles)
  );
$$;
REVOKE ALL ON FUNCTION public.finnest_has_admin_role(text[]) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.finnest_has_admin_role(text[]) TO authenticated;

ALTER POLICY "Content editors read all guidance" ON public.finnest_guidance
USING (public.finnest_has_admin_role(ARRAY['super_admin','content_manager']));
ALTER POLICY "Content editors create guidance" ON public.finnest_guidance
WITH CHECK (public.finnest_has_admin_role(ARRAY['super_admin','content_manager']));
ALTER POLICY "Content editors update guidance" ON public.finnest_guidance
USING (public.finnest_has_admin_role(ARRAY['super_admin','content_manager']))
WITH CHECK (public.finnest_has_admin_role(ARRAY['super_admin','content_manager']));
ALTER POLICY "Content editors read audit" ON public.finnest_admin_audit
USING (public.finnest_has_admin_role(ARRAY['super_admin','content_manager']));

DO $$
DECLARE admin_id uuid; saved_claims text; outsider uuid := gen_random_uuid();
BEGIN
  saved_claims := current_setting('request.jwt.claims', true);
  SELECT user_id INTO admin_id FROM public.finnest_admin_roles WHERE role = 'super_admin' LIMIT 1;
  IF admin_id IS NULL THEN RAISE EXCEPTION 'Cannot verify password-only administrator access without an assigned super admin'; END IF;
  PERFORM set_config('request.jwt.claims', json_build_object('sub',admin_id,'role','authenticated','aal','aal1')::text,true);
  IF NOT public.finnest_has_admin_role(ARRAY['super_admin']) THEN RAISE EXCEPTION 'Password-only super admin must retain access'; END IF;
  IF public.finnest_has_admin_role(ARRAY['not_a_role']) THEN RAISE EXCEPTION 'Unassigned roles must be denied'; END IF;
  PERFORM set_config('request.jwt.claims', json_build_object('sub',outsider,'role','authenticated','aal','aal1')::text,true);
  IF public.finnest_has_admin_role(ARRAY['super_admin','content_manager','support_manager','expert_manager']) THEN RAISE EXCEPTION 'Non-admin must not receive administrator access'; END IF;
  PERFORM set_config('request.jwt.claims','{}',true);
  IF public.finnest_has_admin_role(ARRAY['super_admin']) THEN RAISE EXCEPTION 'Signed-out callers must be denied'; END IF;
  PERFORM set_config('request.jwt.claims',coalesce(saved_claims,''),true);
END;
$$;