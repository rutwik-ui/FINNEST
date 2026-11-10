import React, { useCallback, useEffect, useState } from 'react';
import { LayoutDashboard, FileText, Users, UserRound, LifeBuoy, ExternalLink, ChevronRight, Home, RefreshCw, LogOut, Plus, Trash2 } from 'lucide-react';
import { supabase } from './supabase';
import Admin from './Admin';
import { BrandMark } from './BrandMark';
import BulkImport from './AdminBulkImport';

export const ADMIN_ROLES = ['super_admin', 'content_manager', 'support_manager', 'expert_manager'];
export const ROLE_LABELS = { super_admin: 'Super admin', content_manager: 'Content manager', support_manager: 'Support manager', expert_manager: 'Expert manager' };
// Which console sections each role can open; the database enforces the same rules.
export const SECTION_ROLES = {
  overview: ADMIN_ROLES,
  guidance: ['super_admin', 'content_manager'],
  users: ['super_admin', 'content_manager', 'support_manager', 'expert_manager'],
  experts: ['super_admin', 'expert_manager'],
  support: ['super_admin', 'support_manager'],
};
export const canOpen = (roles, section) => roles.some(r => (SECTION_ROLES[section] || []).includes(r));

const SECTIONS = [
  ['overview', 'Overview', LayoutDashboard],
  ['guidance', 'Guidance content', FileText],
  ['users', 'Users & roles', Users],
  ['experts', 'Experts', UserRound],
  ['support', 'Support requests', LifeBuoy],
];
const fmt = d => d ? new Date(d).toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' }) : '—';

export default function AdminConsole({ go, session, authLoading, navigationGuardRef }) {
  const [roles, setRoles] = useState(null), [aal, setAal] = useState(null), [section, setSection] = useState('overview');
  const userId = session?.user?.id;
  const check = useCallback(async () => {
    if (!userId) { setRoles(null); setAal(null); return; }
    const [{ data: r }, { data: a }] = await Promise.all([
      supabase.from('finnest_admin_roles').select('role').eq('user_id', userId),
      supabase.auth.mfa.getAuthenticatorAssuranceLevel(),
    ]);
    setRoles((r || []).map(x => x.role)); setAal(a?.currentLevel || null);
  }, [userId]);
  useEffect(() => { check(); }, [check, session?.access_token]);
  const ready = userId && roles?.length && aal === 'aal2';
  const shell = (title, body) => <div className="desk">
    <aside className="desk-side">
      <div className="desk-brand"><BrandMark size={26} /><span>FINNEST</span><small>Admin console</small></div>
      {ready && <><div className="desk-group">Manage</div>
        {SECTIONS.filter(([id]) => canOpen(roles, id)).map(([id, label, Icon]) => <button key={id} className={'desk-link' + (section === id ? ' active' : '')} onClick={() => setSection(id)}><Icon size={16} />{label}</button>)}</>}
      <div className="desk-foot">
        <button className="desk-link" onClick={() => go('/')}><ExternalLink size={16} />Open live website</button>
        {ready && <button className="desk-link" onClick={async () => { await supabase.auth.signOut(); }}><LogOut size={16} />Sign out</button>}
        <p>{ready ? session.user.email : 'Shared backend · Web & Android'}</p>
      </div>
    </aside>
    <div className="desk-body">
      <div className="desk-bar"><span>FINNEST</span><ChevronRight size={14} /><strong>{title}</strong>{ready && <span className="desk-roles">{roles.map(r => ROLE_LABELS[r]).join(', ')}</span>}<span className="desk-status"><i />Connected</span></div>
      <div className="desk-content">{body}</div>
    </div>
  </div>;
  if (!ready) return shell('Sign in', <Admin go={go} session={session} authLoading={authLoading} navigationGuardRef={navigationGuardRef} onAccessChange={check} />);
  const label = SECTIONS.find(s => s[0] === section)?.[1];
  return shell(label, <>
    {section === 'overview' && <Overview roles={roles} open={setSection} />}
    {section === 'guidance' && <Admin go={go} session={session} authLoading={authLoading} navigationGuardRef={navigationGuardRef} />}
    {section === 'users' && <UsersPanel roles={roles} me={userId} />}
    {section === 'experts' && <ExpertsPanel />}
    {section === 'support' && <SupportPanel />}
  </>);
}

