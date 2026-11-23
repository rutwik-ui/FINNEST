import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { supabase } from './supabase';

// Validated pair (light surface): passes lightness, chroma, CVD and contrast checks.
const SERIES = [{ key: 'web', label: 'Website', color: '#2F6FDE' }, { key: 'android', label: 'Android app', color: '#E7373E' }];
const RANGES = [[7, '7 days'], [14, '14 days'], [30, '30 days']];
const EVENT_LABELS = { signed_in: 'Signed in', signed_up: 'Created an account', step_saved: 'Saved journey progress', support_requested: 'Sent a support request', support_request: 'Sent a support request', document_uploaded: 'Uploaded a document' };

const dayKey = d => { const x = new Date(d); return `${x.getFullYear()}-${String(x.getMonth() + 1).padStart(2, '0')}-${String(x.getDate()).padStart(2, '0')}`; };
const dayLabel = key => new Date(`${key}T00:00:00`).toLocaleDateString('en-IN', { day: 'numeric', month: 'short' });
function lastDays(n) { const out = []; const today = new Date(); for (let i = n - 1; i >= 0; i--) { const d = new Date(today); d.setDate(today.getDate() - i); out.push(dayKey(d)); } return out; }
const niceMax = v => { if (v <= 4) return 4; const p = 10 ** Math.floor(Math.log10(v)); const m = v / p; return (m <= 2 ? 2 : m <= 5 ? 5 : 10) * p; };

