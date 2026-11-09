import React, { useCallback, useEffect, useRef, useState } from 'react';
import { RefreshCw, ShieldAlert } from 'lucide-react';
import { supabase } from './supabase';

const filters = [
  ['all', 'All topics'], ['general', 'General'],
  ['new_property', 'New property'], ['resale_property', 'Resale property'],
];
const names = Object.fromEntries(filters);
const PAGE_SIZE = 50;

// The explicit published filter is required even for administrators, whose RLS can also allow drafts.
export async function loadPublishedGuidance(client, category) {
  let query = client.from('finnest_guidance')
    .select('id,title,body,category,status,updated_at')
    .eq('status', 'published');
  if (category !== 'all') query = query.eq('category', category);
  const { data, error } = await query.order('updated_at', { ascending: false }).order('id', { ascending: false }).limit(PAGE_SIZE + 1);
  if (error) throw error;
  if (!Array.isArray(data)) throw new Error('Invalid response');
  // Defense in depth: never render a draft or a different category, even if the service response is unexpected.
  const safe = data.filter(item => item && item.status === 'published' && names[item.category] && item.category !== 'all' &&
    (category === 'all' || item.category === category) && typeof item.id === 'string' && typeof item.title === 'string' && typeof item.body === 'string');
  return { items: safe.slice(0, PAGE_SIZE), truncated: data.length > PAGE_SIZE };
}

export default function Guidance() {
  const [category, setCategory] = useState('all');
  const [state, setState] = useState({ kind: 'loading', items: [], truncated: false });
  const request = useRef(0);
  const reload = useCallback(async () => {
    const current = ++request.current;
    if (!supabase) { setState({ kind: 'unconfigured', items: [], truncated: false }); return; }
    setState({ kind: 'loading', items: [], truncated: false });
    try {
      const result = await loadPublishedGuidance(supabase, category);
      if (current === request.current) setState({ kind: 'ready', ...result });
    } catch {
      if (current === request.current) setState({ kind: 'error', items: [], truncated: false });
    }
  }, [category]);
  useEffect(() => { reload(); return () => { request.current += 1; }; }, [reload]);
  return <>
    <div className="page-head"><span className="eyebrow">GUIDANCE LIBRARY</span><h1>Helpful guidance<span className="red-dot">.</span></h1><p>Explore published information for your home-buying journey.</p></div>
    <div className="notice"><ShieldAlert size={20}/><span>Guidance is general information, not a property verification or a substitute for qualified legal, financial or technical advice.</span></div>
    <section className="card guidance-toolbar" aria-label="Guidance filters">
      <label>Category<select value={category} onChange={event => setCategory(event.target.value)}>{filters.map(([id, title]) => <option value={id} key={id}>{title}</option>)}</select></label>
      <button type="button" className="outline-button" onClick={reload} disabled={state.kind === 'loading'}><RefreshCw size={16}/> Refresh</button>
    </section>
    {state.kind === 'loading' && <p className="guidance-state" role="status">Loading published guidance…</p>}
    {state.kind === 'unconfigured' && <p className="guidance-state" role="status">Guidance is not configured for this website yet.</p>}
    {state.kind === 'error' && <div className="guidance-state" role="alert"><p>Could not load guidance. Check your connection and try again.</p><button className="outline-button" onClick={reload}>Try again</button></div>}
    {state.kind === 'ready' && <div aria-live="polite">
      {state.items.length === 0 ? <div className="card guidance-empty"><h2>No published guidance yet</h2><p>Check another category or return later.</p></div> : <div className="guidance-list">{state.items.map(item => <article className="card guidance-item" key={item.id}>
        <span className="eyebrow">{names[item.category]}</span><h2>{item.title}</h2><p className="guidance-body">{item.body}</p>
      </article>)}</div>}
      {state.truncated && <p className="footnote guidance-limit">Showing the latest 50 items for this filter. Older items are not available in this view yet.</p>}
    </div>}
  </>;
}
