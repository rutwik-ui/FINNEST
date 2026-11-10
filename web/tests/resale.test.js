import test from 'node:test';
import assert from 'node:assert/strict';
import {RESALE_BUDGET, RESALE_CONFIG, RESALE_LOCATION, RESALE_CONDITION, RESALE_SECTIONS, resaleRows, resalePending} from '../src/resale.js';
test('prototype areas have stable unique keys',()=>{
 for(const [items,count] of [[RESALE_BUDGET,10],[RESALE_CONFIG,8],[RESALE_LOCATION,9],[RESALE_CONDITION,12]]){
  assert.equal(items.length,count);assert.equal(new Set(items.map(([id])=>id)).size,count);
 }
});

test('four remaining sections have unique stable keys and expected prompt counts',()=>{
 assert.deepEqual(RESALE_SECTIONS.map(s=>s.checks.length),[5,7,8,13]);
 for(const section of RESALE_SECTIONS)assert.equal(new Set(section.checks.map(([id])=>id)).size,section.checks.length);
});
test('summary does not guess what free text statuses mean',()=>{
 const data={resaleCondition:{walls:{status:'Checked by you'}},resaleSeller:{},resaleSociety:{maintenance:{status:'Paid'}},resaleLegal:{},resaleHandover:{}};
 const rows=resaleRows(data);assert.equal(rows.length,45);
 assert.equal(resalePending(rows.find(x=>x.title==='Maintenance dues')),false);
 assert.equal(resalePending(rows.find(x=>x.title==='Seller identity & authority')),true);
 assert.equal(resalePending({status:'Pending / unknown'}),true);
});
