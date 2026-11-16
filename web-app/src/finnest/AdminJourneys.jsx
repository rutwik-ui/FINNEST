import React, { useCallback, useEffect, useState } from 'react';
import { RefreshCw, Search, TriangleAlert } from 'lucide-react';
import { supabase } from './supabase';

/**
 * Support view of buyer journeys.
 *
 * Super admins only, and the database additionally requires an AAL2 session, so
 * this is not merely a hidden button — an unverified session reads zero rows.
 * Deliberately read-only: staff must never edit a buyer's own answers.
 */
export default function AdminJourneys() {
  const [rows, setRows] = useState(null);
  const [query, setQuery] = useState('');
  const [selected, setSelected] = useState(null);
  const [fields, setFields] = useState(null);
  const [msg, setMsg] = useState('');

  const load = useCallback(async () => {
    const { data, error } = await supabase
      .from('finnest_draft_fields')
      .select('user_id,journey_type,updated_at')
      .order('updated_at', { ascending: false })
      .limit(1000);
    if (error) { setRows([]); setMsg('Could not load buyer journeys. Super admin and two-factor verification are required.'); return; }
    // Collapse to one row per buyer + journey with a count of answers.
    const map = new Map();
    for (const r of data || []) {
      const key = `${r.user_id}::${r.journey_type}`;
      const seen = map.get(key);
      if (seen) { seen.count += 1; if (r.updated_at > seen.updated_at) seen.updated_at = r.updated_at; }
      else map.set(key, { user_id: r.user_id, journey_type: r.journey_type, updated_at: r.updated_at, count: 1 });
    }
    setRows([...map.values()].sort((a, b) => (a.updated_at < b.updated_at ? 1 : -1)));
  }, []);
  useEffect(() => { load(); }, [load]);

  const open = async row => {
    setSelected(row); setFields(null);
    const { data, error } = await supabase
      .from('finnest_draft_fields')
      .select('field_key,value,client_updated_at')
      .eq('user_id', row.user_id)
      .eq('journey_type', row.journey_type)
      .order('field_key')
      .limit(1000);
    setFields(error ? [] : (data || []));
  };

  if (rows === null) return <p>Loading buyer journeys…</p>;

  const filtered = query.trim()
    ? rows.filter(r => r.user_id.includes(query.trim().toLowerCase()) || r.journey_type.includes(query.trim().toLowerCase()))
    : rows;

  return <div className="admin-stack">
    <section className="card form-card">
      <div className="section-heading">
        <h3>Buyer journeys</h3>
        <button className="text-link" onClick={load}><RefreshCw size={14} />Refresh</button>
      </div>
      <div className="notice"><TriangleAlert size={18} /><span>Buyer answers are private. Use this only to help someone who asked for support, never to browse or copy data. Every view is limited to super admins with two-factor verification.</span></div>
      <label>Find a buyer<input placeholder="Paste the buyer's user id" value={query} onChange={e => setQuery(e.target.value)} /></label>
      {msg && <p className="footnote">{msg}</p>}
      {filtered.length === 0 ? <div className="empty"><Search size={40} /><h2>No journeys found</h2><p>Buyer journeys appear here after they sign in on the website or the Android app and save an answer.</p></div>
        : <ul className="admin-table">
          {filtered.map(r => <li key={`${r.user_id}::${r.journey_type}`}>
            <div>
              <strong>{r.journey_type === 'resale_property' ? 'Resale Property' : 'New Property'}</strong>
              <span className="footnote">{r.user_id}</span>
            </div>
            <span className="footnote">{r.count} answers · {new Date(r.updated_at).toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' })}</span>
            <button className="text-link" onClick={() => open(r)}>View</button>
          </li>)}
        </ul>}
    </section>

    {selected && <section className="card form-card">
      <div className="section-heading"><h3>Answers</h3><button className="text-link" onClick={() => { setSelected(null); setFields(null); }}>Close</button></div>
      <p className="footnote">Read-only. Editing a buyer's answers is not supported by design.</p>
      {fields === null ? <p>Loading…</p> : fields.length === 0 ? <p className="footnote">No answers stored.</p>
        : <ul className="admin-field-list">
          {fields.map(f => <li key={f.field_key}>
            <code>{f.field_key}</code>
            <span>{f.value || <em>empty</em>}</span>
          </li>)}
        </ul>}
    </section>}
  </div>;
}
