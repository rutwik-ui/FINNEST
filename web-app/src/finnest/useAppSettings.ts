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
      try {
        const { data, error } = await supabase.from('finnest_app_settings').select('payload').eq('state', 'published').single();
        if (!active) return;
        if (!error && data) { apply(data.payload); await writePublicCache('published-settings', data.payload); }
      } finally { if (active) setReady(true); }
    };
    const fallback = window.setTimeout(() => { if (active) setReady(true); }, 5000);
    void readPublicCache<AppSettings>('published-settings').then(v => { if (v) { apply(v); if (active) setReady(true); } }).catch(() => {});
    void load().catch(() => {});
    const reload = () => { void load().catch(() => {}); };
    const channel = supabase.channel(`settings-${crypto.randomUUID()}`).on('postgres_changes', { event: 'UPDATE', schema: 'public', table: 'finnest_app_settings', filter: 'state=eq.published' }, reload).subscribe();
    window.addEventListener('online', reload);
    return () => { active = false; window.clearTimeout(fallback); void supabase.removeChannel(channel); window.removeEventListener('online', reload); };
  }, []);
  return { settings, ready };
}