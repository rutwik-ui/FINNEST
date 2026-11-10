import React from 'react';
import {ArrowLeft, ArrowRight, Info} from 'lucide-react';
import {emptyFeedback, LEGAL_CHECKS, LENDER_CHECKS, monthlyEmi, STATUSES} from './newJourney';

const statusChoices = value => value && !STATUSES.includes(value) ? [value,...STATUSES] : STATUSES;
const patchObject = (data,update,key,field,value) => update({[key]:{...data[key],[field]:value}});
function Navigation({back,next,go,nextText='Continue'}) {return <div className="step-actions"><button className="outline-button" onClick={()=>go(back)}><ArrowLeft size={17}/> Back</button><button className="outline-button" onClick={()=>go('/')}>Return home</button>{next&&<button className="primary compact" onClick={()=>go(next)}>{nextText} <ArrowRight size={18}/></button>}</div>}
function Notice({children}) {return <div className="notice"><Info size={20}/><span>{children}</span></div>}
export function Feedback({data,update,go}) {
 const form=data.feedbackForm, notes=data.feedbackNotes;
 const set=(key,value)=>patchObject(data,update,'feedbackForm',key,value);
 function add(e){e.preventDefault();if(!form.topic.trim()||!form.shared.trim())return;
  const id=globalThis.crypto?.randomUUID?.() ?? `${Date.now()}-${Math.random()}`;
  update({feedbackNotes:[...notes,{...form,id,topic:form.topic.trim(),shared:form.shared.trim()}],feedbackForm:emptyFeedback()});
 }
 return <><div className="page-head"><span className="eyebrow">STEP 4B · NEW PROPERTY</span><h1>Resident feedback notes<span className="red-dot">.</span></h1><p>Keep conversations and sources in perspective. An experience is an individual account, not a guarantee.</p></div>
 <section className="card form-card"><form className="auth-form" onSubmit={add}>
  <label>Topic<input required value={form.topic} onChange={e=>set('topic',e.target.value)} placeholder="Enter a topic" maxLength={150}/></label>
  <div className="field-grid"><label>Date (optional)<input type="date" value={form.date} onChange={e=>set('date',e.target.value)}/></label><label>Context (optional)<input value={form.context} onChange={e=>set('context',e.target.value)} placeholder="Who / setting" maxLength={500}/></label></div>
  <label>What was shared?<textarea required rows={4} value={form.shared} onChange={e=>set('shared',e.target.value)} placeholder="Enter only the note you want to keep" maxLength={5000}/></label>
  <label>Concern or question<textarea rows={3} value={form.concern} onChange={e=>set('concern',e.target.value)} placeholder="Optional" maxLength={5000}/></label>
  <label>Follow-up<textarea rows={3} value={form.followup} onChange={e=>set('followup',e.target.value)} placeholder="Optional next question" maxLength={5000}/></label>
  <button className="primary compact" type="submit" disabled={!form.topic.trim()||!form.shared.trim()}>Add feedback note</button>
 </form></section>
 <section className="card form-card"><div className="section-heading"><h2>Notes you entered</h2><span>{notes.length}</span></div>{notes.length===0?<p>No resident feedback notes entered yet.</p>:notes.map(note=><article className="feedback-note" key={note.id}><h3>{note.topic}</h3>{(note.date||note.context)&&<p>{[note.date,note.context].filter(Boolean).join(' · ')}</p>}<p className="guidance-body">{note.shared}</p>{note.concern&&<p className="guidance-body"><strong>Concern or question:</strong> {note.concern}</p>}{note.followup&&<p className="guidance-body"><strong>Follow-up:</strong> {note.followup}</p>}<button type="button" className="text-link" onClick={()=>{if(window.confirm('Delete this feedback note?'))update({feedbackNotes:notes.filter(n=>n.id!==note.id)})}}>Delete note</button></article>)}</section>
 <Notice>Do not treat one account as representative or independently verified. Only notes you enter are summarized here. Topic is free text because the reference menu options were not visible.</Notice>
 <Navigation back="/visit" next="/loan" go={go} nextText="Continue to loan guide"/></>;
}
export function LoanGuide({data,update,go}) {
 const loan=data.loan, docs=data.lenderChecks;
 const set=(key,value)=>patchObject(data,update,'loan',key,value);
 const setDoc=(id,key,value)=>update({lenderChecks:{...docs,[id]:{...docs[id],[key]:value}}});
 const emi=monthlyEmi(loan.amount||'',loan.rate||'',loan.years||'');
 let text= !loan.amount?.trim()?'Add a loan amount':!loan.rate?.trim()||!loan.years?.trim()?'Add the rate and tenure':emi===null?'Check the amount, rate and tenure':`₹${new Intl.NumberFormat('en-IN',{minimumFractionDigits:2,maximumFractionDigits:2}).format(emi)} / month`;
 return <><div className="page-head"><span className="eyebrow">STEP 5 OF 8 · NEW PROPERTY</span><h1>Home loan guide<span className="red-dot">.</span></h1><p>Explore an illustrative monthly payment and prepare questions for lenders.</p></div>
 <Notice>This estimate is not a lender offer, eligibility check or promise of approval. Actual terms, fees and rates vary.</Notice>
 <section className="card form-card"><div className="field-grid">{[['amount','Loan amount (₹)'],['rate','Annual interest rate (%)'],['years','Tenure (years)'],['lender','Lender name (optional)']].map(([id,label])=><label key={id}>{label}<input inputMode={id==='lender'?'text':id==='years'?'numeric':'decimal'} value={loan[id]||''} onChange={e=>set(id,e.target.value)} placeholder="Enter if known"/></label>)}</div></section>
 <section className="card form-card"><h3>Optional planning context</h3><p>These are your inputs only; FINNEST does not determine affordability or eligibility.</p><div className="field-grid">{[['income','Monthly household income estimate'],['obligations','Monthly obligations estimate']].map(([id,label])=><label key={id}>{label}<input inputMode="decimal" value={loan[id]||''} onChange={e=>set(id,e.target.value)} placeholder="Unknown"/></label>)}</div></section>
 <section className="card form-card loan-result"><span className="eyebrow">ILLUSTRATIVE MONTHLY EMI</span><h2 aria-live="polite">{text}</h2><p>Fixed-rate estimate excludes fees, insurance, rate changes and lender-specific terms. Enter an amount up to ₹1 trillion, rate 0–100%, and whole years 1–50.</p></section>
 <section className="card form-card"><h3>Questions for a lender</h3><ul><li>What is the effective rate and how can it change?</li><li>Which processing, legal or other fees apply?</li><li>What are the prepayment and late-payment terms?</li><li>What happens if disbursement timing changes?</li></ul></section>
 <section className="card form-card"><h3>Documents to ask the lender about</h3><p>Requirements vary. Track the conversation without uploading personal documents here. Status choices are provisional.</p>{LENDER_CHECKS.map(([id,title])=><div className="lender-check" key={id}><label>{title}<select value={docs[id]?.status||STATUSES[0]} onChange={e=>setDoc(id,'status',e.target.value)}>{statusChoices(docs[id]?.status).map(s=><option key={s}>{s}</option>)}</select></label><label>Note / source for {title}<input value={docs[id]?.note||''} placeholder="Optional note / source" onChange={e=>setDoc(id,'note',e.target.value)}/></label></div>)}</section>
 <Navigation back="/feedback" next="/legal" go={go} nextText="Continue to legal review"/></>;
}
export function LegalReview({data,update,go}) {
 const checks=data.legalChecks;
 const set=(id,key,value)=>update({legalChecks:{...checks,[id]:{...checks[id],[key]:value}}});
 return <><div className="page-head"><span className="eyebrow">STEP 6 OF 8 · NEW PROPERTY</span><h1>Legal due diligence<span className="red-dot">.</span></h1><p>Questions for an appropriately qualified local lawyer. Document relevance and requirements vary by location.</p></div>
 <Notice>FINNEST does not determine title, legal clearance or whether a property is safe to buy.</Notice>
 {LEGAL_CHECKS.map(([id,title])=><section className="card form-card check-card" key={id}><h3>{title}</h3><label>Status for {title}<select value={checks[id]?.status||STATUSES[0]} onChange={e=>set(id,'status',e.target.value)}>{statusChoices(checks[id]?.status).map(s=><option key={s}>{s}</option>)}</select></label><div className="field-grid"><label>Note / observation<input value={checks[id]?.note||''} onChange={e=>set(id,'note',e.target.value)} placeholder="Optional"/></label><label>Source / context<input value={checks[id]?.source||''} onChange={e=>set(id,'source',e.target.value)} placeholder="Who provided this?"/></label><label>Date / follow-up date, if any<input type="date" value={checks[id]?.date||''} onChange={e=>set(id,'date',e.target.value)}/></label></div></section>)}
 <p className="footnote">Status choices are provisional and represent your own notes, not legal findings.</p><section className="card form-card"><h3>Want legal guidance?</h3><p>Review the property-lawyer role and when independent advice may help. No expert profiles are configured.</p><button className="outline-button" onClick={()=>go('/experts')}>Explore expert roles <ArrowRight size={17}/></button></section>
 <Navigation back="/loan" next="/financial" nextText="Continue to financial review" go={go}/></>;
}
