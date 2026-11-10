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
