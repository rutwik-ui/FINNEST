import React, { useCallback, useEffect, useRef, useState } from 'react';
import { RefreshCw, ShieldAlert } from 'lucide-react';
import { supabase } from './supabase';

const filters = [
  ['all', 'All topics'], ['general', 'General'],
  ['new_property', 'New property'], ['resale_property', 'Resale property'],
];
const names = Object.fromEntries(filters);
const PAGE_SIZE = 50;
// Cursors originate from database UUID and timestamptz columns. Validate before embedding in PostgREST's filter grammar.
const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
const TIMESTAMP = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d{1,6})?(?:Z|[+-]\d{2}:\d{2})$/;
const validCursor = cursor => cursor && UUID.test(cursor.id) && TIMESTAMP.test(cursor.updated_at);
const empty = kind => ({ kind, items: [], cursor: null, loadingMore: false, moreError: false });

// Explicit filtering matters even for administrators, whose RLS may also allow drafts.
export async function loadPublishedGuidance(client, category, cursor = null) {
  if (!Object.hasOwn(names, category)) throw new Error('Invalid category');
  let query = client.from('finnest_guidance')
    .select('id,title,body,category,status,updated_at')
    .eq('status', 'published');
  if (category !== 'all') query = query.eq('category', category);
  if (cursor) {
    if (!validCursor(cursor)) throw new Error('Invalid cursor');
    // Keyset pagination uses both columns so records sharing a timestamp aren't skipped.
    query = query.or(`updated_at.lt."${cursor.updated_at}",and(updated_at.eq."${cursor.updated_at}",id.lt.${cursor.id})`);
  }
  const { data, error } = await query.order('updated_at', { ascending: false }).order('id', { ascending: false }).limit(PAGE_SIZE + 1);
  if (error) throw error;
  if (!Array.isArray(data)) throw new Error('Invalid response');
  const page = data.slice(0, PAGE_SIZE);
  const nextCursor = data.length > PAGE_SIZE ? page.at(-1) : null;
  if (nextCursor && !validCursor(nextCursor)) throw new Error('Invalid pagination data');
  // Defense in depth: do not render a draft or a different category even for a privileged session.
  const items = page.filter(item => item && item.status === 'published' && names[item.category] && item.category !== 'all' &&
    (category === 'all' || item.category === category) && typeof item.id === 'string' && typeof item.title === 'string' && typeof item.body === 'string');
  return { items, cursor: nextCursor ? { id: nextCursor.id, updated_at: nextCursor.updated_at } : null };
}

export default function Guidance() {
  const [category, setCategory] = useState('all');
  const [state, setState] = useState(() => empty('loading'));
  const request = useRef(0);
  const moreBusy = useRef(false);
  const reload = useCallback(async () => {
    const current = ++request.current;
    moreBusy.current = false;
    if (!supabase) { setState(empty('unconfigured')); return; }
    setState(empty('loading'));
    try {
      const result = await loadPublishedGuidance(supabase, category);
      if (current === request.current) setState({ ...empty('ready'), ...result });
    } catch {
      if (current === request.current) setState(empty('error'));
    }
  }, [category]);
  useEffect(() => { reload(); return () => { request.current += 1; }; }, [reload]);
  const loadMore = async () => {
    if (!state.cursor || state.kind !== 'ready' || moreBusy.current) return;
    const current = request.current;
    moreBusy.current = true;
    setState(previous => ({ ...previous, loadingMore: true, moreError: false }));
    try {
      const result = await loadPublishedGuidance(supabase, category, state.cursor);
      if (current !== request.current) return;
      setState(previous => {
        const ids = new Set(previous.items.map(item => item.id));
        const unique = result.items.filter(item => !ids.has(item.id) && (ids.add(item.id), true));
        return { ...previous, items: [...previous.items, ...unique], cursor: result.cursor, loadingMore: false };
      });
    } catch {
      if (current === request.current) setState(previous => ({ ...previous, loadingMore: false, moreError: true }));
    } finally {
      if (current === request.current) moreBusy.current = false;
    }
  };
  return <>
    <div className="page-head"><span className="eyebrow">GUIDANCE LIBRARY</span><h1>Helpful guidance<span className="red-dot">.</span></h1><p>Explore published information for your home-buying journey.</p></div>
    <div className="notice"><ShieldAlert size={20}/><span>Guidance is general information, not a property verification or a substitute for qualified legal, financial or technical advice.</span></div>
    <section className="card guidance-toolbar" aria-label="Guidance filters">
      <label>Category<select value={category} onChange={event => { request.current += 1; setCategory(event.target.value); }}>{filters.map(([id, title]) => <option value={id} key={id}>{title}</option>)}</select></label>
      <button type="button" className="outline-button" onClick={reload} disabled={state.kind === 'loading'}><RefreshCw size={16}/> Refresh</button>
    </section>
    {state.kind === 'loading' && <p className="guidance-state" role="status">Loading published guidance…</p>}
    {state.kind === 'unconfigured' && <p className="guidance-state" role="status">Guidance is not configured for this website yet.</p>}
    {state.kind === 'error' && <div className="guidance-state" role="alert"><p>Could not load guidance. Check your connection and try again.</p><button className="outline-button" onClick={reload}>Try again</button></div>}
    {state.kind === 'ready' && <div aria-live="polite">
      {state.items.length === 0 && !state.cursor ? <div className="card guidance-empty"><h2>No published guidance yet</h2><p>Check another category or return later.</p></div> : <div className="guidance-list">{state.items.map(item => <article className="card guidance-item" key={item.id}>
        <span className="eyebrow">{names[item.category]}</span><h2>{item.title}</h2><p className="guidance-body">{item.body}</p>
      </article>)}</div>}
      {state.moreError && <p className="auth-error" role="alert">Could not load more guidance. Your current results are still shown.</p>}
      {state.cursor && <div className="guidance-more"><button className="outline-button" type="button" onClick={loadMore} disabled={state.loadingMore}>{state.loadingMore ? 'Loading…' : state.moreError ? 'Retry loading more' : 'Load more guidance'}</button></div>}
      <p className="footnote guidance-limit">Published guidance may change; refresh to see the latest version.</p>
    </div>}
  </>;
}
