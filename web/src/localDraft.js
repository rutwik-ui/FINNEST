import {emptyFeedback} from './newJourney.js';
import {emptyReminder,validReminderDate} from './common.js';
// Browser-local data only. Do not use this storage format as a cloud-sync protocol.
export const DRAFT_KEY = 'finnest-web-v1';
export const freshDraft = () => ({journey:'New Property',budget:{},location:[],configuration:[],checks:{},visitChecks:{},projects:[],feedbackForm:emptyFeedback(),feedbackNotes:[],loan:{},lenderChecks:{},legalChecks:{},initialCosts:{},recurringCosts:{},costChecks:{},loanTerms:{},nextActions:'',resaleBudget:{},resalePreferences:{},resaleConfiguration:[],resaleLocation:[],resaleCondition:{},resaleSeller:{},resaleSociety:{},resaleLegal:{},resaleHandover:{},resaleLawyerStatus:'',localProfile:{},reminderForm:emptyReminder(),reminders:[]});
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
    ['location','configuration','resaleConfiguration','resaleLocation'].some(k=>value[k] !== undefined && (!Array.isArray(value[k]) || !value[k].every(v=>typeof v==='string'))) ||
    ['checks','visitChecks','lenderChecks','legalChecks','costChecks','resaleCondition','resaleSeller','resaleSociety','resaleLegal','resaleHandover'].some(k=>value[k] !== undefined && !checkValues(value[k])) ||
    ['loan','initialCosts','recurringCosts','loanTerms','resaleBudget','resalePreferences','localProfile'].some(k=>value[k] !== undefined && !stringValues(value[k])) ||
    (['nextActions','resaleLawyerStatus'].some(k=>value[k] !== undefined && typeof value[k] !== 'string')) ||
    (value.reminderForm !== undefined && !stringValues(value.reminderForm)) ||
    (value.reminders !== undefined && (!Array.isArray(value.reminders) || !value.reminders.every(r=>stringValues(r) && ['id','title','date','note'].every(k=>typeof r[k]==='string') && validReminderDate(r.date)))) ||
    (value.feedbackForm !== undefined && !stringValues(value.feedbackForm)) ||
    (value.feedbackNotes !== undefined && (!Array.isArray(value.feedbackNotes) || !value.feedbackNotes.every(n=>stringValues(n) && ['id','topic','date','context','shared','concern','followup'].every(k=>typeof n[k]==='string')))) ||
    (value.projects !== undefined && (!Array.isArray(value.projects) || !value.projects.every(p=>object(p) && typeof p.id==='string' && typeof p.name==='string')))) throw new Error('invalid');
 return {...freshDraft(),...value,feedbackForm:{...emptyFeedback(),...value.feedbackForm},reminderForm:{...emptyReminder(),...value.reminderForm}};
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

// Recovery files are deliberately not a cloud-sync or Android interchange format.
export function decodeRecovery(raw) {
 if(typeof raw!=='string' || raw.length>5*1024*1024) throw new Error('invalid');
 const value=JSON.parse(raw);
 if(!object(value) || !Object.keys(freshDraft()).some(key=>Object.hasOwn(value,key))) throw new Error('invalid');
 return decodeDraft(raw);
}
export function restoreDraft(storage,expected,raw) {
 let data;try{data=decodeRecovery(raw)}catch{return {issue:'invalid-file'}}
 try {
  if(storage.getItem(DRAFT_KEY)!==expected)return {issue:'conflict'};
  storage.setItem(DRAFT_KEY,raw);
  return {issue:null,raw,data};
 } catch{return {issue:'unavailable'}}
}
