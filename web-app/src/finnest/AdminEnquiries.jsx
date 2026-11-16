import React, { useCallback, useEffect, useState } from 'react';
import { RefreshCw, Mail, Phone, Inbox } from 'lucide-react';
import { supabase } from './supabase';

const STATUSES = ['new', 'in_progress', 'contacted', 'closed', 'spam'];
const LABELS = { new: 'New', in_progress: 'In progress', contacted: 'Contacted', closed: 'Closed', spam: 'Spam' };

/**
 * Buyer enquiries: someone asking to be contacted, as opposed to a support
 * report. Only super_admin and support_manager can read these; the database
 * enforces that, so a content manager sees an empty list rather than a leak.
 */
export default function AdminEnquiries() {
  const [rows, setRows] = useState(null);
  const [filter, setFilter] = useState('all');
  const [open, setOpen] = useState(null);
  const [msg, setMsg] = useState('');

  const load = useCallback(async () => {
    let q = supabase.from('finnest_enquiries').select('*').order('created_at', { ascending: false }).limit(200);
    if (filter !== 'all') q = q.eq('status', filter);
    const { data, error } = await q;
    if (error) { setRows([]); setMsg('Could not load enquiries.'); return; }
    setRows(data || []);
  }, [filter]);
  useEffect(() => { load(); }, [load]);

  const save = async () => {
    const { error } = await supabase
      .from('finnest_enquiries')
      .update({ status: open.status, admin_note: open.admin_note || null })
      .eq('id', open.id);
    setMsg(error ? 'Could not update this enquiry.' : 'Enquiry updated.');
    if (!error) { setOpen(null); load(); }
  };

  if (rows === null) return <p>Loading enquiries…</p>;

  return <div className="admin-stack">
    <section className="card form-card">
      <div className="section-heading">
        <h3>Buyer enquiries</h3>
        <div className="admin-row-actions">
          <select value={filter} onChange={e => setFilter(e.target.value)} aria-label="Filter by status">
            <option value="all">All</option>
            {STATUSES.map(s => <option key={s} value={s}>{LABELS[s]}</option>)}
          </select>
          <button className="text-link" onClick={load}><RefreshCw size={14} />Refresh</button>
        </div>
      </div>
      <p>People who asked FINNEST to contact them. Contact details are shown only when the buyer ticked consent.</p>
      {msg && <p className="footnote" role="status">{msg}</p>}
      {rows.length === 0 ? <div className="empty"><Inbox size={40} /><h2>No enquiries yet</h2><p>When a buyer asks for a callback it appears here.</p></div>
        : <ul className="admin-table">
          {rows.map(row => <li key={row.id}>
            <div>
              <strong>{row.subject}</strong>
              <span className="footnote">{new Date(row.created_at).toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' })} · {row.topic}{row.consent_contact ? ' · consent given' : ' · no contact consent'}</span>
            </div>
            <span className={'pill pill-' + row.status}>{LABELS[row.status] || row.status}</span>
            <div className="admin-row-actions">
              {row.consent_contact && row.contact_phone && <a className="text-link" href={`tel:${row.contact_phone}`}><Phone size={14} />{row.contact_phone}</a>}
              {row.consent_contact && row.contact_email && <a className="text-link" href={`mailto:${row.contact_email}`}><Mail size={14} />Email</a>}
              <button className="text-link" onClick={() => setOpen(row)}>Open</button>
            </div>
          </li>)}
        </ul>}
    </section>

    {open && <div className="modal-backdrop" role="dialog" aria-modal="true" aria-label="Enquiry">
      <div className="modal">
        <div className="modal-head"><h2>{open.subject}</h2><button className="plain-icon" onClick={() => setOpen(null)} aria-label="Close">×</button></div>
        <p className="footnote">{open.topic} · {new Date(open.created_at).toLocaleString('en-IN')}</p>
        <p>{open.message}</p>
        {open.contact_name && <p className="footnote">Name: {open.contact_name}</p>}
        {open.consent_contact && open.contact_phone && <p className="footnote">Phone: {open.contact_phone}</p>}
        {open.consent_contact && open.contact_email && <p className="footnote">Email: {open.contact_email}</p>}
        {!open.consent_contact && <p className="footnote">The buyer did not consent to be contacted. Do not contact them outside the app.</p>}
        <label>Status<select value={open.status} onChange={e => setOpen({ ...open, status: e.target.value })}>
          {STATUSES.map(s => <option key={s} value={s}>{LABELS[s]}</option>)}
        </select></label>
        <label>Internal note<textarea rows={3} maxLength={2000} value={open.admin_note || ''} onChange={e => setOpen({ ...open, admin_note: e.target.value })} /></label>
        <button className="primary compact" onClick={save}>Save</button>
      </div>
    </div>}
  </div>;
}
