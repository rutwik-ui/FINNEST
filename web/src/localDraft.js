// Browser-local data only. Do not use this storage format as a cloud-sync protocol.
export const DRAFT_KEY = 'finnest-web-v1';
export const freshDraft = () => ({journey:'New Property',budget:{},location:[],configuration:[],checks:{},visitChecks:{},projects:[]});
const object = value => value !== null && typeof value === 'object' && !Array.isArray(value);
const stringValues = value => object(value) && Object.values(value).every(v => typeof v === 'string');
const checkValues = value => object(value) && Object.values(value).every(stringValues);

// Preserve unfamiliar properties for forwards compatibility. Reject malformed known
// structures rather than falling back and accidentally overwriting the stored value.
export function decodeDraft(raw) {
 if(raw === null) return freshDraft();
 let value; try { value=JSON.parse(raw); } catch { throw new Error('invalid'); }
 if(!object(value) || (value.journey !== undefined && !['New Property','Resale Property'].includes(value.journey)) ||
    (value.budget !== undefined && !stringValues(value.budget)) ||
    ['location','configuration'].some(k=>value[k] !== undefined && (!Array.isArray(value[k]) || !value[k].every(v=>typeof v==='string'))) ||
    ['checks','visitChecks'].some(k=>value[k] !== undefined && !checkValues(value[k])) ||
    (value.projects !== undefined && (!Array.isArray(value.projects) || !value.projects.every(p=>object(p) && typeof p.id==='string' && typeof p.name==='string')))) throw new Error('invalid');
 return {...freshDraft(),...value};
}
export function readDraft(storage) {
 try {const raw=storage.getItem(DRAFT_KEY);try{return {data:decodeDraft(raw),raw,issue:null}}catch{return {data:freshDraft(),raw,issue:'invalid'}}}
 catch{return {data:freshDraft(),raw:null,issue:'unavailable'}}
}
// Compare before each mutation to avoid silently overwriting changes from another tab.
// localStorage has no atomic compare-and-swap; simultaneous writes can still race.
export function writeDraft(storage, expected, data) {
 try {
  if(storage.getItem(DRAFT_KEY)!==expected) return {issue:'conflict'};
  const raw=JSON.stringify(data);storage.setItem(DRAFT_KEY,raw);return {raw,issue:null};
 } catch {return {issue:'unavailable'}}
}
export function deleteDraft(storage, expected) {
 try {if(storage.getItem(DRAFT_KEY)!==expected)return {issue:'conflict'};storage.removeItem(DRAFT_KEY);return {raw:null,issue:null}}
 catch{return {issue:'unavailable'}}
}
