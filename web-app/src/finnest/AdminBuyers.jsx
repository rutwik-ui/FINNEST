import React, { useCallback, useEffect, useState } from 'react';
import { RefreshCw, ArrowLeft, FileText } from 'lucide-react';
import { supabase } from './supabase';
import { useServerFn } from '@tanstack/react-start';
import { supportAssist } from './supportAssist.functions';
import { STEP_KEYS, hasContent } from './cloudSync';

const fmt = d => d ? new Date(d).toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' }) : '—';
const TYPE = { new: 'New Property', resale: 'Resale Property' };
const EVENT = { signed_in: 'Signed in', step_saved: 'Saved step', support_request: 'Sent support request', document_uploaded: 'Uploaded document' };
const platformOf = d => /android/i.test(d || '') ? 'Android' : d ? 'Web' : '—';
const pretty = k => k.replace(/^resale/, '').replace(/([A-Z])/g, ' $1').replace(/^./, c => c.toUpperCase()).trim();
function show(v) {
  if (Array.isArray(v)) return v.map(x => typeof x === 'object' ? (x.name || x.title || x.topic || JSON.stringify(x)) : x).join(', ');
  if (v && typeof v === 'object') return Object.entries(v).filter(([, x]) => hasContent(x)).map(([k, x]) => `${k}: ${typeof x === 'object' ? Object.values(x).filter(Boolean).join(' / ') : x}`).join(' · ');
  return String(v);
}

export default function BuyerProgress({ roles }) {
  const [rows, setRows] = useState(null), [error, setError] = useState(''), [open, setOpen] = useState(null), [q, setQ] = useState('');
  const load = useCallback(async () => {
    setError('');
    const [j, p, a] = await Promise.all([
      supabase.from('finnest_journeys').select('user_id,journey_type,completed_steps,current_step,updated_at,updated_by_device').order('updated_at', { ascending: false }).limit(1000),
      supabase.from('finnest_profiles').select('user_id,email,full_name,created_at'),
      supabase.from('finnest_activity').select('user_id,occurred_at,platform').order('occurred_at', { ascending: false }).limit(1000),
    ]);
    if (j.error || p.error) { setError('Could not load buyer progress. Check your access and try again.'); setRows([]); return; }
    const last = {}; for (const x of a.data || []) if (!last[x.user_id]) last[x.user_id] = x;
    setRows((p.data || []).map(u => ({ ...u, journeys: (j.data || []).filter(x => x.user_id === u.user_id), last: last[u.user_id] }))
      .sort((x, y) => new Date(y.last?.occurred_at || y.created_at) - new Date(x.last?.occurred_at || x.created_at)));
  }, []);
  useEffect(() => { load(); }, [load]);
  useEffect(() => { // live refresh when any buyer saves
    const ch = supabase.channel('admin-journeys-' + Math.random().toString(36).slice(2))
      .on('postgres_changes', { event: '*', schema: 'public', table: 'finnest_journeys' }, () => load())
      .on('postgres_changes', { event: 'INSERT', schema: 'public', table: 'finnest_activity' }, () => load()).subscribe();
    return () => { supabase.removeChannel(ch); };
  }, [load]);
  if (open) return <BuyerDetail user={open} roles={roles} back={() => { setOpen(null); load(); }} />;
  const list = (rows || []).filter(u => !q || (u.email || '').toLowerCase().includes(q.toLowerCase()) || (u.full_name || '').toLowerCase().includes(q.toLowerCase()));
  return <>
    <div className="panel-head"><div><h1>Buyer progress</h1><p>Live journey progress from the website and the Android app. Updates appear automatically.</p></div>
      <div className="panel-tools"><input className="tool-input" placeholder="Search email or name" value={q} onChange={e => setQ(e.target.value)} /><button className="outline-button" onClick={load}><RefreshCw size={14} /> Refresh</button></div></div>
    {error && <p className="auth-error" role="alert">{error}</p>}
    <div className="table-wrap"><table className="dtable"><thead><tr><th>Buyer</th><th>New Property</th><th>Resale Property</th><th>Last activity</th><th>Device</th><th /></tr></thead><tbody>
      {rows === null ? <tr><td colSpan={6}>Loading…</td></tr> : list.length === 0 ? <tr><td colSpan={6} className="empty-cell">No buyers yet.</td></tr> : list.map(u => <tr key={u.user_id}>
        <td><strong>{u.email || '—'}</strong>{u.full_name && <div className="muted-sm">{u.full_name}</div>}</td>
        {['new', 'resale'].map(t => { const j = u.journeys.find(x => x.journey_type === t); return <td key={t}>{j ? <Progress done={j.completed_steps.length} total={STEP_KEYS[t].length} /> : <span className="muted-sm">Not started</span>}</td>; })}
        <td>{fmt(u.last?.occurred_at || u.journeys[0]?.updated_at)}</td>
        <td>{u.last?.platform === 'android' ? 'Android' : u.last ? 'Web' : platformOf(u.journeys[0]?.updated_by_device)}</td>
        <td className="row-actions"><button className="text-link" onClick={() => setOpen(u)}>View</button></td></tr>)}
    </tbody></table></div></>;
}
const Progress = ({ done, total }) => <div className="mini-progress"><span><i style={{ width: `${(done / total) * 100}%` }} /></span>{done} / {total} steps</div>;

