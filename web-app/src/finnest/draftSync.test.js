import { describe, expect, it } from 'vitest';
import {
  JOURNEY_TYPES, fieldKeyFor, toFieldMap, fromFieldMap, resolveField,
  mergeFields, rowsToFields, fieldsToRows, describeSync,
  CANONICAL_ALIASES, canonicalKey,
} from './draftSync';
import { freshDraft } from './localDraft';

const draft = () => ({
  ...freshDraft(),
  journey: 'New Property',
  budget: { min: '5000000', max: '8000000' },
  location: ['Andheri', 'Powai'],
  checks: { amenities: 'Checked', parking: 'Pending / unknown' },
  visitChecks: { visit_parking_status: 'Done' },
  nextActions: 'Ask for the OC copy',
  projects: [{ id: 'p1', name: 'Parkside' }],
});

describe('flat field protocol', () => {
  it('round-trips a full website draft without loss', () => {
    const original = draft();
    const fields = toFieldMap(original);
    const restored = fromFieldMap(fields, freshDraft());

    expect(restored.journey).toBe('New Property');
    expect(restored.budget).toMatchObject({ min: '5000000', max: '8000000' });
    expect(restored.location).toEqual(['Andheri', 'Powai']);
    expect(restored.checks).toMatchObject({ amenities: 'Checked', parking: 'Pending / unknown' });
    expect(restored.nextActions).toBe('Ask for the OC copy');
    expect(restored.projects).toEqual([{ id: 'p1', name: 'Parkside' }]);
  });

  it('flattens one level so a single answer edits one row', () => {
    const fields = toFieldMap(draft());
    expect(fields['budget.min']).toBe('5000000');
    expect(fields['checks.amenities']).toBe('Checked');
    // Changing only the min budget must not touch the max budget key.
    expect(fields['budget.max']).toBe('8000000');
  });

  it('stores arrays as JSON, matching how Android stores its shortlist', () => {
    const fields = toFieldMap(draft());
    expect(JSON.parse(fields.location)).toEqual(['Andheri', 'Powai']);
  });

  it('never loses a field it does not recognise', () => {
    const fields = toFieldMap({ ...draft(), futureNested: { a: { b: 'c' } } });
    const restored = fromFieldMap(fields, freshDraft());
    expect(restored.futureNested).toEqual({ a: { b: 'c' } });
  });

  it('survives a corrupt envelope instead of discarding the draft', () => {
    const fields = toFieldMap(draft());
    fields.web_envelope = '{not json';
    const restored = fromFieldMap(fields, freshDraft());
    expect(restored.nextActions).toBe('Ask for the OC copy');
  });

  it('tolerates a partial field set from another device', () => {
    const restored = fromFieldMap({ 'budget.min': '100' }, freshDraft());
    expect(restored.budget.min).toBe('100');
    expect(restored.journey).toBe('New Property');
  });

  it('rejects a non-object draft rather than silently producing junk', () => {
    expect(() => toFieldMap(null)).toThrow(TypeError);
    expect(() => toFieldMap([])).toThrow(TypeError);
  });

  it('maps the journey label to the database journey type', () => {
    expect(fieldKeyFor('New Property')).toBe(JOURNEY_TYPES.NEW);
    expect(fieldKeyFor('Resale Property')).toBe(JOURNEY_TYPES.RESALE);
  });
});

describe('per-field last-write-wins', () => {
  it('keeps the newer value and reports which side won', () => {
    expect(resolveField({ local: 'a', localAt: 200, remote: 'b', remoteAt: 100 })).toBe('local');
    expect(resolveField({ local: 'a', localAt: 100, remote: 'b', remoteAt: 200 })).toBe('remote');
    expect(resolveField({ local: 'a', localAt: 100, remote: 'a', remoteAt: 50 })).toBe('equal');
  });

  it('gives an unparseable timestamp to the other side instead of inventing order', () => {
    expect(resolveField({ local: 'a', localAt: null, remote: 'b', remoteAt: 1 })).toBe('remote');
    expect(resolveField({ local: 'a', localAt: 'nonsense', remote: 'b', remoteAt: 1 })).toBe('remote');
  });

  it('uploads only the fields this device actually changed', () => {
    const { merged, push } = mergeFields({
      local: { budget: '9000', parking: 'mine' },
      localAt: 500,
      remote: { budget: '9000', parking: 'theirs', lift: 'theirs' },
      remoteAt: { budget: 100, parking: 100, lift: 100 },
    });
    expect(merged).toEqual({ budget: '9000', parking: 'mine', lift: 'theirs' });
    expect(push).toEqual({ parking: 'mine' });
  });

  it('never pushes a field the remote copy already has', () => {
    const { push } = mergeFields({
      local: { a: 'x' }, localAt: 1,
      remote: { a: 'x', b: 'y' }, remoteAt: { a: 2, b: 2 },
    });
    expect(push).toEqual({});
  });

  it('pushes a local key that remote does not have yet', () => {
    const { merged, push } = mergeFields({ local: { only: 'here' }, localAt: 9, remote: {}, remoteAt: {} });
    expect(merged).toEqual({ only: 'here' });
    expect(push).toEqual({ only: 'here' });
  });
});

