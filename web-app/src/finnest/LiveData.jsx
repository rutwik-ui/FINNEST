import React, { useEffect, useState } from 'react';
import { UserRound, Send, MapPin, Upload, FileText } from 'lucide-react';
import { logActivity } from './cloudSync';
import { supabase } from './supabase';
import { readPublicCache, writePublicCache } from './offlineStore';
import { Button } from '@/components/ui/button';

// Published experts managed in the admin console.
export function PublishedExperts({role='all',search=''}) {
  const [rows, setRows] = useState(null), [error,setError]=useState(''),[cached,setCached]=useState(false),[attempt,setAttempt]=useState(0);
  useEffect(() => {
    let active=true;
    const load=async()=>{
      setError('');const saved=await readPublicCache('published-experts');if(!active)return;
      if(saved){setRows(saved);setCached(true);}
      const {data,error}=await supabase.from('finnest_experts').select('id,name,specialty,city,phone,email,bio,status').eq('status','published').order('name').limit(1000);
      if(!active)return;
      if(error){setError(saved?'Showing the last saved experts. Reconnect for updates.':'Could not load experts. Check your connection and retry.');return;}
      const published=(data||[]).filter(r=>r.status==='published');setRows(published);setCached(false);await writePublicCache('published-experts',published);
    };
    load();const channel=supabase.channel(`public-experts-${crypto.randomUUID()}`).on('postgres_changes',{event:'*',schema:'public',table:'finnest_experts'},load).subscribe();window.addEventListener('online',load);
    return()=>{active=false;supabase.removeChannel(channel);window.removeEventListener('online',load);};
  },[attempt]);
  const visible=(rows||[]).filter(x=>(role==='all'||x.specialty===role)&&(!search||[x.name,x.specialty,x.city].some(v=>v?.toLowerCase().includes(search.toLowerCase()))));
  if(rows===null&&!error)return <div className="content-skeletons" role="status" aria-label="Loading experts">{[0,1,2].map(i=><div className="content-skeleton" key={i}/>)}</div>;
  if(rows===null&&error)return <div className="directory-empty" role="alert"><p>{error}</p><Button variant="outline" onClick={()=>setAttempt(v=>v+1)}>Try again</Button></div>;
  if(!visible.length)return <div className="directory-empty"><h2>{rows?.length?'No matching experts':'No published experts yet'}</h2><p>{rows?.length?'Try a different search or category.':'Explore the professional roles below while the directory is being prepared.'}</p></div>;
  return <section className="admin-space"><h2>Listed experts</h2><p className="footnote">Listed by FINNEST. Check credentials yourself before engaging anyone.</p>
    {error&&<p role="status">{error}</p>}<div className="expert-grid">{visible.map(x => <article className="card expert-card" id={`expert-${x.id}`} key={x.id}><div className="project-icon"><UserRound /></div><h3>{x.name}</h3><p><strong>{x.specialty}</strong>{x.city && <> · <MapPin size={12} /> {x.city}</>}</p>{x.bio && <p>{x.bio}</p>}{!cached&&(x.phone || x.email) && <p>{x.phone&&<a href={`tel:${x.phone}`}>{x.phone}</a>}{x.phone && x.email && ' · '}{x.email && <a href={`mailto:${x.email}`}>{x.email}</a>}</p>}</article>)}</div></section>;
}

