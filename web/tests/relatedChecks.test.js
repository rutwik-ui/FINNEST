import test from 'node:test';
import assert from 'node:assert/strict';
import {relatedChecks,pendingPatch} from '../src/relatedChecks.js';
import {freshDraft} from '../src/localDraft.js';
test('maps both journeys to unique and separate known checklist targets',()=>{
 const fresh=freshDraft();const newItems=relatedChecks('New Property'), resale=relatedChecks('Resale Property');
 assert.equal(newItems.length,44);assert.equal(resale.length,45);assert.equal(new Set([...newItems,...resale].map(x=>x.value)).size,89);
 assert.equal(pendingPatch(fresh,resale[0]),null);
 const patch=pendingPatch({...fresh,checks:{track:{note:'keep'}}},{...newItems[0],key:'resaleCondition'});
 assert.equal(patch.checks.track.status,'Pending / unknown');assert.equal(patch.checks.track.note,'keep');assert.equal(patch.resaleCondition,undefined);
});
