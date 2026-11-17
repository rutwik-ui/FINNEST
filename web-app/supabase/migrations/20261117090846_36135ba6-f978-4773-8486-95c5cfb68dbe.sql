DROP POLICY "Public reads published app settings" ON public.finnest_app_settings;
DROP POLICY "Super admins read draft settings" ON public.finnest_app_settings;
CREATE POLICY "Visitors read published settings" ON public.finnest_app_settings FOR SELECT TO anon USING (state = 'published');
CREATE POLICY "Accounts read published or authorized draft settings" ON public.finnest_app_settings FOR SELECT TO authenticated USING (state = 'published' OR public.finnest_has_admin_role(ARRAY['super_admin']));