const SUPPORT_TYPES = ['Question', 'Problem', 'Feedback', 'Account'];
const STATUS_LABEL = { open: 'Open', in_progress: 'In progress', resolved: 'Resolved' };
// Help & feedback, shared with the Android app: requests reach the admin Support inbox and replies show here live.
export function SupportForm({ session, openAuth }) {
  const [kind, setKind] = useState('Question'), [subject, setSubject] = useState(''), [message, setMessage] = useState(''), [state, setState] = useState(''), [mine, setMine] = useState(null);
  const userId = session?.user?.id;
  const load = () => userId && supabase.from('finnest_support_tickets').select('id,subject,status,admin_note,created_at').eq('user_id', userId).order('created_at', { ascending: false }).limit(20).then(({ data, error }) => setMine(error ? [] : data || []));
  useEffect(() => {
    setMine(null); setState(''); if (!userId) return;
    load();
    const channel = supabase.channel(`support-${userId}-${crypto.randomUUID()}`).on('postgres_changes', { event: '*', schema: 'public', table: 'finnest_support_tickets', filter: `user_id=eq.${userId}` }, load).subscribe();
    return () => { supabase.removeChannel(channel); };
  }, [userId]);
  if (!userId) return <section className="card form-card"><h2>Sign in to contact us</h2><p>Messages are linked to your account so we can reply to you securely.</p>
    {openAuth && <div className="step-actions"><button className="primary compact" onClick={() => openAuth('signin')}>Log in</button><button className="outline-button" onClick={() => openAuth('signup')}>Create account</button></div>}</section>;
  const submit = async e => {
    e.preventDefault(); if (!subject.trim() || message.trim().length < 5) return; if (!navigator.onLine) return setState('offline'); setState('sending');
    const { error } = await supabase.from('finnest_support_tickets').insert({ user_id: userId, subject: `[${kind}] ${subject.trim()}`.slice(0, 150), message: message.trim().slice(0, 5000) });
    if (error) return setState('error'); logActivity('support_requested', { category: kind }); setSubject(''); setMessage(''); setState('sent'); load();
  };
  const date = iso => { try { return new Date(iso).toLocaleDateString(undefined, { day: 'numeric', month: 'short', year: 'numeric' }); } catch { return ''; } };
  return <>
    <section className="card form-card"><h2>Send a message</h2><p>Ask a question, report a problem or tell us what would make FINNEST better. Please don't include passwords or bank details.</p>
      <form className="auth-form" onSubmit={submit}>
        <label>Type<select value={kind} onChange={e => setKind(e.target.value)}>{SUPPORT_TYPES.map(t => <option key={t}>{t}</option>)}</select></label>
        <label>Subject<input required maxLength={120} value={subject} onChange={e => setSubject(e.target.value)} /></label>
        <label>Message<textarea required minLength={5} maxLength={5000} rows={5} value={message} onChange={e => setMessage(e.target.value)} /></label>
        <button className="primary compact" disabled={state === 'sending'}><Send size={16} /> {state === 'sending' ? 'Sending…' : 'Send message'}</button></form>
      {state === 'sent' && <p className="auth-message" role="status">Sent. Our reply will appear below.</p>}
      {state === 'error' && <p className="auth-error" role="alert">Your message couldn't be sent. Please try again.</p>}
      {state === 'offline' && <p className="auth-error" role="status">Connect to the internet to send this. Your message is still here.</p>}
    </section>
    <section className="card form-card admin-space"><h2>Your requests</h2>
      {mine === null ? <p className="footnote" role="status">Loading…</p> : mine.length === 0 ? <p className="footnote">No requests yet.</p> :
        <ul className="ticket-list support-list">{mine.map(t => <li key={t.id}><div><strong>{t.subject}</strong><small>{date(t.created_at)}</small>{t.admin_note && <p className="support-reply"><span>Reply from FINNEST</span>{t.admin_note}</p>}</div><span className={'pill pill-' + t.status}>{STATUS_LABEL[t.status] ?? t.status}</span></li>)}</ul>}
    </section>
  </>;
}

// Private documents: stored under the user's own folder; admins with support roles can view them.
export function MyDocuments({ session, journey }) {
  const userId = session?.user?.id;
  const [docs, setDocs] = useState([]), [state, setState] = useState('');
  const load = () => userId && supabase.from('finnest_documents').select('*').eq('user_id', userId).order('uploaded_at', { ascending: false }).then(({ data }) => setDocs(data || []));
  useEffect(() => { setDocs([]);setState(''); load(); }, [userId]);
  if (!userId) return null;
  const upload = async e => {
    const file = e.target.files?.[0]; e.target.value = ''; if (!file) return;
    if(!navigator.onLine)return setState('Connect to the internet to upload documents.');
    if (file.size > 10 * 1024 * 1024) return setState('Files must be 10 MB or smaller.');
    if (!['application/pdf','image/jpeg','image/png'].includes(file.type) && !/\.(pdf|jpe?g|png)$/i.test(file.name)) return setState('Upload a PDF, JPG or PNG file.');
    setState('Uploading…');
    const path = `${userId}/${crypto.randomUUID()}-${file.name.replace(/[^\w.\-]+/g, '_').slice(-80)}`;
    const up = await supabase.storage.from('finnest-private-documents').upload(path, file);
    if (up.error) return setState('Upload failed. Please try again.');
    const { error } = await supabase.from('finnest_documents').insert({ user_id: userId, path, name: file.name.slice(0, 150), journey_type: journey === 'Resale Property' ? 'resale' : 'new' });
    if (error) return setState('Upload failed. Please try again.');
    logActivity('document_uploaded', { name: file.name.slice(0, 150) }); setState('Uploaded.'); load();
  };
  const open = async d => { const { data } = await supabase.storage.from('finnest-private-documents').createSignedUrl(d.path, 120); if (data?.signedUrl) window.open(data.signedUrl, '_blank', 'noopener'); };
  return <section className="card form-card"><h3>My documents</h3><p>Keep copies of your documents with your journey (PDF, JPG or PNG, up to 10 MB). FINNEST support staff can view them. FINNEST does not verify documents.</p>
    <label className="outline-button upload-btn"><Upload size={16} /> Upload a file<input type="file" hidden accept=".pdf,.jpg,.jpeg,.png,application/pdf,image/jpeg,image/png" onChange={upload} /></label>
    {state && <p className="footnote" role="status">{state}</p>}
    {docs.length > 0 && <ul className="ticket-list">{docs.map(d => <li key={d.id}><button className="text-link" onClick={() => open(d)}><FileText size={14} /> {d.name}</button></li>)}</ul>}
  </section>;
}
