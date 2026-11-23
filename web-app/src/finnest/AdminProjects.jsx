import React, { useCallback, useEffect, useState } from 'react';
import { Plus, Trash2, RefreshCw, ExternalLink } from 'lucide-react';
import { supabase } from './supabase';

const EMPTY = {
  name: '', developer: '', city: '', locality: '', configuration: '',
  price_from_label: '', possession_label: '', approval_note: '', summary: '',
  status: 'draft', sort_order: 100,
};

const money = v => v?.trim() || '';

/**
 * Admin-managed project catalogue.
 *
 * Published rows appear on the public website's Projects screen and are what a
 * buyer can shortlist from. Draft rows are visible only to content editors.
 */
export default function AdminProjects() {
  const [rows, setRows] = useState(null);
  const [form, setForm] = useState(EMPTY);
  const [editing, setEditing] = useState(null);
  const [msg, setMsg] = useState('');
  const [busy, setBusy] = useState(false);

  const load = useCallback(async () => {
    const { data, error } = await supabase
      .from('finnest_projects')
      .select('*')
      .order('sort_order', { ascending: true })
      .order('name', { ascending: true });
    if (error) { setRows([]); setMsg('Could not load projects.'); return; }
    setRows(data || []);
  }, []);
  useEffect(() => { load(); }, [load]);

  const set = (k, v) => setForm(f => ({ ...f, [k]: v }));

  const save = async e => {
    e.preventDefault();
    if (!form.name.trim()) return setMsg('A project name is required.');
    setBusy(true); setMsg('');
    const payload = {
      name: form.name.trim(),
      developer: form.developer.trim() || null,
      city: form.city.trim() || null,
      locality: form.locality.trim() || null,
      configuration: form.configuration.trim() || null,
      price_from_label: money(form.price_from_label) || null,
      possession_label: form.possession_label.trim() || null,
      approval_note: form.approval_note.trim() || null,
      summary: form.summary.trim() || null,
      status: form.status,
      sort_order: Number(form.sort_order) || 100,
    };
    const { error } = editing
      ? await supabase.from('finnest_projects').update(payload).eq('id', editing)
      : await supabase.from('finnest_projects').insert(payload);
    setBusy(false);
    if (error) return setMsg('Could not save. Check your role and try again.');
    setMsg(editing ? 'Project updated.' : 'Project created.');
    setForm(EMPTY); setEditing(null); load();
  };

  const edit = row => {
    setEditing(row.id);
    setForm({ ...EMPTY, ...Object.fromEntries(Object.keys(EMPTY).map(k => [k, row[k] ?? (k === 'sort_order' ? 100 : '')])) });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const remove = async row => {
    if (!window.confirm(`Delete “${row.name}”? Buyers who shortlisted it keep their own copy.`)) return;
    const { error } = await supabase.from('finnest_projects').delete().eq('id', row.id);
    setMsg(error ? 'Could not delete.' : 'Project deleted.');
    load();
  };

  const toggle = async row => {
    const next = row.status === 'published' ? 'draft' : 'published';
    const { error } = await supabase.from('finnest_projects').update({ status: next }).eq('id', row.id);
    setMsg(error ? 'Could not change visibility.' : next === 'published' ? 'Published — live on the website.' : 'Moved back to draft.');
    load();
  };

  if (rows === null) return <p>Loading projects…</p>;

  return <div className="admin-stack">
    <section className="card form-card">
      <h3>{editing ? 'Edit project' : 'Add a project'}</h3>
      <p>Published projects appear on the website so buyers can shortlist them. Drafts stay internal.</p>
      <form onSubmit={save}>
        <div className="field-grid">
          <label>Project name<input required maxLength={120} value={form.name} onChange={e => set('name', e.target.value)} /></label>
          <label>Developer<input maxLength={120} value={form.developer} onChange={e => set('developer', e.target.value)} /></label>
          <label>City<input maxLength={80} value={form.city} onChange={e => set('city', e.target.value)} /></label>
          <label>Locality<input maxLength={120} value={form.locality} onChange={e => set('locality', e.target.value)} /></label>
          <label>Configuration<input maxLength={120} placeholder="2 & 3 BHK" value={form.configuration} onChange={e => set('configuration', e.target.value)} /></label>
          <label>Price from<input maxLength={60} placeholder="₹1.2 Cr" value={form.price_from_label} onChange={e => set('price_from_label', e.target.value)} /></label>
          <label>Possession<input maxLength={60} placeholder="Dec 2027" value={form.possession_label} onChange={e => set('possession_label', e.target.value)} /></label>
          <label>Approval note<input maxLength={200} placeholder="MahaRERA registered" value={form.approval_note} onChange={e => set('approval_note', e.target.value)} /></label>
          <label>Visibility<select value={form.status} onChange={e => set('status', e.target.value)}>
            <option value="draft">Draft — internal only</option>
            <option value="published">Published — live on website</option>
            <option value="archived">Archived — hidden</option>
          </select></label>
          <label>Order<input type="number" min={0} max={9999} value={form.sort_order} onChange={e => set('sort_order', e.target.value)} /></label>
        </div>
        <label>Summary<textarea rows={3} maxLength={2000} value={form.summary} onChange={e => set('summary', e.target.value)} /></label>
        {msg && <p className={msg.startsWith('Could not') ? 'auth-error' : 'auth-message'} role="status">{msg}</p>}
        <div className="admin-actions">
          <button className="primary compact" disabled={busy} type="submit"><Plus size={16} />{busy ? 'Saving…' : editing ? 'Save changes' : 'Add project'}</button>
          {editing && <button type="button" className="outline-button" onClick={() => { setEditing(null); setForm(EMPTY); }}>Cancel</button>}
        </div>
      </form>
    </section>

    <section className="card form-card">
      <div className="section-heading"><h3>Projects</h3><button className="text-link" onClick={load}><RefreshCw size={14} />Refresh</button></div>
      {rows.length === 0 && <p className="footnote">No projects yet. Add one above and publish it to show it on the website.</p>}
      <ul className="admin-table">
        {rows.map(row => <li key={row.id}>
          <div>
            <strong>{row.name}</strong>
            <span className="footnote">{row.developer || 'Developer not set'}{row.city ? ` · ${row.city}` : ''}{row.price_from_label ? ` · ${row.price_from_label}` : ''}</span>
          </div>
          <span className={'pill pill-' + row.status}>{row.status}</span>
          <div className="admin-row-actions">
            <button className="text-link" onClick={() => edit(row)}>Edit</button>
            <button className="text-link" onClick={() => toggle(row)}>{row.status === 'published' ? 'Unpublish' : 'Publish'}</button>
            <button className="text-link danger" onClick={() => remove(row)}><Trash2 size={14} />Delete</button>
          </div>
        </li>)}
      </ul>
    </section>
  </div>;
}
