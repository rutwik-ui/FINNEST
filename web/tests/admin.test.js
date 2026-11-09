import { test } from 'node:test';
import { strict as assert } from 'node:assert';
import { validateGuidance, CONTENT_ROLES } from '../src/admin.js';
const good = {title:'Useful advice', body:'Ask a lawyer.', category:'new_property', status:'draft'};
test('validates content boundaries and rejects invalid states',()=>{
 assert.equal(validateGuidance(good),null);
 assert.ok(validateGuidance({...good,title:'   '}));
 assert.ok(validateGuidance({...good,body:'x'.repeat(10001)}));
 assert.ok(validateGuidance({...good,category:'other'}));
 assert.ok(validateGuidance({...good,status:'private'}));
});
test('only the two content roles are offered the editor',()=>{
 assert.equal(CONTENT_ROLES.includes('content_manager'),true);
 assert.equal(CONTENT_ROLES.includes('support_manager'),false);
 assert.equal(CONTENT_ROLES.includes('expert_manager'),false);
});
