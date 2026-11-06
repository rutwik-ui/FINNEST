import { test } from 'node:test';
import { strict as assert } from 'node:assert';
import { addProject, renameProject, removeProject } from '../src/projects.js';

test('adds trimmed names, rejects empty names, caps new entries at five', () => {
  let items = addProject([], '  First  ', '1');
  assert.deepEqual(items, [{ id: '1', name: 'First' }]);
  assert.deepEqual(addProject(items, '  ', '2'), items);
  for (let i = 2; i <= 5; i++) items = addProject(items, `Home ${i}`, `${i}`);
  assert.equal(addProject(items, 'Sixth', '6').length, 5);
});
test('rename and remove preserve other entries, including older lists over the cap', () => {
  const items = Array.from({length: 6}, (_, i) => ({id: i, name: `Old ${i}`}));
  const renamed = renameProject(items, 2, ' Updated ');
  assert.equal(renamed[2].name, 'Updated');
  assert.equal(renamed[1].name, 'Old 1');
  assert.deepEqual(renameProject(renamed, 2, ' '), renamed);
  assert.equal(removeProject(renamed, 2).length, 5);
  assert.equal(items.length, 6);
});
