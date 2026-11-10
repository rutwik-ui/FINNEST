import React, { useEffect, useState } from 'react';
import { supabase } from './supabase';

/** TOTP is enrolled and verified with Supabase Auth; the secret is never persisted by this UI. */
export default function AdminMfa({onVerified}) {
  const [factors,setFactors]=useState([]), [factorId,setFactorId]=useState('');
  const [enrollment,setEnrollment]=useState(null), [code,setCode]=useState(''), [busy,setBusy]=useState(false), [error,setError]=useState(''), [loading,setLoading]=useState(true), [loadFailed,setLoadFailed]=useState(false);
  async function loadFactors() {
    const {data,error}=await supabase.auth.mfa.listFactors();
    if(error)throw error;
    return (data?.all || []).filter(f=>f.factor_type==='totp');
  }
  useEffect(()=>{
    let active=true;
    loadFactors().then(list=>{if(active){setFactors(list);setFactorId(list.find(f=>f.status==='verified')?.id || '');setLoading(false)}})
      .catch(()=>{if(active){setError('Could not load authenticator setup. Please retry.');setLoadFailed(true);setLoading(false)}});
    return()=>{active=false};
  },[]);
  async function retryLoad(){
    setLoading(true);setError('');
    try {const list=await loadFactors();setFactors(list);setFactorId(list.find(f=>f.status==='verified')?.id || '');setLoadFailed(false)}
    catch {setLoadFailed(true);setError('Could not load authenticator setup. Please retry.')}
    finally {setLoading(false)}
  }
  async function enroll(){
    setError('');setBusy(true);
    try {
      // Unverified enrollments cannot be resumed after leaving this page; explicitly remove those
      // before starting again. Other factors are never removed by this flow.
      for(const factor of factors.filter(f=>f.status==='unverified' && f.friendly_name==='FINNEST admin')){
        const result=await supabase.auth.mfa.unenroll({factorId:factor.id});if(result.error)throw result.error;
        setFactors(previous=>previous.filter(f=>f.id!==factor.id));
      }
      const {data,error}=await supabase.auth.mfa.enroll({factorType:'totp',friendlyName:'FINNEST admin'});
      if(error)throw error;
      setEnrollment(data);setFactorId(data.id);setCode('');
    } catch(e){setError(e?.message || 'Could not start authenticator setup.')} finally{setBusy(false)}
  }
  async function verify(e){
    e.preventDefault(); if(!/^\d{6}$/.test(code)){setError('Enter the six-digit code from your authenticator.');return}
    setError('');setBusy(true);
    try {
      const {error}=await supabase.auth.mfa.challengeAndVerify({factorId,code});if(error)throw error;
      setCode('');setEnrollment(null);onVerified();
    } catch(e){setError(e?.message || 'Could not verify the code.')} finally{setBusy(false)}
  }
  const verified=factors.filter(f=>f.status==='verified');
  return <section className="mfa-panel"><h2>Secure your administrator session</h2><p>An authenticator code is required before accessing guidance drafts or making changes.</p>
    {loading?<p role="status">Checking authenticator setup…</p>:loadFailed?<button type="button" className="outline-button" onClick={retryLoad}>Retry</button>:enrollment?<><p>Scan this QR code with an authenticator app, or enter the setup key manually. Keep the setup key private.</p><img className="mfa-qr" src={enrollment.totp.qr_code} alt="Authenticator setup QR code"/><label>Setup key<code className="mfa-secret">{enrollment.totp.secret}</code></label></>:verified.length?<label>Authenticator<select value={factorId} onChange={e=>setFactorId(e.target.value)}>{verified.map(f=><option key={f.id} value={f.id}>{f.friendly_name || 'Authenticator'}</option>)}</select></label>:<><p>Set up a time-based authenticator app to continue. If you leave during setup, start setup again.</p><button type="button" className="outline-button" disabled={busy} onClick={enroll}>Set up authenticator</button></>}
    {!loading && !loadFailed && (enrollment || verified.length>0) && <form className="auth-form mfa-form" onSubmit={verify}><label>Six-digit code<input inputMode="numeric" autoComplete="one-time-code" pattern="[0-9]{6}" maxLength={6} value={code} onChange={e=>setCode(e.target.value)} required /></label><button className="primary compact" disabled={busy} type="submit">{busy?'Verifying…':'Verify and continue'}</button></form>}
    {error&&<p className="auth-error" role="alert">{error}</p>}
  </section>
}
