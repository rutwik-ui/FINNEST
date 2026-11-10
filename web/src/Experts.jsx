import React,{useState} from 'react';
import {ArrowRight,Info,UserRound} from 'lucide-react';
export const ROLES=[
 ['Property lawyer','Title, agreements, disclosures and local requirements.'],
 ['CA / tax advisor','Transaction costs and personal financial questions.'],
 ['Home loan advisor','Loan terms, eligibility, fees and repayment obligations.'],
 ['Property consultant','Independent process or local market context.'],
 ['Architect / technical inspector','Condition, plans, structure or visible concerns.'],
 ['Interior expert','Layout, finishes and interior planning.'],
 ['Valuation expert','Independent valuation within their professional scope.']
];
export function Experts({go}) {
 const [role,setRole]=useState('all');const visible=role==='all'?ROLES:ROLES.filter(([name])=>name===role);
 return <><div className="page-head"><span className="eyebrow">FIND THE RIGHT KIND OF HELP</span><h1>Experts directory<span className="red-dot">.</span></h1><p>Know what each professional can help with and when a conversation may be useful.</p></div>
 <section className="card form-card"><label>Role category<select value={role} onChange={e=>setRole(e.target.value)}><option value="all">All roles</option>{ROLES.map(([name])=><option key={name}>{name}</option>)}</select></label><p>The filter changes informational roles only; it does not filter or imply available professionals.</p></section>
 <div className="notice"><Info size={20}/><span>No experts are configured. No profiles, credentials, ratings, availability, prices or contact details are supplied.</span></div>
 <h2>Professional guidance</h2><div className="expert-grid">{visible.map(([name,description])=><article className="card expert-card" key={name}><div className="project-icon"><UserRound/></div><h3>{name}</h3><p>{description}</p></article>)}</div>
 {role==='all'&&<section className="card form-card admin-space"><h2>Optional personal preferences</h2><p>Personal choices only—not legal, financial or technical due diligence.</p><p><strong>Vastu expert · optional</strong> — Personal preference guidance; not due diligence.</p><p><strong>Astrologer · optional</strong> — Personal preference only; not due diligence.</p></section>}
 <button className="outline-button admin-space" onClick={()=>go('/expert-detail')}>Expert detail & contact <ArrowRight size={16}/></button></>;
}
export function ExpertDetail({go}) {return <><div className="page-head"><span className="eyebrow">EXPERT DETAIL</span><h1>A clearer way to ask for help<span className="red-dot">.</span></h1><p>Expert profiles and introductions are not configured.</p></div><section className="card empty"><Info/><h2>No configured expert selected</h2><p>No request can be sent and no professional contact information is collected.</p></section><div className="notice admin-space"><Info size={20}/><span>There is no booking, messaging, contact-sharing or payment service connected.</span></div><button className="primary" disabled>Request a conversation · unavailable</button><button className="outline-button admin-space" onClick={()=>go('/experts')}>Back to directory</button></>}
