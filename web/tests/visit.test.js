import { test } from 'node:test';
import { strict as assert } from 'node:assert';
import { VISIT_CHECKS, updateVisitCheck } from '../src/visit.js';
test('visit observations have the 14 reference prompts and unique keys', () => {
  assert.equal(VISIT_CHECKS.length, 14);
  assert.equal(new Set(VISIT_CHECKS.map(([id]) => id)).size, 14);
  assert.equal(VISIT_CHECKS.at(-1)[1], 'Garden / play area');
});
test('editing preserves other fields and does not mutate the previous draft', () => {
  const original = { water: { note: 'Ask about supply', status: 'Not checked' }, light: { note: 'Morning' } };
  const updated = updateVisitCheck(original, 'water', 'source', 'Resident');
  assert.equal(updated.water.note, 'Ask about supply');
  assert.equal(updated.water.source, 'Resident');
  assert.deepEqual(updated.light, original.light);
  assert.equal(original.water.source, undefined);
});
