import { useEffect, useState } from 'react';
import { supabase } from './supabase';
import { defaultSettings, settingsSchema, type AppSettings } from './appSettings';
import { readPublicCache, writePublicCache } from './offlineStore';

export function useAppSettings() {
  const [settings, setSettings] = useState(defaultSettings);
  const [ready, setReady] = useState(false);
  useEffect(() => {
    let active = true;
    const apply = (v: unknown) => { const result = settingsSchema.safeParse(v); if (active && result.success) setSettings(result.data); };
    const load = async () => {
      const { data, error } = await supabase.from('finnest_app_settings').select('payload').eq('state', 'published').single();
      if (!active) return;
      if (!error && data) { apply(data.payload); await writePublicCache('published-settings', data.payload); }
      setReady(true);
    };
    void readPublicCache<AppSettings>('published-settings').then(v => { if (v) apply(v); });
    void load();
    const channel = supabase.channel(`settings-${crypto.randomUUID()}`).on('postgres_changes', { event: 'UPDATE', schema: 'public', table: 'finnest_app_settings', filter: 'state=eq.published' }, () => { void load(); }).subscribe();
    window.addEventListener('online', load);
    return () => { active = false; void supabase.removeChannel(channel); window.removeEventListener('online', load); };
  }, []);
  return { settings, ready };
}