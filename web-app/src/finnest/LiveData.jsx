import React, { useEffect, useState } from 'react';
import { UserRound, Send, MapPin, Building2 } from 'lucide-react';
import { supabase } from './supabase';

// Published experts managed in the admin console.
export function PublishedExperts() {
  const [rows, setRows] = useState(null);
  useEffect(() => { supabase.from('finnest_experts').select('id,name,specialty,city,phone,email,bio').eq('status', 'published').order('name').then(({ data }) => setRows(data || [])); }, []);
  if (!rows?.length) return null;
  return <section className="admin-space"><h2>Listed experts</h2><p className="footnote">Listed by FINNEST. Check credentials yourself before engaging anyone.</p>
    <div className="expert-grid">{rows.map(x => <article className="card expert-card" key={x.id}><div className="project-icon"><UserRound /></div><h3>{x.name}</h3><p><strong>{x.specialty}</strong>{x.city && <> · <MapPin size={12} /> {x.city}</>}</p>{x.bio && <p>{x.bio}</p>}{(x.phone || x.email) && <p>{x.phone}{x.phone && x.email && ' · '}{x.email && <a href={`mailto:${x.email}`}>{x.email}</a>}</p>}</article>)}</div></section>;
}

export function SupportForm({ session }) {
  const [subject, setSubject] = useState(''), [message, setMessage] = useState(''), [state, setState] = useState(''), [mine, setMine] = useState([]);
  const userId = session?.user?.id;
  const load = () => userId && supabase.from('finnest_support_tickets').select('id,subject,status,created_at').order('created_at', { ascending: false }).limit(10).then(({ data }) => setMine(data || []));
  useEffect(() => { load(); }, [userId]);
  if (!userId) return null;
  const submit = async e => {
    e.preventDefault(); if (!subject.trim() || !message.trim()) return; setState('sending');
    const { error } = await supabase.from('finnest_support_tickets').insert({ user_id: userId, subject: subject.trim().slice(0, 150), message: message.trim().slice(0, 5000) });
    if (error) return setState('error'); setSubject(''); setMessage(''); setState('sent'); load();
  };
  return <section className="card form-card admin-space"><h3>Contact support</h3><p>Send a question to the FINNEST team. Do not include passwords or bank details.</p>
    <form className="auth-form" onSubmit={submit}><label>Subject<input required maxLength={150} value={subject} onChange={e => setSubject(e.target.value)} /></label><label>Message<textarea required maxLength={5000} rows={4} value={message} onChange={e => setMessage(e.target.value)} /></label>
      <button className="primary compact" disabled={state === 'sending'}><Send size={16} /> {state === 'sending' ? 'Sending…' : 'Send request'}</button></form>
    {state === 'sent' && <p className="auth-message" role="status">Request sent. We'll update its status here.</p>}
    {state === 'error' && <p className="auth-error" role="alert">Request could not be sent. Please try again.</p>}
    {mine.length > 0 && <ul className="ticket-list">{mine.map(t => <li key={t.id}><span>{t.subject}</span><span className={'pill pill-' + t.status}>{t.status.replace('_', ' ')}</span></li>)}</ul>}
  </section>;
}

/** Admin-managed project catalogue, published rows only. */
export function PublishedProjects({ onShortlist }) {
  const [rows, setRows] = useState(null);
  const [error, setError] = useState(false);
  useEffect(() => {
    let active = true;
    const load = () => supabase
      .from('finnest_projects')
      .select('id,name,developer,city,locality,configuration,price_from_label,possession_label,approval_note,summary')
      .eq('status', 'published')
      .order('sort_order')
      .order('name')
      .then(({ data, error: err }) => {
        if (!active) return;
        if (err) { setError(true); setRows([]); return; }
        setRows(data || []);
      });
    load();
    // Realtime keeps the catalogue current without a refresh when an admin publishes.
    const channel = supabase.channel('finnest-projects')
      .on('postgres_changes', { event: '*', schema: 'public', table: 'finnest_projects' }, load)
      .subscribe();
    return () => { active = false; try { supabase.removeChannel(channel); } catch { /* closed */ } };
  }, []);
  if (error) return <p className="footnote">The project catalogue could not be loaded. Try refreshing.</p>;
  if (rows === null) return <p role="status">Loading listed projects…</p>;
  if (!rows.length) return null;
  return <section className="admin-space">
    <h2>Listed projects</h2>
    <p className="footnote">Added by the FINNEST team. Always verify RERA registration and approvals yourself before paying anything.</p>
    <div className="project-list">
      {rows.map(p => <article className="card project-row" key={p.id}>
        <div className="project-icon"><Building2 size={22} /></div>
        <div>
          <h3>{p.name}</h3>
          <p>{[p.developer, p.locality, p.city].filter(Boolean).join(' · ')}</p>
          <p>{[p.configuration, p.price_from_label, p.possession_label].filter(Boolean).join(' · ')}</p>
          {p.approval_note && <p className="footnote">{p.approval_note}</p>}
          {p.summary && <p>{p.summary}</p>}
        </div>
        {onShortlist && <button className="outline-button" onClick={() => onShortlist(p.name)}>Add to shortlist</button>}
      </article>)}
    </div>
  </section>;
}

/** Ask FINNEST to call back. Requires an account so the reply has an owner. */
export function EnquiryForm({ session, projectName }) {
  const [topic, setTopic] = useState(projectName ? 'project' : 'general');
  const [subject, setSubject] = useState(projectName ? `Enquiry about ${projectName}` : '');
  const [message, setMessage] = useState('');
  const [name, setName] = useState('');
  const [phone, setPhone] = useState('');
  const [consent, setConsent] = useState(false);
  const [state, setState] = useState('');
  const [mine, setMine] = useState([]);
  const userId = session?.user?.id;

  const load = () => userId && supabase.from('finnest_enquiries')
    .select('id,subject,status,created_at')
    .order('created_at', { ascending: false })
    .limit(10)
    .then(({ data }) => setMine(data || []));
  useEffect(() => { load(); /* eslint-disable-next-line react-hooks/exhaustive-deps */ }, [userId]);

  if (!userId) return null;

  const submit = async e => {
    e.preventDefault();
    if (!subject.trim() || !message.trim()) return;
    if (consent && !phone.trim() && !session?.user?.email) return;
    setState('sending');
    const { error } = await supabase.from('finnest_enquiries').insert({
      user_id: userId,
      topic,
      subject: subject.trim().slice(0, 150),
      message: message.trim().slice(0, 5000),
      contact_name: name.trim().slice(0, 120) || null,
      contact_phone: consent ? (phone.trim().slice(0, 20) || null) : null,
      contact_email: consent ? (session?.user?.email || null) : null,
      consent_contact: consent,
    });
    if (error) return setState('error');
    setSubject(projectName ? `Enquiry about ${projectName}` : '');
    setMessage(''); setPhone(''); setName(''); setConsent(false);
    setState('sent'); load();
  };

  return <section className="card form-card admin-space">
    <h3>Ask FINNEST to contact you</h3>
    <p>Send an enquiry and our team will follow up. Do not include bank details, passwords or identity documents.</p>
    <form className="auth-form" onSubmit={submit}>
      <label>Topic<select value={topic} onChange={e => setTopic(e.target.value)}>
        <option value="general">General</option>
        <option value="project">A specific project</option>
        <option value="expert">An expert</option>
        <option value="loan">Home loan</option>
        <option value="legal">Legal review</option>
      </select></label>
      <label>Subject<input required maxLength={150} value={subject} onChange={e => setSubject(e.target.value)} /></label>
      <label>Message<textarea required maxLength={5000} rows={4} value={message} onChange={e => setMessage(e.target.value)} /></label>
      <label>Your name (optional)<input maxLength={120} value={name} onChange={e => setName(e.target.value)} /></label>
      <label className="check-line"><input type="checkbox" checked={consent} onChange={e => setConsent(e.target.checked)} /> I agree to be contacted about this enquiry</label>
      {consent && <label>Phone number (optional)<input maxLength={20} inputMode="tel" value={phone} onChange={e => setPhone(e.target.value)} /></label>}
      <button className="primary compact" disabled={state === 'sending'}><Send size={16} />{state === 'sending' ? 'Sending…' : 'Send enquiry'}</button>
    </form>
    {state === 'sent' && <p className="auth-message" role="status">Enquiry received. We will update its status here.</p>}
    {state === 'error' && <p className="auth-error" role="alert">Your enquiry could not be sent. Please try again.</p>}
    {mine.length > 0 && <ul className="ticket-list">{mine.map(t => <li key={t.id}><span>{t.subject}</span><span className={'pill pill-' + t.status}>{t.status.replace('_', ' ')}</span></li>)}</ul>}
  </section>;
}