function useTable(load) {
  const [state, setState] = useState({ rows: null, error: '' });
  const refresh = useCallback(async () => {
    setState(s => ({ ...s, error: '' }));
    const { data, error } = await load();
    setState({ rows: data || [], error: error ? 'Could not load data. Check your access and try again.' : '' });
  }, [load]);
  useEffect(() => { refresh(); }, [refresh]);
  return [state, refresh];
}
function PanelHead({ title, sub, onRefresh, children }) {
  return <div className="panel-head"><div><h1>{title}</h1>{sub && <p>{sub}</p>}</div><div className="panel-tools">{children}<button className="outline-button" onClick={onRefresh}><RefreshCw size={14} /> Refresh</button></div></div>;
}
const Status = ({ v }) => <span className={'pill pill-' + v}>{String(v).replace('_', ' ')}</span>;

function Overview({ roles, open }) {
  const [stats, setStats] = useState({});
  useEffect(() => {
    const count = async (t, f) => { let q = supabase.from(t).select('*', { count: 'exact', head: true }); if (f) q = f(q); const { count: c } = await q; return c ?? '—'; };
    (async () => setStats({
      users: canOpen(roles, 'users') ? await count('finnest_profiles') : '—',
      guidance: canOpen(roles, 'guidance') ? await count('finnest_guidance', q => q.eq('status', 'published')) : '—',
      experts: canOpen(roles, 'experts') ? await count('finnest_experts', q => q.eq('status', 'published')) : '—',
      tickets: canOpen(roles, 'support') ? await count('finnest_support_tickets', q => q.neq('status', 'resolved')) : '—',
    }))();
  }, [roles]);
  const tiles = [['users', 'Registered users', stats.users], ['guidance', 'Published guidance', stats.guidance], ['experts', 'Published experts', stats.experts], ['support', 'Open support requests', stats.tickets]];
  return <><PanelHead title="Overview" sub="Live figures from the shared database used by the website and the Android app." onRefresh={() => open('overview')} />
    <div className="kpi-grid">{tiles.map(([id, l, v]) => <button key={id} className="kpi" disabled={!canOpen(roles, id)} onClick={() => open(id)}><span>{l}</span><strong>{v ?? '…'}</strong></button>)}</div></>;
}