function BuyerDetail({ user, roles, back }) {
  const [j, setJ] = useState(null), [acts, setActs] = useState([]), [docs, setDocs] = useState(null);
  const canDocs = roles.some(r => ['super_admin', 'support_manager'].includes(r));
  const load = useCallback(async () => {
    const [a, b, c] = await Promise.all([
      supabase.from('finnest_journeys').select('*').eq('user_id', user.user_id),
      supabase.from('finnest_activity').select('*').eq('user_id', user.user_id).order('occurred_at', { ascending: false }).limit(100),
      canDocs ? supabase.from('finnest_documents').select('*').eq('user_id', user.user_id).order('uploaded_at', { ascending: false }) : Promise.resolve({ data: null }),
    ]);
    setJ(a.data || []); setActs(b.data || []); setDocs(c.data);
  }, [user.user_id, canDocs]);
  useEffect(() => { load(); }, [load]);
  useEffect(() => {
    const ch = supabase.channel('admin-buyer-' + user.user_id + Math.random().toString(36).slice(2))
      .on('postgres_changes', { event: '*', schema: 'public', table: 'finnest_journeys', filter: 'user_id=eq.' + user.user_id }, load).subscribe();
    return () => { supabase.removeChannel(ch); };
  }, [load, user.user_id]);
  const openDoc = async d => { const { data } = await supabase.storage.from('finnest-private-documents').createSignedUrl(d.path, 120); if (data?.signedUrl) window.open(data.signedUrl, '_blank', 'noopener'); };
  return <>
    <div className="panel-head"><div><button className="text-link" onClick={back}><ArrowLeft size={14} /> All buyers</button><h1>{user.email}</h1><p>{user.full_name || 'No name given'} · Joined {fmt(user.created_at)}</p></div></div>
    {j === null ? <p>Loading…</p> : ['new', 'resale'].map(t => { const row = j.find(x => x.journey_type === t); return <section key={t} className="card dform buyer-journey">
      <div className="wide"><h3>{TYPE[t]}</h3>{row ? <p className="muted-sm">Last saved {fmt(row.updated_at)} on {platformOf(row.updated_by_device)} · version {row.version}{row.current_step ? ` · last screen ${row.current_step}` : ''}</p> : <p className="muted-sm">Not started.</p>}</div>
      {row && <div className="wide"><ol className="step-status">{STEP_KEYS[t].map(([label, keys]) => { const done = row.completed_steps.includes(label); return <li key={label} className={done ? 'done' : ''}><strong>{done ? 'Completed' : 'Pending'}</strong> · {label}
        {done && <ul>{keys.filter(k => hasContent(row.payload[k])).map(k => <li key={k}><span>{pretty(k)}:</span> {show(row.payload[k])}</li>)}</ul>}</li>; })}</ol></div>}
    </section>; })}
    {canDocs && <SupportAssistant userId={user.user_id} />}
    {canDocs && <section className="card dform"><div className="wide"><h3>Documents</h3>{docs?.length ? <ul className="doc-list">{docs.map(d => <li key={d.id}><FileText size={14} /><button className="text-link" onClick={() => openDoc(d)}>{d.name}</button><span className="muted-sm">{fmt(d.uploaded_at)}</span></li>)}</ul> : <p className="muted-sm">No documents uploaded.</p>}</div></section>}
    <section className="card dform"><div className="wide"><h3>Activity</h3>{acts.length ? <ul className="activity-list">{acts.map(x => <li key={x.id}><span>{fmt(x.occurred_at)}</span><strong>{EVENT[x.event] || x.event}</strong>{x.detail?.steps && <em>{x.detail.steps.join(', ')}</em>}{x.detail?.name && <em>{x.detail.name}</em>}<small>{x.platform === 'android' ? 'Android' : 'Web'}</small></li>)}</ul> : <p className="muted-sm">No activity recorded.</p>}</div></section>
  </>;
}

function SupportAssistant({ userId }) {
  const run = useServerFn(supportAssist);
  const [notes, setNotes] = useState(''), [busy, setBusy] = useState(false), [out, setOut] = useState(null), [err, setErr] = useState('');
  const go = async () => { setBusy(true); setErr(''); setOut(null); try { setOut(await run({ data: { userId, notes } })); } catch (e) { setErr(e?.message || 'Review failed.'); } finally { setBusy(false); } };
  return <section className="card dform"><div className="wide"><h3>Support assistant</h3>
    <p className="muted-sm">Add your support notes. AI reviews this buyer's saved progress and suggests blockers and one next action. Suggestions are guidance only — check before acting.</p>
    <textarea className="tool-input" rows={4} style={{ width: '100%' }} placeholder="e.g. Buyer called, worried about bank loan rejection and missing OC" value={notes} onChange={e => setNotes(e.target.value)} maxLength={4000} />
    <p><button className="outline-button" onClick={go} disabled={busy}>{busy ? 'Reviewing…' : 'Find blockers & next action'}</button></p>
    {err && <p className="auth-error" role="alert">{err}</p>}
    {out && <div><p><strong>Summary:</strong> {out.summary}</p>
      {out.blockers.length ? <ul>{out.blockers.map((b, i) => <li key={i}><strong>{b.severity.toUpperCase()}</strong> · {b.step}: {b.issue}</li>)}</ul> : <p className="muted-sm">No blockers found.</p>}
      <p><strong>Next action:</strong> {out.next_action}</p><p><strong>Suggested message to buyer:</strong> {out.message_to_buyer}</p></div>}
  </div></section>;
}
