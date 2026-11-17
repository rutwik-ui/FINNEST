import React,{useState} from 'react';
import {ArrowRight,Info,UserRound} from 'lucide-react';
import {PublishedExperts} from './LiveData';
import { Button } from '@/components/ui/button';
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
 const [role,setRole]=useState('all'),[search,setSearch]=useState('');const visible=role==='all'?ROLES:ROLES.filter(([name])=>name===role);
 return <><div className="page-head"><span className="eyebrow">FIND THE RIGHT KIND OF HELP</span><h1>Experts directory<span className="red-dot">.</span></h1><p>Know what each professional can help with and when a conversation may be useful.</p></div>
 <div className="directory-filters"><label>Search experts<input type="search" value={search} onChange={e=>setSearch(e.target.value)} placeholder="Name, city or specialty"/></label><label>Role category<select value={role} onChange={e=>setRole(e.target.value)}><option value="all">All roles</option>{ROLES.map(([name])=><option key={name}>{name}</option>)}</select></label></div>
 <PublishedExperts role={role} search={search}/><h2 className="admin-space">Professional guidance</h2><div className="expert-grid">{visible.map(([name,description])=><article className="card expert-card" key={name}><div className="project-icon"><UserRound/></div><h3>{name}</h3><p>{description}</p></article>)}</div>
 {role==='all'&&<section className="card form-card admin-space"><h2>Optional personal preferences</h2><p>Personal choices only—not legal, financial or technical due diligence.</p><p><strong>Vastu expert · optional</strong> — Personal preference guidance; not due diligence.</p><p><strong>Astrologer · optional</strong> — Personal preference only; not due diligence.</p></section>}
 </>;
}
export function ExpertDetail({go}) {return <><div className="page-head"><span className="eyebrow">EXPERT DETAIL</span><h1>A clearer way to ask for help<span className="red-dot">.</span></h1><p>Expert profiles and introductions are not configured.</p></div><section className="card empty"><Info/><h2>No configured expert selected</h2><p>No request can be sent and no professional contact information is collected.</p></section><div className="notice admin-space"><Info size={20}/><span>There is no booking, messaging, contact-sharing or payment service connected.</span></div><button className="primary" disabled>Request a conversation · unavailable</button><button className="outline-button admin-space" onClick={()=>go('/experts')}>Back to directory</button></>}
