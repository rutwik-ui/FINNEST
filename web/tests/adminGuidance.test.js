import test from 'node:test';
import assert from 'node:assert/strict';
import {ADMIN_PAGE_SIZE, loadAdminGuidance} from '../src/adminGuidance.js';

function client(data) {
 const calls=[];
 const query={select:(...a)=>(calls.push(['select',...a]),query),eq:(...a)=>(calls.push(['eq',...a]),query),or:(...a)=>(calls.push(['or',...a]),query),order:(...a)=>(calls.push(['order',...a]),query),limit:async(...a)=>(calls.push(['limit',...a]),{data,error:null})};
 return {calls,from:table=>(calls.push(['from',table]),query)};
}
const row=n=>({id:`00000000-0000-4000-8000-${String(n).padStart(12,'0')}`,updated_at:'2026-01-01T00:00:00.123456+00:00',title:`Item ${n}`});
test('filters server-side, orders by timestamp and id, and uses lookahead for cursor',async()=>{
 const db=client(Array.from({length:51},(_,i)=>row(51-i)));
 const first=await loadAdminGuidance(db,{category:'resale_property',status:'draft'});
 assert.equal(first.items.length,ADMIN_PAGE_SIZE);assert.deepEqual(first.cursor,{id:row(2).id,updated_at:row(2).updated_at});
 assert.deepEqual(db.calls.filter(c=>c[0]==='eq'),[['eq','category','resale_property'],['eq','status','draft']]);
 assert.deepEqual(db.calls.filter(c=>c[0]==='order').map(c=>c[1]),['updated_at','id']);
 assert.deepEqual(db.calls.at(-1),['limit',51]);
 const second=client([row(1)]);const result=await loadAdminGuidance(second,{category:'all',status:'all'},first.cursor);
 assert.equal(result.cursor,null);assert.deepEqual(result.items,[row(1)]);
 assert.equal(second.calls.find(c=>c[0]==='or')[1],`updated_at.lt."${row(2).updated_at}",and(updated_at.eq."${row(2).updated_at}",id.lt.${row(2).id})`);
});
test('rejects untrusted filters and malformed cursors before query interpolation',async()=>{
 const db=client([]);
 await assert.rejects(loadAdminGuidance(db,{category:'all',status:'draft,whatever'}),/Invalid filter/);
 await assert.rejects(loadAdminGuidance(db,{category:'all',status:'all'},{id:'bad),or(x',updated_at:row(1).updated_at}),/Invalid cursor/);
 await assert.rejects(loadAdminGuidance(client(Array.from({length:51},()=>({id:'bad',updated_at:'bad'}))),{category:'all',status:'all'}),/Invalid pagination data/);
});