describe('Supabase row mapping', () => {
  it('reads rows into a field map with timestamps', () => {
    const { fieldMap, updatedAt } = rowsToFields([
      { field_key: 'budget.min', value: '1', client_updated_at: '2026-01-01T00:00:00Z' },
      { field_key: 'budget.max', value: '2', updated_at: '2026-01-02T00:00:00Z' },
      { field_key: 'broken' },
    ]);
    expect(fieldMap).toEqual({ 'budget.min': '1', 'budget.max': '2', broken: '' });
    expect(updatedAt['budget.min']).toBe('2026-01-01T00:00:00Z');
    expect(updatedAt['budget.max']).toBe('2026-01-02T00:00:00Z');
  });

  it('drops rows the database would reject instead of failing the whole write', () => {
    const rows = fieldsToRows('u1', JOURNEY_TYPES.NEW, {
      ok: 'fine',
      ['k'.repeat(121)]: 'too long a key',
      big: 'x'.repeat(20001),
    }, '2026-01-01T00:00:00Z');
    expect(rows).toHaveLength(1);
    expect(rows[0]).toMatchObject({ user_id: 'u1', journey_type: 'new_property', field_key: 'ok', value: 'fine' });
  });

  it('refuses an unknown journey so data cannot land in the wrong table scope', () => {
    expect(() => fieldsToRows('u1', 'nonsense', { a: 'b' })).toThrow(TypeError);
    expect(() => fieldsToRows(null, JOURNEY_TYPES.NEW, { a: 'b' })).toThrow(TypeError);
  });
});

describe('sync messaging', () => {
  it('never claims cross-device saving while signed out', () => {
    expect(describeSync('signed-out')).toMatch(/this browser only/i);
  });

  it('tells the user when data is only on the device', () => {
    expect(describeSync('offline')).toMatch(/on this device/i);
    expect(describeSync('error')).toMatch(/on this device/i);
  });

  it('confirms when the account copy is current', () => {
    expect(describeSync('synced')).toMatch(/other devices/i);
  });
});

describe('cross-platform key agreement', () => {
  // The website nests its budget; Android saves flat keys. Before this map existed
  // a down payment typed on the phone landed under `down_payment` while the laptop
  // looked for `budget.down`, found nothing and silently showed a blank box.
  it('reads a row written by the old Android spelling', () => {
    const draft = fromFieldMap({ down_payment: '2000000' }, () => ({ budget: {} }));
    expect(draft.budget.down).toBe('2000000');
  });

  it('maps every Android budget key to a website budget key', () => {
    const androidKeys = Object.values(CANONICAL_ALIASES);
    for (const key of androidKeys) expect(canonicalKey(key)).toBeTruthy();
    // Every alias must point at a website path, and point somewhere unique.
    expect(new Set(Object.values(CANONICAL_ALIASES)).size).toBe(androidKeys.length);
  });

  it('leaves unknown keys alone rather than dropping them', () => {
    expect(canonicalKey('some_future_field')).toBe('some_future_field');
    const draft = fromFieldMap({ some_future_field: 'kept' }, () => ({}));
    expect(draft.some_future_field).toBe('kept');
  });

  it('keeps a field stable when it comes back in canonical form', () => {
    const once = fromFieldMap({ 'budget.down': '3000000' }, () => ({ budget: {} }));
    expect(once.budget.down).toBe('3000000');
    const twice = fromFieldMap(toFieldMap(once), () => ({ budget: {} }));
    expect(twice.budget.down).toBe('3000000');
  });

  it('agrees with the Android map in DraftSync.kt', async () => {
    // The two maps are maintained by hand in different languages, so this reads the
    // Kotlin source and fails if one side gains a line the other does not have.
    const fs = await import('node:fs');
    const path = await import('node:path');
    const kotlin = fs.readFileSync(
      path.resolve(process.cwd(), '../android/app/src/main/java/com/finnest/app/DraftSync.kt'),
      'utf8',
    );
    const block = kotlin.slice(kotlin.indexOf('CANONICAL_ALIASES'), kotlin.indexOf('fun toAccountKey'));
    const pairs = [...block.matchAll(/"([\w.]+)" to "([\w.]+)"/g)].map(([, a, b]) => [a, b]);
    expect(Object.fromEntries(pairs)).toEqual(CANONICAL_ALIASES);
  });
});
