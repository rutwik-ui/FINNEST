import React from 'react';
import {ArrowLeft,ArrowRight,Check,Info,Plus} from 'lucide-react';
import {STATUSES} from './newJourney';
import {RESALE_BUDGET,RESALE_CONFIG,RESALE_LOCATION,RESALE_CONDITION} from './resale';

const options=value=>value && !STATUSES.includes(value)?[value,...STATUSES]:STATUSES;
export function ResaleOverview({go}) {
 const sections=[['Requirements & location','/resale-requirements'],['Property condition','/resale-condition'],['Seller, ownership & title',null],['Society documents & dues',null],['Legal review',null],['Agreement, registration & handover',null]];
 return <><div className="page-head"><span className="eyebrow">A SEPARATE HOME-BUYING JOURNEY</span><h1>Resale Property Journey<span className="red-dot">.</span></h1><p>Explore key areas when considering a resale home, at your own pace.</p></div>
 <div className="notice"><Info size={20}/><span>These are the subject areas shown in the prototype, not a complete resale journey. The remaining areas are not yet available on the website.</span></div>
 <section className="card form-card"><h2>Confirmed subject areas</h2><div className="resale-sections">{sections.map(([name,path])=><button className="outline-button" key={name} disabled={!path} onClick={()=>path&&go(path)}>{name} {path?<ArrowRight size={17}/>:<span>Coming soon</span>}</button>)}</div></section>
 <p className="footnote">FINNEST does not inspect a property, verify documents or provide legal advice. Notes are saved only in this browser.</p><button className="primary" onClick={()=>go('/resale-requirements')}>Start / continue resale checks <ArrowRight size={18}/></button></>;
}
export function ResaleRequirements({data,update,go}) {
 const set=(key,field,value)=>update({[key]:{...data[key],[field]:value}});
 const toggle=(key,id)=>update({[key]:data[key].includes(id)?data[key].filter(v=>v!==id):[...data[key],id]});
 const chips=(key,items)=><div className="chips">{items.map(([id,name])=><button type="button" key={id} className={data[key].includes(id)?'chip selected':'chip'} aria-pressed={data[key].includes(id)} onClick={()=>toggle(key,id)}>{data[key].includes(id)?<Check size={16}/>:<Plus size={16}/>} {name}</button>)}</div>;
 return <><div className="page-head"><span className="eyebrow">RESALE · PLANNING NOTES</span><h1>Requirements & location<span className="red-dot">.</span></h1><p>Optional preferences and estimated costs for your resale search.</p></div>
 <div className="notice"><Info size={20}/><span>Cost reminders only. Applicable costs vary; no current tax or fee rate is assumed. Blank amounts remain unknown.</span></div>
 <section className="card form-card"><h2>Budget & comfort</h2><p>Estimates are optional and only for planning. No current rates or fees are supplied.</p><div className="field-grid">{RESALE_BUDGET.map(([id,name])=><label key={id}>{name} (₹)<input inputMode="decimal" value={data.resaleBudget[id]||''} onChange={e=>set('resaleBudget',id,e.target.value)} placeholder="Unknown / not provided"/></label>)}</div></section>
 <section className="card form-card"><h2>Configuration & practical needs</h2>{chips('resaleConfiguration',RESALE_CONFIG)}<div className="field-grid">{[['carpet','Carpet area'],['building_age','Building age preference'],['timeline','Timeline or family preferences']].map(([id,name])=><label key={id}>{name}<input value={data.resalePreferences[id]||''} onChange={e=>set('resalePreferences',id,e.target.value)} placeholder="Optional"/></label>)}</div></section>
 <section className="card form-card"><h2>Location priorities</h2>{chips('resaleLocation',RESALE_LOCATION)}</section>
 <div className="step-actions"><button className="outline-button" onClick={()=>go('/journey')}><ArrowLeft size={17}/> Back to resale journey</button><button className="outline-button" onClick={()=>go('/')}>Return home</button><button className="primary compact" onClick={()=>go('/resale-condition')}>Continue to condition <ArrowRight size={18}/></button></div></>;
}
export function ResaleCondition({data,update,go}) {
 const checks=data.resaleCondition;
 const set=(id,field,value)=>update({resaleCondition:{...checks,[id]:{...checks[id],[field]:value}}});
 return <><div className="page-head"><span className="eyebrow">RESALE PROPERTY · YOUR REVIEW</span><h1>Condition inspection<span className="red-dot">.</span></h1><p>Record what you observe; arrange a qualified technical inspection for concerns. This checklist cannot assess structural safety.</p></div>
 <div className="notice"><Info size={20}/><span>Keep your observations, information shared by a seller, evidence reviewed and professional advice distinct. Status choices are provisional.</span></div>
 {RESALE_CONDITION.map(([id,title])=><section className="card form-card check-card" key={id}><h3>{title}</h3><label>Status for {title}<select value={checks[id]?.status||STATUSES[0]} onChange={e=>set(id,'status',e.target.value)}>{options(checks[id]?.status).map(s=><option key={s}>{s}</option>)}</select></label><div className="field-grid"><label>Note / observation<input value={checks[id]?.note||''} placeholder="Optional" onChange={e=>set(id,'note',e.target.value)}/></label><label>Source / context<input value={checks[id]?.source||''} placeholder="Who provided this?" onChange={e=>set(id,'source',e.target.value)}/></label><label>Date / follow-up date, if any<input type="date" value={checks[id]?.date||''} onChange={e=>set(id,'date',e.target.value)}/></label></div></section>)}
 <p className="footnote">These are your notes, not a technical inspection or independent verification. Seller and title notes are not yet available on the website.</p><div className="step-actions"><button className="outline-button" onClick={()=>go('/resale-requirements')}><ArrowLeft size={17}/> Back to requirements</button><button className="outline-button" onClick={()=>go('/journey')}>Resale overview</button><button className="outline-button" onClick={()=>go('/')}>Return home</button></div></>;
}