function UsersPanel({ roles, me }) {
  const isSuper = roles.includes('super_admin');
  const [q, setQ] = useState(''), [msg, setMsg] = useState('');
  const loadUsers = useCallback(() => supabase.from('finnest_profiles').select('user_id,email,full_name,created_at,last_seen_at').order('created_at', { ascending: false }).limit(500), []);
  const loadRoles = useCallback(() => isSuper ? supabase.from('finnest_admin_roles').select('user_id,role') : Promise.resolve({ data: [] }), [isSuper]);
  const [users, refresh] = useTable(loadUsers), [roleRows, refreshRoles] = useTable(loadRoles);
  const rolesOf = id => (roleRows.rows || []).filter(r => r.user_id === id).map(r => r.role);
  const grant = async (id, role) => { setMsg(''); const { error } = await supabase.from('finnest_admin_roles').insert({ user_id: id, role }); setMsg(error ? 'Role could not be granted.' : 'Role granted.'); refreshRoles(); };
  const revoke = async (id, role) => { if (!confirm(`Remove ${ROLE_LABELS[role]}?`)) return; const { error } = await supabase.from('finnest_admin_roles').delete().eq('user_id', id).eq('role', role); setMsg(error ? 'Role could not be removed.' : 'Role removed.'); refreshRoles(); };
  const rows = (users.rows || []).filter(u => !q || (u.email || '').toLowerCase().includes(q.toLowerCase()) || (u.full_name || '').toLowerCase().includes(q.toLowerCase()));
  return <><PanelHead title="Users & roles" sub="Accounts are shared between the website and the Android app." onRefresh={() => { refresh(); refreshRoles(); }}><input className="tool-input" placeholder="Search email or name" value={q} onChange={e => setQ(e.target.value)} /></PanelHead>
    {users.error && <p className="auth-error" role="alert">{users.error}</p>}{msg && <p className="auth-message" role="status">{msg}</p>}
    <div className="table-wrap"><table className="dtable"><thead><tr><th>Email</th><th>Name</th><th>Joined</th>{isSuper && <th>Admin roles</th>}</tr></thead><tbody>
      {users.rows === null ? <tr><td colSpan={4}>Loading…</td></tr> : rows.length === 0 ? <tr><td colSpan={4} className="empty-cell">No users found.</td></tr> : rows.map(u => <tr key={u.user_id}>
        <td>{u.email || '—'}</td><td>{u.full_name || '—'}</td><td>{fmt(u.created_at)}</td>
        {isSuper && <td><div className="role-cell">{rolesOf(u.user_id).map(r => <span key={r} className="pill pill-role">{ROLE_LABELS[r]}{u.user_id !== me && <button aria-label={`Remove ${ROLE_LABELS[r]}`} onClick={() => revoke(u.user_id, r)}><Trash2 size={11} /></button>}</span>)}
          <select className="tool-input sm" value="" onChange={e => e.target.value && grant(u.user_id, e.target.value)}><option value="">+ Grant role</option>{ADMIN_ROLES.filter(r => !rolesOf(u.user_id).includes(r)).map(r => <option key={r} value={r}>{ROLE_LABELS[r]}</option>)}</select></div></td>}
      </tr>)}</tbody></table></div></>;
}

const blankExpert = { name: '', specialty: 'Property lawyer', city: '', phone: '', email: '', bio: '', status: 'draft' };
export const SPECIALTIES = ['Property lawyer', 'CA / tax advisor', 'Home loan advisor', 'Property consultant', 'Architect / technical inspector', 'Interior expert', 'Valuation expert'];
function ExpertsPanel() {
  const load = useCallback(() => supabase.from('finnest_experts').select('*').order('updated_at', { ascending: false }), []);
  const [list, refresh] = useTable(load);
  const [form, setForm] = useState(null), [msg, setMsg] = useState(''), [busy, setBusy] = useState(false);
  const save = async e => {
    e.preventDefault(); if (!form.name.trim()) return setMsg('Enter a name.'); setBusy(true);
    const { id, created_at, updated_at, ...v } = form; const payload = { ...v, name: v.name.trim() };
    const { error } = id ? await supabase.from('finnest_experts').update(payload).eq('id', id) : await supabase.from('finnest_experts').insert(payload);
    setBusy(false); setMsg(error ? 'Could not save expert.' : 'Expert saved.'); if (!error) { setForm(null); refresh(); }
  };
  const remove = async id => { if (!confirm('Delete this expert?')) return; const { error } = await supabase.from('finnest_experts').delete().eq('id', id); setMsg(error ? 'Could not delete.' : 'Expert deleted.'); refresh(); };
  const set = (k, v) => setForm(f => ({ ...f, [k]: v }));
  return <><PanelHead title="Experts" sub="Published experts appear in the website's Experts directory." onRefresh={refresh}><BulkImport kind="experts" disabled={busy || Boolean(form)} onImported={refresh}/><button className="primary compact" onClick={() => setForm(blankExpert)}><Plus size={14} /> New expert</button></PanelHead>
    {msg && <p className="auth-message" role="status">{msg}</p>}
    {form && <form className="card dform" onSubmit={save}>
      <label>Name<input required maxLength={120} value={form.name} onChange={e => set('name', e.target.value)} /></label>
      <label>Speciality<select value={form.specialty} onChange={e => set('specialty', e.target.value)}>{SPECIALTIES.map(s => <option key={s}>{s}</option>)}</select></label>
      <label>City<input value={form.city || ''} onChange={e => set('city', e.target.value)} /></label>
      <label>Phone<input value={form.phone || ''} onChange={e => set('phone', e.target.value)} /></label>
      <label>Email<input type="email" value={form.email || ''} onChange={e => set('email', e.target.value)} /></label>
      <label>Status<select value={form.status} onChange={e => set('status', e.target.value)}><option value="draft">Draft</option><option value="published">Published</option></select></label>
      <label className="wide">Bio<textarea rows={3} value={form.bio || ''} onChange={e => set('bio', e.target.value)} /></label>
      <div className="wide dform-actions"><button className="primary compact" disabled={busy}>{busy ? 'Saving…' : 'Save expert'}</button><button type="button" className="outline-button" onClick={() => setForm(null)}>Cancel</button></div>
    </form>}
    <div className="table-wrap"><table className="dtable"><thead><tr><th>Name</th><th>Speciality</th><th>City</th><th>Status</th><th>Updated</th><th /></tr></thead><tbody>
      {list.rows === null ? <tr><td colSpan={6}>Loading…</td></tr> : list.rows.length === 0 ? <tr><td colSpan={6} className="empty-cell">No experts yet.</td></tr> : list.rows.map(x => <tr key={x.id}><td>{x.name}</td><td>{x.specialty}</td><td>{x.city || '—'}</td><td><Status v={x.status} /></td><td>{fmt(x.updated_at)}</td><td className="row-actions"><button className="text-link" onClick={() => setForm(x)}>Edit</button><button className="text-link" onClick={() => remove(x.id)}>Delete</button></td></tr>)}
    </tbody></table></div></>;
}

