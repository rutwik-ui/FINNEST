import React, { useCallback, useEffect, useRef, useState } from 'react';
import { ArrowRight, LockKeyhole, ShieldAlert } from 'lucide-react';
import { supabase } from './supabase';
import { CATEGORIES, CONTENT_ROLES, validateGuidance } from './admin';
import AdminMfa from './AdminMfa';
import { loadAdminGuidance } from './adminGuidance';

const empty = () => ({title:'', body:'', category:'general', status:'draft'});
const formFrom = item => item ? ({title:item.title, body:item.body, category:item.category, status:item.status}) : empty();
const sameForm = (a,b) => ['title','body','category','status'].every(key => a[key] === b[key]);
export default function Admin({go,session,authLoading,navigationGuardRef}) {
  const [email,setEmail]=useState(''), [password,setPassword]=useState('');
  const [roleState,setRoleState]=useState(null), [checking,setChecking]=useState(false), [error,setError]=useState(''), [message,setMessage]=useState(''), [busy,setBusy]=useState(false);
  const [items,setItems]=useState([]), [audit,setAudit]=useState([]), [form,setForm]=useState(empty), [editing,setEditing]=useState(null);
  const [listState,setListState]=useState('idle'), [roleRetry,setRoleRetry]=useState(0);
  const [preview,setPreview]=useState(false);
  const [filters,setFilters]=useState({category:'all',status:'all'}), [cursor,setCursor]=useState(null), [moreState,setMoreState]=useState('idle');
  const moreBusy=useRef(false);
  const dirty = !sameForm(form, formFrom(editing));
  const discardChanges = () => !busy && (!dirty || window.confirm('Discard unsaved guidance changes?'));
  if(navigationGuardRef) navigationGuardRef.current=discardChanges;
  useEffect(()=>()=>{if(navigationGuardRef)navigationGuardRef.current=null},[navigationGuardRef]);
  const [aalState,setAalState]=useState(null), [checkingAal,setCheckingAal]=useState(false), [aalError,setAalError]=useState('');
  const userId=session?.user?.id;
  const token=session?.access_token;
  const currentUser=useRef(userId); currentUser.current=userId;
  const currentToken=useRef(token); currentToken.current=token;
  const role=roleState && roleState.userId===userId ? roleState.role : null;
  const aal=aalState && aalState.userId===userId && aalState.token===token ? aalState.level : null;
  useEffect(()=>{
    let live=true; setChecking(false); setRoleState(null); setAalState(null); setItems([]); setAudit([]); setEditing(null); setForm(empty()); setError(''); setMessage(''); setPreview(false); setListState('idle'); setCursor(null); setMoreState('idle');
    if(!supabase || !userId)return;
    setChecking(true);
    (async()=>{const {data,error}=await supabase.from('finnest_admin_roles').select('role').eq('user_id',userId).maybeSingle();
      if(!live)return; setChecking(false);
      if(error) setError('Could not verify administrator access. Please try again.');
      else setRoleState({userId,role:data?.role || 'none'});
    })().catch(()=>{if(live){setChecking(false);setError('Could not verify administrator access. Please try again.')}});
    return()=>{live=false};
  },[userId,roleRetry]);
  const refreshRequest=useRef(0);
  const refresh=useCallback(async()=>{
    const request=++refreshRequest.current;
    setListState('loading'); setCursor(null); setMoreState('idle'); moreBusy.current=false;
    try {
      const [contentResult,auditResult]=await Promise.all([
        loadAdminGuidance(supabase,filters),
        supabase.from('finnest_admin_audit').select('id,actor_id,item_id,action,occurred_at').order('occurred_at',{ascending:false}).limit(20)
      ]);
      if(auditResult.error) throw new Error('Could not load admin content or audit records.');
      if (request !== refreshRequest.current || currentUser.current !== userId || currentToken.current !== token) return;
      setItems(contentResult.items); setCursor(contentResult.cursor); setAudit(auditResult.data || []); setListState('ready');
    } catch(e) {
      if (request === refreshRequest.current && currentUser.current === userId && currentToken.current === token) setListState('error');
    }
  },[userId,token,filters]);
  const loadMore=async()=>{
    if(!cursor || moreBusy.current || listState!=='ready')return;
    const request=refreshRequest.current; moreBusy.current=true; setMoreState('loading');
    try {
      const result=await loadAdminGuidance(supabase,filters,cursor);
      if(request!==refreshRequest.current || currentUser.current!==userId || currentToken.current!==token)return;
      setItems(previous=>{const ids=new Set(previous.map(item=>item.id)); return [...previous,...result.items.filter(item=>!ids.has(item.id) && (ids.add(item.id),true))]});
      setCursor(result.cursor);setMoreState('idle');
    } catch {if(request===refreshRequest.current && currentUser.current===userId && currentToken.current===token)setMoreState('error')}
    finally {if(request===refreshRequest.current)moreBusy.current=false}
  };
  const checkAal=useCallback(async()=>{
    if(!userId || !CONTENT_ROLES.includes(role)){setAalState(null);setCheckingAal(false);setAalError('');return}
    setCheckingAal(true);setAalError('');
    try{const {data,error}=await supabase.auth.mfa.getAuthenticatorAssuranceLevel();if(error)throw error;if(currentUser.current===userId && currentToken.current===token)setAalState({userId,token,level:data.currentLevel})}
    catch(e){if(currentUser.current===userId && currentToken.current===token){setAalState(null);setAalError('Could not verify session assurance. Please retry.')}}
    finally{if(currentUser.current===userId && currentToken.current===token)setCheckingAal(false)}
  },[userId,role,token]);
  useEffect(()=>{checkAal()},[checkAal]);
  useEffect(()=>{if(CONTENT_ROLES.includes(role) && aal==='aal2') refresh(); else {refreshRequest.current++;setItems([]);setAudit([]);setCursor(null);setMoreState('idle');moreBusy.current=false;setListState('idle')} return ()=>{refreshRequest.current++}},[role,aal,refresh]);
  useEffect(()=>{const handler=e=>{if(dirty){e.preventDefault();e.returnValue=''}};window.addEventListener('beforeunload',handler);return ()=>window.removeEventListener('beforeunload',handler)},[dirty]);
  async function login(e){e.preventDefault();if(!supabase || busy)return;setBusy(true);setError('');setMessage('');try{const {error}=await supabase.auth.signInWithPassword({email:email.trim(),password});if(error)throw error;setPassword('')}catch(e){setError(e?.message || 'Sign-in failed.')}finally{setBusy(false)}}
  async function logout(){if(!discardChanges())return;setBusy(true);setError('');try{const {error}=await supabase.auth.signOut();if(error)throw error}catch(e){setError(e?.message || 'Sign-out failed.')}finally{setBusy(false)}}
  async function save(e){
    e.preventDefault(); if(busy)return; const validation=validateGuidance(form); if(validation){setError(validation);return}
    if(form.status==='published' && !window.confirm('Published guidance is publicly readable. Continue?'))return;
    setBusy(true);setError('');setMessage('');
    try{
      const payload={title:form.title.trim(),body:form.body.trim(),category:form.category,status:form.status};
      const query=editing ? supabase.from('finnest_guidance').update(payload).eq('id',editing.id).eq('updated_at',editing.updated_at).select('id') : supabase.from('finnest_guidance').insert(payload).select('id');
      const {data,error}=await query;if(error)throw new Error('Save could not be confirmed. Refresh and check the list before retrying to avoid a duplicate.');
      if(currentUser.current !== userId || currentToken.current !== token)return;
      if(!data?.length)throw new Error('This item changed or access was removed. Reload the list before editing again.');
      setForm(empty());setEditing(null);setMessage('Guidance saved. Published items are public and appear in the website and Android guidance libraries.');
      await refresh();
    }catch(e){if(currentUser.current === userId && currentToken.current === token)setError(e?.message || 'Could not confirm the save. Refresh and check the list before retrying, to avoid a duplicate.')}finally{setBusy(false)}
  }
  const patch=(key,value)=>setForm(previous=>({...previous,[key]:value}));
  return <main className="admin-main"><div className="admin-lock admin-panel"><div className="lock-icon"><LockKeyhole size={30}/></div><span className="eyebrow">FINNEST ADMIN</span><h1>Content management</h1><p>This early admin phase manages guidance only. It does not manage users, buyer data, Android forms, experts, or projects.</p>
  <div className="admin-notice"><ShieldAlert size={21}/><span>Published guidance is public. Do not enter customer data or secrets. Authenticator verification is required for content access. Full security testing is not yet complete; do not use this panel for production operations.</span></div>
  {!supabase?<p>Sign-in is not configured. See README setup.</p>:authLoading||checking||(session && role===null && !error)?<p role="status">Checking administrator access…</p>:!session?<form className="auth-form" onSubmit={login}><label>Admin email<input type="email" autoComplete="username" required value={email} onChange={e=>setEmail(e.target.value)}/></label><label>Password<input type="password" autoComplete="current-password" required value={password} onChange={e=>setPassword(e.target.value)}/></label><button className="primary compact" disabled={busy}>Sign in <ArrowRight size={17}/></button></form>:role===null?<><p role="alert">Could not verify administrator access.</p><button className="outline-button" onClick={()=>{setError('');setRoleRetry(n=>n+1)}}>Retry access check</button><button className="text-link admin-space" disabled={busy} onClick={logout}>Sign out</button></>:!CONTENT_ROLES.includes(role)?<><p>{role==='none'?'This account has no administrator role.':'This role cannot manage guidance content.'} Roles must be assigned by a trusted project administrator.</p><button className="outline-button admin-space" disabled={busy} onClick={logout}>Sign out</button></>:checkingAal?<p role="status">Checking session security…</p>:aalError?<><p className="auth-error" role="alert">{aalError}</p><button className="outline-button" onClick={checkAal}>Retry</button><button className="text-link admin-space" onClick={logout}>Sign out</button></>:aal!=='aal2'?<><AdminMfa onVerified={checkAal}/><button className="text-link admin-space" disabled={busy} onClick={logout}>Sign out</button></>:<>
   <div className="admin-identity"><span>Signed in as {session.user.email} · {role.replaceAll('_',' ')}</span><button className="text-link" disabled={busy} onClick={logout}>Sign out</button></div>
   <h2 className="admin-heading">{editing?'Edit guidance':'New guidance'}</h2><form className="auth-form admin-form" onSubmit={save}>
    <label>Title<input disabled={busy} required maxLength={150} value={form.title} onChange={e=>patch('title',e.target.value)}/></label>
    <label>Category<select disabled={busy} value={form.category} onChange={e=>patch('category',e.target.value)}>{CATEGORIES.map(x=><option key={x} value={x}>{x.replaceAll('_',' ')}</option>)}</select></label>
    <label>Guidance<textarea disabled={busy} required maxLength={10000} rows={7} value={form.body} onChange={e=>patch('body',e.target.value)}/></label>
    <label>Status<select disabled={busy} value={form.status} onChange={e=>patch('status',e.target.value)}><option value="draft">Draft (admins only)</option><option value="published">Published (public)</option></select></label>
    <button type="button" className="outline-button" aria-expanded={preview} onClick={()=>setPreview(v=>!v)}>{preview?'Hide preview':'Preview guidance'}</button>
    {preview&&<section className="card guidance-item admin-preview" aria-label="Guidance preview"><span className="eyebrow">{form.category.replaceAll('_',' ')}</span><h3>{form.title.trim()||'Untitled guidance'}</h3><p className="guidance-body">{form.body.trim()||'No guidance text entered.'}</p><p className="footnote">Preview only · {form.status==='published'?'would be public after saving':'draft is not public'}. Nothing is published by previewing.</p></section>}
    <div className="admin-actions"><button className="primary compact" disabled={busy}>{busy?'Saving…':'Save guidance'}</button>{editing&&<button type="button" className="outline-button" disabled={busy} onClick={()=>{if(!discardChanges())return;setEditing(null);setForm(empty());setError('')}}>Cancel edit</button>}</div>
   </form>
   <div className="admin-list-heading"><h2>Guidance</h2><button className="text-link" disabled={busy || listState==='loading'} onClick={refresh}>Refresh</button></div><p>Most recently updated first. Refresh to include changes made while browsing.</p>
   <div className="admin-filters"><label>Filter category<select disabled={busy} value={filters.category} onChange={e=>{refreshRequest.current++;setItems([]);setCursor(null);setFilters(f=>({...f,category:e.target.value}))}}><option value="all">All categories</option>{CATEGORIES.map(x=><option key={x} value={x}>{x.replaceAll('_',' ')}</option>)}</select></label><label>Filter status<select disabled={busy} value={filters.status} onChange={e=>{refreshRequest.current++;setItems([]);setCursor(null);setFilters(f=>({...f,status:e.target.value}))}}><option value="all">All statuses</option><option value="draft">Draft</option><option value="published">Published</option></select></label></div>
   {listState==='loading'&&<p role="status">Loading guidance and activity…</p>}{listState==='error'&&<p className="auth-error" role="alert">Could not load guidance and activity. Select Refresh to retry; existing results may be out of date.</p>}
   {listState==='ready'&&items.length===0?<p className="admin-empty">No guidance matches these filters.</p>:items.map(item=><article className="admin-item" key={item.id}><div><strong>{item.title}</strong><p>{item.category.replaceAll('_',' ')} · {item.status} · {new Date(item.updated_at).toLocaleString()}</p></div><button className="outline-button" disabled={busy} onClick={()=>{if(!discardChanges())return;setEditing(item);setForm(formFrom(item));setError('');setMessage('')}}>Edit</button></article>)}
   {moreState==='error'&&<p className="auth-error" role="alert">Could not load more guidance. Your current results are still shown.</p>}
   {listState==='ready'&&cursor&&<button type="button" className="outline-button admin-space" disabled={busy || moreState==='loading'} onClick={loadMore}>{moreState==='loading'?'Loading…':moreState==='error'?'Retry loading more':'Load more guidance'}</button>}
   <h2 className="admin-heading">Recent content activity</h2><p>Most recent 20 create/update events. This is not a full security audit.</p>{listState==='ready'&&audit.length===0?<p className="admin-empty">No content activity yet.</p>:audit.map(entry=><div className="audit-row" key={entry.id}><span>{entry.action==='INSERT'?'Created':'Updated'} · {new Date(entry.occurred_at).toLocaleString()}</span><small>Item {entry.item_id} · actor {entry.actor_id || 'removed'}</small></div>)}
  </>}
  {error&&<p className="auth-error" role="alert">{error}</p>}{message&&<p className="auth-message" role="status">{message}</p>}
  <button className="text-link admin-home" onClick={()=>{if(navigationGuardRef || discardChanges())go('/')}}>Return to FINNEST <ArrowRight size={16}/></button></div></main>
}
