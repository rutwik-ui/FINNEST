import { useEffect, useState } from 'react';
import { ArrowDown, ArrowUp, Plus, Save, Trash2, Upload } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { supabase } from './supabase';
import { settingsSchema, defaultSettings, type AppSettings } from './appSettings';
import type { Json } from '@/integrations/supabase/types';
import { sameJourney } from './offlineStore';
const labels = { journey: 'Selected journey', nextAction: 'Next action', progress: 'Progress', guidance: 'Guidance' };
export default function AdminSettings() {
  const [form, setForm] = useState<AppSettings>(defaultSettings), [revision, setRevision] = useState(0), [publishedRevision, setPublishedRevision] = useState(0);
  const [busy, setBusy] = useState(false), [ready, setReady] = useState(false), [error, setError] = useState(''), [message, setMessage] = useState('');
  const [history, setHistory] = useState<{ id: number; state: string; revision: number; occurred_at: string }[]>([]);
  async function load() {
    const { data, error: err } = await supabase.from('finnest_app_settings').select('state,payload,revision');
    if (err) { setError('Could not load settings. Check your access and retry.'); return; }
    const draft = data.find(r => r.state === 'draft'), published = data.find(r => r.state === 'published');
    const parsed = settingsSchema.safeParse(draft?.payload);
    if (!draft || !published || !parsed.success) { setError('Settings are unavailable.'); return; }
    setForm(parsed.data); setRevision(draft.revision); setPublishedRevision(published.revision); setReady(true);
    const audit = await supabase.from('finnest_settings_history').select('id,state,revision,occurred_at').order('occurred_at', { ascending: false }).limit(20);
    setHistory(audit.data || []);
  }
  useEffect(() => { void load(); }, []);
  async function save(publish: boolean) {
    if (busy || !ready || !navigator.onLine) return;
    const parsed = settingsSchema.safeParse(form); if (!parsed.success) { setError(parsed.error.issues[0]?.message || 'Check your settings.'); return; }
    if (publish && !window.confirm('Publish these settings to all FINNEST buyers?')) return;
    setBusy(true); setError(''); setMessage('');
    const state = publish ? 'published' : 'draft';
    const expected = publish ? publishedRevision : revision;
    try {
      const result = await supabase.from('finnest_app_settings').update({ payload: parsed.data as unknown as Json }).eq('state', state).eq('revision', expected).select('payload,revision').single();
      const returned = settingsSchema.safeParse(result.data?.payload);
      if (result.error || !returned.success || !sameJourney(returned.data, parsed.data)) setError('Settings changed elsewhere or could not be saved. Reload before retrying.');
      else { setMessage(publish ? 'Published. Buyers receive the update on their next sync.' : 'Draft saved. The published app is unchanged.'); if (publish) setPublishedRevision(result.data.revision); else setRevision(result.data.revision); }
    } catch { setError('Settings could not be saved. Check your connection and reload before retrying.'); }
    finally { setBusy(false); }
    const audit = await supabase.from('finnest_settings_history').select('id,state,revision,occurred_at').order('occurred_at', { ascending: false }).limit(20); setHistory(audit.data || []);
  }
  return <><div className="panel-head"><div><h1>App settings</h1><p>Shared website content and access preferences. Native launcher icons require a new APK.</p></div><Button variant="outline" onClick={() => { setError(''); void load(); }}>Reload</Button></div>
    {error && <p className="auth-error" role="alert">{error}</p>}{message && <p className="auth-message" role="status">{message}</p>}
    <form className="settings-form" onSubmit={e => { e.preventDefault(); void save(false); }}>
      <label>Home title<input maxLength={120} value={form.homeTitle} onChange={e => setForm({ ...form, homeTitle: e.target.value })}/></label>
      <label>Home introduction<textarea rows={3} maxLength={300} value={form.homeSubtitle} onChange={e => setForm({ ...form, homeSubtitle: e.target.value })}/></label>
      <label className="settings-toggle"><input type="checkbox" checked={form.guestEnabled} onChange={e => setForm({ ...form, guestEnabled: e.target.checked })}/>Allow buyers to continue as guests</label>
      <label className="settings-toggle"><input type="checkbox" checked={form.maintenanceEnabled} onChange={e => setForm({ ...form, maintenanceEnabled: e.target.checked })}/>Show maintenance notice</label>
      <label>Maintenance message<textarea maxLength={500} rows={3} value={form.maintenanceMessage} onChange={e => setForm({ ...form, maintenanceMessage: e.target.value })}/></label>
      <label>Introduction version<input type="number" min={1} max={10000} value={form.onboardingVersion} onChange={e => setForm({ ...form, onboardingVersion: Number(e.target.value) })}/></label>
      <fieldset className="settings-introduction"><legend>Introduction slides</legend>{form.onboardingSlides.map((slide, i) => <div className="settings-slide" key={i}><h3>Slide {i + 1}</h3><label>Title<input maxLength={120} value={slide.title} onChange={e => setForm({ ...form, onboardingSlides: form.onboardingSlides.map((s, n) => n === i ? { ...s, title: e.target.value } : s) })}/></label><label>Introduction<textarea rows={3} maxLength={500} value={slide.body} onChange={e => setForm({ ...form, onboardingSlides: form.onboardingSlides.map((s, n) => n === i ? { ...s, body: e.target.value } : s) })}/></label><label>Photo<select value={slide.image} onChange={e => { const image = e.target.value; if (image !== 'building' && image !== 'planning' && image !== 'family') return; setForm({ ...form, onboardingSlides: form.onboardingSlides.map((s, n) => n === i ? { ...s, image } : s) }); }}><option value="building">Apartment building</option><option value="planning">Home planning</option><option value="family">Family with keys</option></select></label><div className="settings-actions">{[-1, 1].map(delta => <Button type="button" variant="ghost" size="icon" key={delta} aria-label={`Move slide ${i + 1} ${delta < 0 ? 'up' : 'down'}`} disabled={i + delta < 0 || i + delta >= form.onboardingSlides.length} onClick={() => { const slides = [...form.onboardingSlides]; const other = slides[i + delta]; if (!other) return; slides[i + delta] = slide; slides[i] = other; setForm({ ...form, onboardingSlides: slides }); }}>{delta < 0 ? <ArrowUp/> : <ArrowDown/>}</Button>)}<Button type="button" variant="outline" disabled={form.onboardingSlides.length <= 2} onClick={() => setForm({ ...form, onboardingSlides: form.onboardingSlides.filter((_, n) => n !== i) })}>Remove slide</Button></div></div>)}<Button type="button" variant="outline" disabled={form.onboardingSlides.length >= 4} onClick={() => setForm({ ...form, onboardingSlides: [...form.onboardingSlides, { title: 'Your next step', body: 'Keep your questions and notes together.', image: 'planning' }] })}>Add slide</Button></fieldset>
      <fieldset className="settings-introduction settings-announcements"><legend>Announcements</legend>
        <p className="settings-help">Shown to every buyer under the notification bell on the website and in the Android app after you publish settings. Newest first; up to 20.</p>
        {form.announcements.length === 0 && <p className="settings-help">No announcements yet.</p>}
        {form.announcements.map((a, i) => <div className="settings-slide" key={a.id}><h3>Announcement {i + 1}<span className="settings-date"> · {new Date(a.publishedAt).toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' })}</span></h3>
          <label>Title<input maxLength={120} value={a.title} onChange={e => setForm({ ...form, announcements: form.announcements.map((x, n) => n === i ? { ...x, title: e.target.value } : x) })}/></label>
          <label>Message<textarea rows={3} maxLength={500} value={a.body} onChange={e => setForm({ ...form, announcements: form.announcements.map((x, n) => n === i ? { ...x, body: e.target.value } : x) })}/></label>
          <Button type="button" variant="outline" onClick={() => setForm({ ...form, announcements: form.announcements.filter((_, n) => n !== i) })}><Trash2/>Remove</Button>
        </div>)}
        <Button type="button" variant="outline" disabled={form.announcements.length >= 20} onClick={() => setForm({ ...form, announcements: [{ id: crypto.randomUUID(), title: '', body: '', publishedAt: new Date().toISOString() }, ...form.announcements] })}><Plus/>New announcement</Button>
      </fieldset>
      <div><h2>Home section order</h2><ol className="settings-order">{form.sections.map((id, i) => <li key={id}><span>{labels[id]}</span>{[-1, 1].map(delta => <Button key={delta} variant="ghost" size="icon" type="button" aria-label={`Move ${labels[id]} ${delta < 0 ? 'up' : 'down'}`} disabled={i + delta < 0 || i + delta >= form.sections.length} onClick={() => { const sections = [...form.sections]; const other = sections[i + delta]; if (!other) return; sections[i + delta] = id; sections[i] = other; setForm({ ...form, sections }); }}>{delta < 0 ? <ArrowUp/> : <ArrowDown/>}</Button>)}</li>)}</ol></div>
      <div className="settings-actions"><Button type="submit" disabled={busy || !ready}><Save/>Save draft</Button><Button type="button" variant="outline" disabled={busy || !ready} onClick={() => { void save(true); }}><Upload/>Publish settings</Button></div>
    </form><h2>Change history</h2><ul className="activity-list">{history.map(h => <li key={h.id}>{h.state === 'published' ? 'Published' : 'Draft saved'} · revision {h.revision}<time>{new Date(h.occurred_at).toLocaleString('en-IN')}</time></li>)}</ul></>;
}