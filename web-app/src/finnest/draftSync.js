/**
 * Cross-device draft sync.
 *
 * The website stores a journey draft as one nested object. Android stores the
 * same journey as flat `Map<String, String>` field keys scoped by journey. Those
 * are incompatible, so "sync" used to be impossible.
 *
 * This module defines the shared protocol:
 *   flat string key  ->  string value, plus a per-field timestamp
 * and converts the website's nested draft to and from that shape. The mapping is
 * loss-free for the fields both apps share, and unknown website fields survive
 * round-trips in an envelope field so a future field is never silently dropped.
 *
 * Everything here is pure: no Supabase, no network, no browser APIs. That keeps
 * it unit-testable and identical in behaviour to the Android implementation.
 */

/** Journey values used by the website's select-journey screen. */
export const JOURNEY_TYPES = { NEW: 'new_property', RESALE: 'resale_property' };
export const JOURNEY_LABELS = { new_property: 'New Property', resale_property: 'Resale Property' };

/** Reserved key holding website-only fields Android does not render. */
const ENVELOPE_KEY = 'web_envelope';
/** Reserved key holding the website's selected journey label. */
const JOURNEY_KEY = 'selected_journey';
/** Supabase limit from the migration. */
export const MAX_FIELD_KEY = 120;

/**
 * Canonical key registry for the answers BOTH apps render.
 *
 * The website keeps its budget inside a nested object (`budget.down`), while
 * Android saves the same box as a flat key (`down_payment`). Both were valid
 * inside their own app and useless to the other: a down payment typed on the
 * phone landed under `down_payment`, the laptop looked for `budget.down`, found
 * nothing, and showed a blank box. Nothing errored, so nobody noticed.
 *
 * The map below is the single source of truth. The database stores the left-hand
 * (web) key; Android translates at its own boundary in `DraftSync.kt`; the web
 * already writes that shape, so this file needs no rewrite of existing rows.
 *
 * Adding a question that exists on both platforms means adding one line here and
 * one line in `DraftSync.kt`. `draftSync.test.js` fails if one side drifts.
 */
export const CANONICAL_ALIASES = {
  // New property -> budget
  'budget.loan': 'loan',
  'budget.down': 'down_payment',
  'budget.stamp': 'stamp_duty',
  'budget.gst': 'gst',
  'budget.brokerage': 'brokerage',
  'budget.interiors': 'interiors',
  'budget.contingency': 'contingency',
  'budget.emi': 'emi',
  'budget.maintenance': 'maintenance',
  'budget.carpet': 'carpet_area',
  'budget.access': 'accessibility_needs',
};

export const MAX_FIELD_VALUE = 20000;

export const fieldKeyFor = (journeyLabel) =>
  journeyLabel === 'Resale Property' ? JOURNEY_TYPES.RESALE : JOURNEY_TYPES.NEW;

/**
 * Nested website draft -> flat field map.
 *
 * Group keys become a single JSON string (Android already stores lists this way,
 * e.g. `shortlist_projects_v1` is a JSON array). Scalars become plain strings.
 * Nested objects are flattened one level (`budget.min` -> `budget.min`) so a
 * single answer change does not rewrite a whole object.
 */
export function toFieldMap(draft) {
  if (!draft || typeof draft !== 'object' || Array.isArray(draft)) {
    throw new TypeError('toFieldMap expects a draft object');
  }
  const out = {};
  const envelope = {};

  for (const [key, value] of Object.entries(draft)) {
    if (key === 'journey') {
      out[JOURNEY_KEY] = String(value);
      continue;
    }
    if (value === undefined) continue;

    if (value !== null && typeof value === 'object' && !Array.isArray(value)) {
      const nested = Object.entries(value);
      // A record of scalars is flattened; anything else is stored whole.
      if (nested.every(([, v]) => v === undefined || v === null || typeof v !== 'object')) {
        let any = false;
        for (const [sub, v] of nested) {
          if (v === undefined) continue;
          out[`${key}.${sub}`] = String(v);
          any = true;
        }
        if (!any) envelope[key] = value;
        continue;
      }
      envelope[key] = value;
      continue;
    }

    if (Array.isArray(value)) {
      out[key] = JSON.stringify(value);
      continue;
    }

    out[key] = String(value);
  }

  if (Object.keys(envelope).length) out[ENVELOPE_KEY] = JSON.stringify(envelope);
  return out;
}

/** Flat field map -> nested website draft. Tolerates partial rows. */
/**
 * Accept a row stored under an older/Android spelling and return the canonical key.
 * Unknown keys pass through untouched so nothing is ever dropped.
 */
export function canonicalKey(key) {
  for (const [webKey, androidKey] of Object.entries(CANONICAL_ALIASES)) {
    if (androidKey === key) return webKey;
  }
  return key;
}

