import test from 'node:test';
import assert from 'node:assert/strict';
import {RESALE_BUDGET, RESALE_CONFIG, RESALE_LOCATION, RESALE_CONDITION} from '../src/resale.js';
test('prototype areas have stable unique keys',()=>{
 for(const [items,count] of [[RESALE_BUDGET,10],[RESALE_CONFIG,8],[RESALE_LOCATION,9],[RESALE_CONDITION,12]]){
  assert.equal(items.length,count);assert.equal(new Set(items.map(([id])=>id)).size,count);
 }
});
