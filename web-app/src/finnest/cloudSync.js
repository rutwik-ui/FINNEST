// Account sync: one finnest_journeys row per user and journey type, shared by web, Android and admin.
import { useEffect, useRef, useState } from 'react';
import { supabase } from './supabase';
import { offlineStore, mergeJourney, sameJourney } from './offlineStore';
import { freshDraft } from './localDraft';

export const STEP_KEYS = {
  new: [
    ['Requirements & budget', ['budget', 'location', 'configuration']],
    ['Developer checks', ['checks']],
    ['Shortlist projects', ['projects']],
    ['Visit observations', ['visitChecks', 'feedbackNotes']],
    ['Home loan guide', ['loan', 'lenderChecks']],
    ['Legal due diligence', ['legalChecks']],
    ['Financial due diligence', ['initialCosts', 'recurringCosts', 'costChecks', 'loanTerms']],
    ['Final review', ['nextActions']],
  ],
  resale: [
    ['Requirements & location', ['resaleBudget', 'resalePreferences', 'resaleConfiguration', 'resaleLocation']],
    ['Property condition', ['resaleCondition']],
    ['Seller, ownership & title', ['resaleSeller']],
    ['Society documents & dues', ['resaleSociety']],
    ['Legal review', ['resaleLegal']],
    ['Agreement & handover', ['resaleHandover']],
    ['Journey summary', ['resaleLawyerStatus']],
  ],
};
// Transient form state stays on the device.
const LOCAL_ONLY = ['feedbackForm', 'reminderForm'];

export const hasContent = v => v == null ? false : typeof v === 'string' ? v.trim() !== '' : Array.isArray(v) ? v.length > 0 : typeof v === 'object' ? Object.values(v).some(hasContent) : Boolean(v);
export const journeyOfKey = k => k.startsWith('resale') ? 'resale' : 'new';

export function splitDraft(data) {
  const out = { new: {}, resale: {} };
  for (const [k, v] of Object.entries(data)) if (!LOCAL_ONLY.includes(k)) out[journeyOfKey(k)][k] = v;
  return out;
}
export const completedSteps = (type, payload) => STEP_KEYS[type].filter(([, keys]) => keys.some(k => hasContent(payload[k]))).map(([label]) => label);

export function deviceId() {
  try { let id = localStorage.getItem('finnest-device'); if (!id) { id = crypto.randomUUID(); localStorage.setItem('finnest-device', id); } return id; } catch { return 'web'; }
}
export const logActivity = (event, detail = {}) => supabase.from('finnest_activity').insert({ event, detail, platform: 'web' }).then(() => {}, () => {});