/** Live usage figures for the admin Overview: who is active on each platform, sign-ups and recent events. */
export function AdminInsights() {
  const [range, setRange] = useState(14);
  const [data, setData] = useState(null), [error, setError] = useState('');
  const load = useCallback(async () => { try { await fetchAll(); } catch { setError('Usage figures could not be loaded.'); setData({ activity: [], profiles: [], journeys: [] }); } }, []);
  const fetchAll = async () => {
    setError('');
    const since = new Date(); since.setDate(since.getDate() - 30); since.setHours(0, 0, 0, 0);
    const [activity, profiles, journeys] = await Promise.all([
      supabase.from('finnest_activity').select('user_id,event,platform,occurred_at').gte('occurred_at', since.toISOString()).order('occurred_at', { ascending: false }).limit(5000),
      supabase.from('finnest_profiles').select('user_id,email,created_at').gte('created_at', since.toISOString()).limit(5000),
      supabase.from('finnest_journeys').select('journey_type,user_id').limit(10000),
    ]);
    if (activity.error) { setError('Usage figures need access to the activity log.'); setData({ activity: [], profiles: profiles.data || [], journeys: journeys.data || [] }); return; }
    setData({ activity: activity.data || [], profiles: profiles.data || [], journeys: journeys.data || [] });
  };
  useEffect(() => {
    load();
    let channel = null;
    try { channel = supabase.channel(`admin-activity-${crypto.randomUUID()}`).on('postgres_changes', { event: 'INSERT', schema: 'public', table: 'finnest_activity' }, load).subscribe(); } catch { /* live updates unavailable */ }
    return () => { if (channel) supabase.removeChannel(channel); };
  }, [load]);

  const view = useMemo(() => {
    if (!data) return null;
    const days = lastDays(range);
    const inRange = new Set(days);
    const active = Object.fromEntries(SERIES.map(s => [s.key, Object.fromEntries(days.map(d => [d, new Set()]))]));
    const totals = Object.fromEntries(SERIES.map(s => [s.key, new Set()]));
    for (const row of data.activity) {
      const d = dayKey(row.occurred_at); const p = row.platform === 'android' ? 'android' : 'web';
      if (!inRange.has(d)) continue;
      active[p][d].add(row.user_id); totals[p].add(row.user_id);
    }
    const both = [...totals.web].filter(id => totals.android.has(id)).length;
    const signups = Object.fromEntries(days.map(d => [d, 0]));
    for (const p of data.profiles) { const d = dayKey(p.created_at); if (d in signups) signups[d] += 1; }
    const journeyUsers = { new: new Set(), resale: new Set() };
    for (const j of data.journeys) journeyUsers[j.journey_type === 'resale' ? 'resale' : 'new']?.add(j.user_id);
    return {
      days,
      lines: SERIES.map(s => ({ ...s, values: days.map(d => active[s.key][d].size) })),
      signups: days.map(d => signups[d]),
      totals: { web: totals.web.size, android: totals.android.size, both, newJourney: journeyUsers.new.size, resale: journeyUsers.resale.size },
      recent: data.activity.slice(0, 8),
    };
  }, [data, range]);

  return <section className="insights" aria-label="Usage">
    <div className="insights-head"><h2>Usage</h2>
      <div className="segmented" role="group" aria-label="Date range">{RANGES.map(([n, l]) => <button key={n} type="button" aria-pressed={range === n} className={range === n ? 'on' : ''} onClick={() => setRange(n)}>{l}</button>)}</div>
    </div>
    {error && <p className="footnote" role="status">{error}</p>}
    {!view ? <div className="content-skeleton" role="status" aria-label="Loading usage" style={{ height: 220 }}/> : <>
      <div className="stat-row">
        <Stat label="Active on the website" value={view.totals.web} note={`last ${range} days`}/>
        <Stat label="Active on Android" value={view.totals.android} note={`last ${range} days`}/>
        <Stat label="Used both" value={view.totals.both} note="same account on web and phone"/>
        <Stat label="Journeys started" value={view.totals.newJourney + view.totals.resale} note={`${view.totals.newJourney} new · ${view.totals.resale} resale`}/>
      </div>
      <div className="chart-grid">
        <figure className="card chart-card"><figcaption><strong>Daily active people</strong><span>People who saved progress, signed in or used the app, by platform</span></figcaption>
          <LineChart days={view.days} series={view.lines}/></figure>
        <figure className="card chart-card"><figcaption><strong>New accounts</strong><span>Accounts created per day</span></figcaption>
          <BarChart days={view.days} values={view.signups} label="New accounts"/></figure>
      </div>
      <div className="card recent-card"><h3>Latest activity</h3>
        {view.recent.length === 0 ? <p className="footnote">No activity yet.</p> : <ul>{view.recent.map((r, i) => <li key={i}>
          <span className="platform-dot" style={{ background: r.platform === 'android' ? SERIES[1].color : SERIES[0].color }} aria-hidden="true"/>
          <span>{EVENT_LABELS[r.event] ?? r.event.replace(/_/g, ' ')}</span><small>{r.platform === 'android' ? 'Android' : 'Website'} · {new Date(r.occurred_at).toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' })}</small></li>)}</ul>}
      </div>
    </>}
  </section>;
}

function Stat({ label, value, note }) {
  return <div className="stat"><span>{label}</span><strong>{value}</strong><small>{note}</small></div>;
}

const W = 560, H = 220, PAD = { l: 34, r: 16, t: 14, b: 26 };
function useScale(days, max) {
  const iw = W - PAD.l - PAD.r, ih = H - PAD.t - PAD.b;
  const x = i => PAD.l + (days.length === 1 ? iw / 2 : (i * iw) / (days.length - 1));
  const y = v => PAD.t + ih - (v / max) * ih;
  return { x, y, iw, ih };
}
function Axes({ days, max, y, band }) {
  const ticks = [0, max / 2, max];
  const every = Math.ceil(days.length / 7);
  return <g className="axes">
    {ticks.map(t => <g key={t}><line x1={PAD.l} x2={W - PAD.r} y1={y(t)} y2={y(t)}/><text x={PAD.l - 8} y={y(t)} dy="0.32em" textAnchor="end">{t}</text></g>)}
    {days.map((d, i) => i % every === 0 && <text key={d} x={band(i)} y={H - 6} textAnchor="middle">{dayLabel(d)}</text>)}
  </g>;
}

function LineChart({ days, series }) {
  const [hover, setHover] = useState(null);
  const [table, setTable] = useState(false);
  const max = niceMax(Math.max(1, ...series.flatMap(s => s.values)));
  const { x, y } = useScale(days, max);
  const pick = e => { const r = e.currentTarget.getBoundingClientRect(); const px = ((e.clientX - r.left) / r.width) * W; let best = 0; days.forEach((_, i) => { if (Math.abs(x(i) - px) < Math.abs(x(best) - px)) best = i; }); setHover(best); };
  return <>
    <div className="chart-legend">{series.map(s => <span key={s.key}><i style={{ background: s.color }}/>{s.label}</span>)}<button type="button" className="text-link" onClick={() => setTable(v => !v)}>{table ? 'Show chart' : 'Show table'}</button></div>
    {table ? <DataTable days={days} columns={series.map(s => [s.label, s.values])}/> : <div className="chart-box">
      <svg viewBox={`0 0 ${W} ${H}`} role="img" aria-label={`Daily active people: ${series.map(s => `${s.label} ${s.values.reduce((a, b) => a + b, 0)} active-days`).join(', ')}`} onMouseMove={pick} onMouseLeave={() => setHover(null)}>
        <Axes days={days} max={max} y={y} band={x}/>
        {hover !== null && <line className="crosshair" x1={x(hover)} x2={x(hover)} y1={PAD.t} y2={H - PAD.b}/>}
        {series.map(s => <g key={s.key}>
          <polyline fill="none" stroke={s.color} strokeWidth="2" strokeLinejoin="round" strokeLinecap="round" points={s.values.map((v, i) => `${x(i)},${y(v)}`).join(' ')}/>
          {hover !== null && <circle cx={x(hover)} cy={y(s.values[hover])} r="4.5" fill={s.color} stroke="#fff" strokeWidth="2"/>}
        </g>)}
      </svg>
      {hover !== null && <div className="chart-tip" style={{ left: `${(x(hover) / W) * 100}%` }}><strong>{dayLabel(days[hover])}</strong>{series.map(s => <span key={s.key}><i style={{ background: s.color }}/>{s.label}<b>{s.values[hover]}</b></span>)}</div>}
    </div>}
  </>;
}

function BarChart({ days, values, label }) {
  const [hover, setHover] = useState(null);
  const [table, setTable] = useState(false);
  const max = niceMax(Math.max(1, ...values));
  const { y, iw } = useScale(days, max);
  const step = iw / days.length, bw = Math.max(4, Math.min(22, step - 4));
  const cx = i => PAD.l + step * i + step / 2;
  return <>
    <div className="chart-legend"><span className="muted">Total {values.reduce((a, b) => a + b, 0)}</span><button type="button" className="text-link" onClick={() => setTable(v => !v)}>{table ? 'Show chart' : 'Show table'}</button></div>
    {table ? <DataTable days={days} columns={[[label, values]]}/> : <div className="chart-box">
      <svg viewBox={`0 0 ${W} ${H}`} role="img" aria-label={`${label}: ${values.reduce((a, b) => a + b, 0)} in this period`} onMouseLeave={() => setHover(null)}>
        <Axes days={days} max={max} y={y} band={cx}/>
        {values.map((v, i) => {
          const top = y(v), base = y(0), h = Math.max(0, base - top), r = Math.min(4, h);
          const left = cx(i) - bw / 2;
          return <g key={days[i]} onMouseEnter={() => setHover(i)}>
            <rect x={PAD.l + step * i} y={PAD.t} width={step} height={H - PAD.t - PAD.b} fill="transparent"/>
            {h > 0 && <path fill="#2F6FDE" opacity={hover === null || hover === i ? 1 : 0.55} d={`M${left},${base}V${top + r}Q${left},${top} ${left + r},${top}H${left + bw - r}Q${left + bw},${top} ${left + bw},${top + r}V${base}Z`}/>}
          </g>;
        })}
      </svg>
      {hover !== null && <div className="chart-tip" style={{ left: `${(cx(hover) / W) * 100}%` }}><strong>{dayLabel(days[hover])}</strong><span>{label}<b>{values[hover]}</b></span></div>}
    </div>}
  </>;
}

function DataTable({ days, columns }) {
  return <div className="table-wrap chart-table"><table className="dtable"><thead><tr><th>Day</th>{columns.map(([l]) => <th key={l}>{l}</th>)}</tr></thead>
    <tbody>{days.map((d, i) => <tr key={d}><td>{dayLabel(d)}</td>{columns.map(([l, v]) => <td key={l}>{v[i]}</td>)}</tr>)}</tbody></table></div>;
}
