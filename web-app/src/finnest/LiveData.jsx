import React, { useEffect, useState } from 'react';
import { UserRound, Send, MapPin } from 'lucide-react';
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