export function fromFieldMap(fields, fallbackDraft) {
  const base = typeof fallbackDraft === 'function' ? fallbackDraft() : { ...(fallbackDraft || {}) };
  const out = { ...base };
  let envelope = {};

  for (const [rawKey, raw] of Object.entries(fields || {})) {
    const key = canonicalKey(rawKey);
    if (key === ENVELOPE_KEY) {
      try {
        const parsed = JSON.parse(raw);
        if (parsed && typeof parsed === 'object' && !Array.isArray(parsed)) envelope = parsed;
      } catch { /* a corrupt envelope must not destroy the rest of the draft */ }
      continue;
    }
    if (key === JOURNEY_KEY) { out.journey = raw; continue; }

    const dot = key.indexOf('.');
    if (dot > 0) {
      const parent = key.slice(0, dot);
      const child = key.slice(dot + 1);
      if (!out[parent] || typeof out[parent] !== 'object' || Array.isArray(out[parent])) out[parent] = {};
      out[parent] = { ...out[parent], [child]: raw };
      continue;
    }

    const original = base[key];
    if (Array.isArray(original)) {
      try {
        const parsed = JSON.parse(raw);
        out[key] = Array.isArray(parsed) ? parsed : original;
      } catch { out[key] = original; }
      continue;
    }
    if (original !== undefined && typeof original === 'boolean') {
      out[key] = raw === 'true';
      continue;
    }
    if (original !== undefined && typeof original === 'number' && raw !== '' && Number.isFinite(Number(raw))) {
      out[key] = Number(raw);
      continue;
    }
    out[key] = raw;
  }

  for (const [key, value] of Object.entries(envelope)) {
    if (!(key in out)) out[key] = value;
  }
  return out;
}

/**
 * Resolve one field between local and remote using per-field last-write-wins.
 * Returns 'local' | 'remote' | 'equal'.
 *
 * `localAt` and `remoteAt` are ISO timestamps or milliseconds. A tie is resolved
 * in favour of the remote copy, because the server already accepted it.
 */
export function resolveField({ local, localAt, remote, remoteAt }) {
  if (local === remote) return 'equal';
  const l = toMillis(localAt);
  const r = toMillis(remoteAt);
  if (l === null && r === null) return 'equal';
  if (l === null) return 'remote';
  if (r === null) return 'local';
  return l > r ? 'local' : 'remote';
}

function toMillis(value) {
  if (value === null || value === undefined) return null;
  if (typeof value === 'number' && Number.isFinite(value)) return value;
  const ms = Date.parse(value);
  return Number.isFinite(ms) ? ms : null;
}

/**
 * Merge a remote row set into the local field map.
 * Returns { merged, push } where `push` holds only the fields this device must
 * upload because its copy is newer. Pushing exclusively the diff is what stops
 * two devices from fighting over fields neither of them edited.
 */
export function mergeFields({ local = {}, localAt = null, remote = {}, remoteAt = {} }) {
  const merged = {};
  const push = {};
  const keys = new Set([...Object.keys(local), ...Object.keys(remote)]);

  for (const key of keys) {
    const hasLocal = Object.hasOwn(local, key);
    const hasRemote = Object.hasOwn(remote, key);
    if (hasLocal && !hasRemote) { merged[key] = local[key]; push[key] = local[key]; continue; }
    if (!hasLocal && hasRemote) { merged[key] = remote[key]; continue; }

    const winner = resolveField({
      local: local[key], localAt,
      remote: remote[key], remoteAt: remoteAt[key] ?? null,
    });
    if (winner === 'local') { merged[key] = local[key]; push[key] = local[key]; }
    else merged[key] = remote[key];
  }
  return { merged, push };
}

/** Supabase rows -> { fieldMap, updatedAtByKey } for a single journey. */
export function rowsToFields(rows) {
  const fieldMap = {};
  const updatedAt = {};
  for (const row of rows || []) {
    if (!row || typeof row.field_key !== 'string') continue;
    fieldMap[row.field_key] = typeof row.value === 'string' ? row.value : '';
    updatedAt[row.field_key] = row.client_updated_at || row.updated_at || null;
  }
  return { fieldMap, updatedAt };
}

/** Field map -> upsert rows for one journey. Omits values that cannot be stored. */
export function fieldsToRows(userId, journeyType, fieldMap, at = new Date().toISOString()) {
  if (!userId) throw new TypeError('fieldsToRows needs a user id');
  if (!Object.values(JOURNEY_TYPES).includes(journeyType)) throw new TypeError('unknown journey type');
  const rows = [];
  for (const [key, value] of Object.entries(fieldMap || {})) {
    if (typeof key !== 'string' || key.length === 0 || key.length > MAX_FIELD_KEY) continue;
    const text = value === null || value === undefined ? '' : String(value);
    if (text.length > MAX_FIELD_VALUE) continue;
    rows.push({ user_id: userId, journey_type: journeyType, field_key: key, value: text, client_updated_at: at });
  }
  return rows;
}

/** A short, human status for the UI. Never claims more than actually happened. */
export function describeSync(state) {
  switch (state) {
    case 'signed-out': return 'Saved in this browser only. Sign in to sync across devices.';
    case 'loading': return 'Loading your saved journey from your account…';
    case 'syncing': return 'Saving to your account…';
    case 'synced': return 'Saved to your account. Available on your other devices.';
    case 'offline': return 'Offline. Changes are saved on this device and will sync when you reconnect.';
    case 'error': return 'Could not reach your account. Changes are saved on this device.';
    case 'conflict': return 'Another device changed your journey. Your newer answers were kept.';
    default: return 'Saved in this browser only.';
  }
}
