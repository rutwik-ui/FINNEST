CREATE OR REPLACE FUNCTION public.finnest_validate_settings() RETURNS trigger LANGUAGE plpgsql SET search_path = public AS $$
DECLARE slide jsonb; ver numeric;
BEGIN
 IF NOT (NEW.payload ?& ARRAY['homeTitle','homeSubtitle','guestEnabled','maintenanceEnabled','maintenanceMessage','sections']) OR jsonb_typeof(NEW.payload) IS DISTINCT FROM 'object' OR jsonb_typeof(NEW.payload->'homeTitle') IS DISTINCT FROM 'string' OR length(btrim(NEW.payload->>'homeTitle')) NOT BETWEEN 1 AND 120 OR jsonb_typeof(NEW.payload->'homeSubtitle') IS DISTINCT FROM 'string' OR length(NEW.payload->>'homeSubtitle') > 300 OR jsonb_typeof(NEW.payload->'guestEnabled') IS DISTINCT FROM 'boolean' OR jsonb_typeof(NEW.payload->'maintenanceEnabled') IS DISTINCT FROM 'boolean' OR jsonb_typeof(NEW.payload->'maintenanceMessage') IS DISTINCT FROM 'string' OR length(btrim(NEW.payload->>'maintenanceMessage')) NOT BETWEEN 1 AND 500 OR jsonb_typeof(NEW.payload->'sections') IS DISTINCT FROM 'array' THEN RAISE EXCEPTION 'Invalid app settings'; END IF;
 IF jsonb_array_length(NEW.payload->'sections') <> 4 OR NOT (NEW.payload->'sections' @> '["journey","nextAction","progress","guidance"]'::jsonb) THEN RAISE EXCEPTION 'Preserve all home sections'; END IF;
 IF NEW.payload ? 'onboardingVersion' THEN
  IF jsonb_typeof(NEW.payload->'onboardingVersion') IS DISTINCT FROM 'number' THEN RAISE EXCEPTION 'Invalid onboarding version'; END IF;
  ver := (NEW.payload->>'onboardingVersion')::numeric;
  IF ver <> trunc(ver) OR ver NOT BETWEEN 1 AND 10000 THEN RAISE EXCEPTION 'Invalid onboarding version'; END IF;
 END IF;
 IF NEW.payload ? 'onboardingSlides' THEN
  IF jsonb_typeof(NEW.payload->'onboardingSlides') IS DISTINCT FROM 'array' THEN RAISE EXCEPTION 'Invalid onboarding slides'; END IF;
  IF jsonb_array_length(NEW.payload->'onboardingSlides') NOT BETWEEN 2 AND 4 THEN RAISE EXCEPTION 'Use two to four introduction slides'; END IF;
  FOR slide IN SELECT value FROM jsonb_array_elements(NEW.payload->'onboardingSlides') LOOP
   IF NOT (slide ?& ARRAY['title','body','image']) OR jsonb_typeof(slide->'title') IS DISTINCT FROM 'string' OR length(btrim(slide->>'title')) NOT BETWEEN 1 AND 120 OR jsonb_typeof(slide->'body') IS DISTINCT FROM 'string' OR length(btrim(slide->>'body')) NOT BETWEEN 1 AND 500 OR slide->>'image' NOT IN ('building','planning','family') THEN RAISE EXCEPTION 'Invalid introduction slide'; END IF;
  END LOOP;
 END IF;
 NEW.revision := OLD.revision + 1; NEW.updated_at := now(); NEW.updated_by := auth.uid(); RETURN NEW;
END $$;
REVOKE ALL ON FUNCTION public.finnest_validate_settings() FROM PUBLIC, anon, authenticated;
UPDATE public.finnest_app_settings SET payload = payload || '{"onboardingVersion":1,"onboardingSlides":[{"title":"Buying a home is a big decision.","body":"There are questions to ask, documents to understand and choices to make. You do not have to figure it all out at once.","image":"building"},{"title":"Know what to check, one step at a time.","body":"Keep your own questions and notes together, and see what still needs attention.","image":"planning"},{"title":"Move forward with more clarity.","body":"A checklist is a way to stay oriented—not an approval to buy. Get qualified help when you need it.","image":"family"}]}'::jsonb WHERE state IN ('draft','published') AND NOT payload ? 'onboardingSlides';
DO $$ BEGIN
 IF has_table_privilege('anon','public.finnest_app_settings','UPDATE') THEN RAISE EXCEPTION 'Guest edits forbidden'; END IF;
 IF has_table_privilege('authenticated','public.finnest_settings_history','INSERT') THEN RAISE EXCEPTION 'Audit must stay trigger-only'; END IF;
END $$;