// status: 'off' | 'loading' | 'saving' | 'saved' | 'offline' | 'error'
export function useCloudSync({ session, data, applyRemote, currentPath }) {
  const userId = session?.user?.id;
  const [status,setStatus]=useState('off'), [notice,setNotice]=useState('');
  const dataRef=useRef(data), applyRef=useRef(applyRemote), controller=useRef(null);
  dataRef.current=data;applyRef.current=applyRemote;
  useEffect(()=>{
    setNotice('');
    if(!userId){setStatus('off');return;}
    let alive=true, initialized=false, running=false, timer, retry, retryCount=0;
    const bases=splitDraft(freshDraft()), versions={}, device=deviceId(), initialData=dataRef.current;
    const key=type=>`${userId}:${type}`;
    const persist=async()=>{
      for(const type of ['new','resale']){
        const payload=splitDraft(dataRef.current)[type], base=bases[type]||{};
        if(!alive)return;
        await offlineStore().journeys.put({key:key(type),userId,type,payload,base,version:versions[type]||0,pending:!sameJourney(payload,base)});
      }
    };
    const mergeRow=row=>{
      const type=row.journey_type;if(!['new','resale'].includes(type))return;
      const mine=splitDraft(dataRef.current)[type];
      const merged=mergeJourney(bases[type]||{},mine,row.payload||{});
      bases[type]=row.payload||{};versions[type]=row.version;
      // Defaults prevent old fields from a different account or a partial payload from surviving.
      const part={...splitDraft(freshDraft())[type],...merged};
      dataRef.current={...dataRef.current,...part};applyRef.current(part);
    };
    const pull=async()=>{
      const res=await supabase.from('finnest_journeys').select('journey_type,payload,version').eq('user_id',userId);
      if(!alive)return;
      if(res.error)throw res.error;
      for(const row of res.data||[])mergeRow(row);
      initialized=true;await persist();
    };
    const flush=async()=>{
      if(!alive||running)return;running=true;clearTimeout(retry);
      try{
        if(!navigator.onLine){await persist();if(alive)setStatus('offline');return;}
        if(!initialized)await pull();
        if(!alive)return;
        setStatus('saving');
        for(const type of ['new','resale']){
          for(let attempt=0;attempt<3;attempt++){
            if(!alive)return;
            const payload=splitDraft(dataRef.current)[type];
            if(sameJourney(payload,bases[type]||{}))break;
            await persist();if(!alive)return;
            const steps=completedSteps(type,payload),row={payload,completed_steps:steps,current_step:currentPath.current,updated_by_device:device};
            const query=versions[type]
              ?supabase.from('finnest_journeys').update(row).eq('user_id',userId).eq('journey_type',type).eq('version',versions[type])
              :supabase.from('finnest_journeys').insert({...row,user_id:userId,journey_type:type});
            const res=await query.select('payload,version');if(!alive)return;
            if(res.error&&res.error.code!=='23505')throw res.error;
            if(res.error||!res.data?.length){
              const latest=await supabase.from('finnest_journeys').select('journey_type,payload,version').eq('user_id',userId).eq('journey_type',type).maybeSingle();
              if(!alive)return;if(latest.error)throw latest.error;
              if(latest.data)mergeRow(latest.data);else versions[type]=0;
              if(attempt===2)throw new Error('Concurrent edits; retry');
              continue;
            }
            const previous=completedSteps(type,bases[type]||{});
            bases[type]=res.data[0].payload;versions[type]=res.data[0].version;
            const added=steps.filter(s=>!previous.includes(s));if(added.length)logActivity('step_saved',{journey:type,steps:added});
            break;
          }
        }
        await persist();if(!alive)return;
        retryCount=0;
        const pending=['new','resale'].some(t=>!sameJourney(splitDraft(dataRef.current)[t],bases[t]||{}));
        setStatus(pending?'saving':'saved');if(pending)retry=setTimeout(flush,300);
      }catch{if(alive){setStatus(navigator.onLine?'error':'offline');retry=setTimeout(flush,Math.min(30000,1000*2**retryCount++));}}
      finally{running=false;}
    };
    const changed=()=>{clearTimeout(timer);timer=setTimeout(async()=>{try{await persist();}catch{if(alive)setNotice('Offline storage is unavailable. Keep this page open until saved.');}void flush();},400);};
    controller.current={userId,changed};
    setStatus(navigator.onLine?'loading':'offline');
    void(async()=>{
      try{
        const cached=await offlineStore().journeys.where('userId').equals(userId).toArray();if(!alive)return;
        for(const row of cached){bases[row.type]=row.base;versions[row.type]=row.version;const merged=mergeJourney(splitDraft(initialData)[row.type],splitDraft(dataRef.current)[row.type],row.payload);dataRef.current={...dataRef.current,...merged};applyRef.current(merged);}
      }catch{if(alive)setNotice('Offline storage is unavailable.');}
      if(alive)void flush();
    })();
    const channel=supabase.channel(`journeys-${userId}-${crypto.randomUUID()}`).on('postgres_changes',{event:'UPDATE',schema:'public',table:'finnest_journeys',filter:`user_id=eq.${userId}`},({new:row})=>{
      if(!alive||!initialized||row.updated_by_device===device||row.version<=(versions[row.journey_type]||0))return;
      mergeRow(row);setNotice('Updated with your other device’s saved answers.');changed();
    }).subscribe();
    const online=()=>{initialized=false;void flush();}, offline=()=>setStatus('offline');
    window.addEventListener('online',online);window.addEventListener('offline',offline);
    return()=>{alive=false;clearTimeout(timer);clearTimeout(retry);controller.current=null;void supabase.removeChannel(channel);window.removeEventListener('online',online);window.removeEventListener('offline',offline);};
  },[userId]);
  useEffect(()=>{const current=controller.current;if(current&&current.userId===userId)current.changed();},[data,userId]);
  return {status,notice,clearNotice:()=>setNotice('')};
}
