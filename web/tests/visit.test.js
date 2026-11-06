import { test } from 'node:test';
import { strict as assert } from 'node:assert';
import { VISIT_CHECKS, updateVisitCheck } from '../src/visit.js';

test('visit observations cover all 14 reference prompts with unique keys', () => {
  assert.equal(VISIT_CHECKS.length, 14);
  assert.equal(new Set(VISIT_CHECKS.map(([id]) => id)).size, 14);
  assert.equal(VISIT_CHECKS.at(-1)[1], 'Garden / play area');
});
test('editing a visit field preserves other fields and checks without mutation', () => {
  const original = { water: { note: 'Ask about supply', status: 'Not checked' }, light: { note: 'Morning' } };
  const updated = updateVisitCheck(original, 'water', 'source', 'Resident');
  assert.equal(updated.water.note, 'Ask about supply');
  assert.equal(updated.water.source, 'Resident');
  assert.deepEqual(updated.light, original.light);
  assert.equal(original.water.source, undefined);
  assert.deepEqual(updateVisitCheck({}, 'noise', 'date', '2026-11-07'), { noise: { date: '2026-11-07' } });
});
