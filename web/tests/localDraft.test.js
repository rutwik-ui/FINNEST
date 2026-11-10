import test from 'node:test';
import assert from 'node:assert/strict';
import {DRAFT_KEY, decodeDraft, readDraft, writeDraft, deleteDraft, freshDraft} from '../src/localDraft.js';
function storage(initial=null) { let value=initial;return {getItem:key=>(assert.equal(key,DRAFT_KEY),value),setItem:(key,v)=>{assert.equal(key,DRAFT_KEY);value=v},removeItem:key=>{assert.equal(key,DRAFT_KEY);value=null},value:()=>value}; }
test('accepts old partial drafts and retains unknown properties',()=>{
 const data=decodeDraft('{"budget":{"loan":"42"},"future":{"note":"keep"}}');
 assert.equal(data.budget.loan,'42');assert.deepEqual(data.future,{note:'keep'});assert.deepEqual(data.projects,[]);
});
test('rejects malformed persisted structures instead of replacing them',()=>{
 for(const raw of ['{', 'null','[]','{"budget":null}','{"checks":{"x":null}}','{"projects":[{"id":1}]}','{"location":"x"}','{"journey":"unknown"}']){
  const store=storage(raw);const read=readDraft(store);assert.equal(read.issue,'invalid');assert.equal(store.value(),raw);
 }
});
test('checks for other-tab changes before saving and deleting',()=>{
 const store=storage(); const start=readDraft(store);const first=writeDraft(store,start.raw,{...freshDraft(),budget:{loan:'10'}});
 assert.equal(first.issue,null);assert.equal(writeDraft(store,null,freshDraft()).issue,'conflict');assert.equal(deleteDraft(store,null).issue,'conflict');
 assert.equal(JSON.parse(store.value()).budget.loan,'10');assert.equal(deleteDraft(store,first.raw).issue,null);assert.equal(store.value(),null);
});
test('storage errors are surfaced without claiming success',()=>{
 const denied={getItem(){throw Error('denied')}};
 assert.equal(readDraft(denied).issue,'unavailable');assert.equal(writeDraft(denied,null,freshDraft()).issue,'unavailable');assert.equal(deleteDraft(denied,null).issue,'unavailable');
 const full={getItem:()=>null,setItem(){throw Error('full')},removeItem(){throw Error('denied')}};
 assert.equal(writeDraft(full,null,freshDraft()).issue,'unavailable');assert.equal(deleteDraft(full,null).issue,'unavailable');
});
test('new journey structures are validated and partial feedback is restored safely',()=>{
 const draft=decodeDraft('{"feedbackForm":{"topic":"Water"},"loan":{"amount":"100"}}');
 assert.equal(draft.feedbackForm.topic,'Water');assert.equal(draft.feedbackForm.shared,'');
 for(const raw of ['{"feedbackNotes":[{}]}','{"feedbackForm":{"topic":7}}','{"loan":[]}','{"legalChecks":{"x":{"note":null}}}','{"lenderChecks":null}']) assert.equal(readDraft(storage(raw)).issue,'invalid');
});

test('financial structures reject malformed records and preserve them',()=>{
 for(const raw of ['{"initialCosts":null}','{"recurringCosts":{"emi":42}}','{"costChecks":{"tax":false}}','{"loanTerms":[]}','{"nextActions":null}']) { const store=storage(raw);assert.equal(readDraft(store).issue,'invalid');assert.equal(store.value(),raw); }
});
test('resale values validate without overwriting malformed data',()=>{
 for(const raw of ['{"resaleBudget":[]}','{"resalePreferences":{"carpet":null}}','{"resaleConfiguration":"lift"}','{"resaleLocation":[3]}','{"resaleCondition":{"walls":{"note":false}}}']) {
  const store=storage(raw);assert.equal(readDraft(store).issue,'invalid');assert.equal(store.value(),raw);
 }
 const older=decodeDraft('{"budget":{"loan":"500"}}');assert.deepEqual(older.resaleBudget,{});assert.deepEqual(older.resaleLocation,[]);
});