function SupportPanel() {
  const [filter, setFilter] = useState('open');
  const load = useCallback(() => { let q = supabase.from('finnest_support_tickets').select('*').order('created_at', { ascending: false }); if (filter !== 'all') q = q.eq('status', filter); return q; }, [filter]);
  const [list, refresh] = useTable(load);
  const [open, setOpen] = useState(null), [msg, setMsg] = useState('');
  const save = async () => { const { error } = await supabase.from('finnest_support_tickets').update({ status: open.status, admin_note: open.admin_note || null }).eq('id', open.id); setMsg(error ? 'Could not update request.' : 'Request updated.'); if (!error) { setOpen(null); refresh(); } };
  return <><PanelHead title="Support requests" sub="Requests sent by signed-in users from the website." onRefresh={refresh}><select className="tool-input" value={filter} onChange={e => setFilter(e.target.value)}><option value="open">Open</option><option value="in_progress">In progress</option><option value="resolved">Resolved</option><option value="all">All</option></select></PanelHead>
    {msg && <p className="auth-message" role="status">{msg}</p>}
    {open && <div className="card dform"><div className="wide"><h3>{open.subject}</h3><p className="pre">{open.message}</p></div>
      <label>Status<select value={open.status} onChange={e => setOpen({ ...open, status: e.target.value })}><option value="open">Open</option><option value="in_progress">In progress</option><option value="resolved">Resolved</option></select></label>
      <label className="wide">Internal note<textarea rows={3} value={open.admin_note || ''} onChange={e => setOpen({ ...open, admin_note: e.target.value })} /></label>
      <div className="wide dform-actions"><button className="primary compact" onClick={save}>Save</button><button className="outline-button" onClick={() => setOpen(null)}>Close</button></div></div>}
    <div className="table-wrap"><table className="dtable"><thead><tr><th>Subject</th><th>Status</th><th>Received</th><th /></tr></thead><tbody>
      {list.rows === null ? <tr><td colSpan={4}>Loading…</td></tr> : list.rows.length === 0 ? <tr><td colSpan={4} className="empty-cell">No requests.</td></tr> : list.rows.map(t => <tr key={t.id}><td>{t.subject}</td><td><Status v={t.status} /></td><td>{fmt(t.created_at)}</td><td className="row-actions"><button className="text-link" onClick={() => setOpen(t)}>Open</button></td></tr>)}
    </tbody></table></div></>;
}
