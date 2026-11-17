CREATE TABLE public.finnest_app_settings (
  state text PRIMARY KEY CHECK (state IN ('draft','published')),
  payload jsonb NOT NULL,
  revision integer NOT NULL DEFAULT 1,
  updated_at timestamptz NOT NULL DEFAULT now(),
  updated_by uuid
);
REVOKE ALL ON public.finnest_app_settings FROM anon, authenticated;
GRANT SELECT ON public.finnest_app_settings TO anon;
GRANT SELECT, UPDATE ON public.finnest_app_settings TO authenticated;
GRANT ALL ON public.finnest_app_settings TO service_role;
ALTER TABLE public.finnest_app_settings ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Public reads published app settings" ON public.finnest_app_settings FOR SELECT TO anon, authenticated USING (state = 'published');
CREATE POLICY "Super admins read draft settings" ON public.finnest_app_settings FOR SELECT TO authenticated USING (public.finnest_has_admin_role(ARRAY['super_admin']));
CREATE POLICY "Super admins update app settings" ON public.finnest_app_settings FOR UPDATE TO authenticated USING (public.finnest_has_admin_role(ARRAY['super_admin'])) WITH CHECK (public.finnest_has_admin_role(ARRAY['super_admin']));
CREATE TABLE public.finnest_settings_history (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  state text NOT NULL,
  payload jsonb NOT NULL,
  revision integer NOT NULL,
  actor_id uuid,
  occurred_at timestamptz NOT NULL DEFAULT now()
);
REVOKE ALL ON public.finnest_settings_history FROM anon, authenticated;
GRANT SELECT ON public.finnest_settings_history TO authenticated;
GRANT ALL ON public.finnest_settings_history TO service_role;
GRANT USAGE, SELECT ON SEQUENCE public.finnest_settings_history_id_seq TO service_role;
ALTER TABLE public.finnest_settings_history ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Super admins read settings history" ON public.finnest_settings_history FOR SELECT TO authenticated USING (public.finnest_has_admin_role(ARRAY['super_admin']));
INSERT INTO public.finnest_app_settings(state,payload) VALUES
('draft','{"homeTitle":"Your home-buying journey","homeSubtitle":"A clearer path to a place of your own.","guestEnabled":true,"maintenanceEnabled":false,"maintenanceMessage":"FINNEST is temporarily unavailable. Please try again shortly.","sections":["journey","nextAction","progress","guidance"]}'),
('published','{"homeTitle":"Your home-buying journey","homeSubtitle":"A clearer path to a place of your own.","guestEnabled":true,"maintenanceEnabled":false,"maintenanceMessage":"FINNEST is temporarily unavailable. Please try again shortly.","sections":["journey","nextAction","progress","guidance"]}');
CREATE FUNCTION public.finnest_validate_settings() RETURNS trigger LANGUAGE plpgsql SET search_path = public AS $$
BEGIN
 IF jsonb_typeof(NEW.payload) <> 'object' OR jsonb_typeof(NEW.payload->'homeTitle') <> 'string' OR length(NEW.payload->>'homeTitle') NOT BETWEEN 1 AND 120 OR jsonb_typeof(NEW.payload->'homeSubtitle') <> 'string' OR length(NEW.payload->>'homeSubtitle') > 300 OR jsonb_typeof(NEW.payload->'guestEnabled') <> 'boolean' OR jsonb_typeof(NEW.payload->'maintenanceEnabled') <> 'boolean' OR jsonb_typeof(NEW.payload->'maintenanceMessage') <> 'string' OR length(NEW.payload->>'maintenanceMessage') NOT BETWEEN 1 AND 500 OR jsonb_typeof(NEW.payload->'sections') <> 'array' THEN RAISE EXCEPTION 'Invalid app settings'; END IF;
 IF jsonb_array_length(NEW.payload->'sections') <> 4 OR NOT (NEW.payload->'sections' @> '["journey","nextAction","progress","guidance"]'::jsonb) THEN RAISE EXCEPTION 'Preserve all home sections'; END IF;
 NEW.revision := OLD.revision + 1; NEW.updated_at := now(); NEW.updated_by := auth.uid(); RETURN NEW;
END $$;
CREATE TRIGGER finnest_validate_settings BEFORE UPDATE ON public.finnest_app_settings FOR EACH ROW EXECUTE FUNCTION public.finnest_validate_settings();
CREATE FUNCTION public.finnest_audit_settings() RETURNS trigger LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
BEGIN
 INSERT INTO public.finnest_settings_history(state,payload,revision,actor_id) VALUES (NEW.state,NEW.payload,NEW.revision,auth.uid()); RETURN NEW;
END $$;
REVOKE ALL ON FUNCTION public.finnest_validate_settings() FROM PUBLIC, anon, authenticated;
REVOKE ALL ON FUNCTION public.finnest_audit_settings() FROM PUBLIC, anon, authenticated;
CREATE TRIGGER finnest_audit_settings AFTER UPDATE ON public.finnest_app_settings FOR EACH ROW EXECUTE FUNCTION public.finnest_audit_settings();
ALTER PUBLICATION supabase_realtime ADD TABLE public.finnest_app_settings;
DO $$ BEGIN
 IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE tablename='finnest_app_settings' AND policyname='Public reads published app settings' AND qual = '(state = ''published''::text)') THEN RAISE EXCEPTION 'Missing public publish boundary'; END IF;
 IF has_table_privilege('anon','public.finnest_app_settings','UPDATE') THEN RAISE EXCEPTION 'Anonymous settings edits forbidden'; END IF;
 IF has_table_privilege('authenticated','public.finnest_settings_history','INSERT') THEN RAISE EXCEPTION 'Audit writes must be trigger-only'; END IF;
END $$;