/**
 * Supabase transport for cross-device draft sync.
 *
 * Reads and writes finnest_draft_fields (one row per answer). Every write is an
 * upsert keyed by (user_id, journey_type, field_key), so two devices editing
 * different answers never clobber each other, and the database's merge trigger
 * refuses a stale write.
 *
 * Real-time: a postgres_changes subscription on the user's own rows means an
 * edit on the phone arrives on the laptop without a refresh. Realtime RLS is
 * enforced server-side, so a client only ever receives its own rows.
 */
import { mergeFields, rowsToFields, fieldsToRows, fieldKeyFor } from './draftSync';

const TABLE = 'finnest_draft_fields';

/** Fetch every stored answer for one journey. */
export async function fetchFields(supabase, userId, journeyType) {
  if (!supabase) throw new Error('Supabase is not configured');
  if (!userId) throw new Error('Not signed in');
  const { data, error } = await supabase
    .from(TABLE)
    .select('field_key,value,client_updated_at,updated_at')
    .eq('user_id', userId)
    .eq('journey_type', journeyType)
    .limit(1000);
  if (error) throw new Error(error.message || 'Could not load your journey');
  return rowsToFields(data);
}

/** Upsert only the given field values. Safe to call with an empty map. */
export async function pushFields(supabase, userId, journeyType, fieldMap, at = new Date().toISOString()) {
  if (!supabase) throw new Error('Supabase is not configured');
  if (!userId) throw new Error('Not signed in');
  const rows = fieldsToRows(userId, journeyType, fieldMap, at);
  if (!rows.length) return { written: 0 };
  const { error } = await supabase.from(TABLE).upsert(rows, {
    onConflict: 'user_id,journey_type,field_key',
    ignoreDuplicates: false,
  });
  if (error) throw new Error(error.message || 'Could not save your journey');
  return { written: rows.length };
}

/**
 * One full reconciliation: pull, merge per field, push only what is newer here.
 * Returns the merged field map so the caller can render it immediately.
 */
export async function reconcile(supabase, { userId, journeyType, localFields, localAt, onPush }) {
  const remote = await fetchFields(supabase, userId, journeyType);
  const { merged, push } = mergeFields({
    local: localFields,
    localAt,
    remote: remote.fieldMap,
    remoteAt: remote.updatedAt,
  });
  // Push before resolving so a failed upload is reported, not silently dropped.
  if (onPush) await onPush(push);
  if (Object.keys(push).length) await pushFields(supabase, userId, journeyType, push, localAt);
  return merged;
}

/**
 * Subscribe to this user's own field changes.
 *
 * `onChange(rows)` receives the raw changed rows. It is the caller's job to
 * decide whether a change came from this tab (ignore it) or another device.
 * Returns an unsubscribe function; safe to call twice.
 */
export function subscribeToFields(supabase, userId, onChange) {
  if (!supabase || !userId) return () => {};
  const channel = supabase
    .channel(`finnest-drafts-${userId}`)
    .on(
      'postgres_changes',
      { event: '*', schema: 'public', table: TABLE, filter: `user_id=eq.${userId}` },
      payload => {
        const row = payload.new && Object.keys(payload.new).length ? payload.new : payload.old;
        if (row && row.field_key) onChange(row, payload.eventType);
      },
    )
    .subscribe();
  let stopped = false;
  return () => {
    if (stopped) return;
    stopped = true;
    try { supabase.removeChannel(channel); } catch { /* already closed */ }
  };
}

/** Journey type for a draft, from the draft's own journey label. */
export const journeyTypeOf = draft => fieldKeyFor(draft?.journey);

/**
 * Device identity for diagnosing sync. Not a security boundary — the row is
 * already owned by user_id and protected by RLS.
 */
export function deviceId(storage = typeof window !== 'undefined' ? window.localStorage : null) {
  const KEY = 'finnest-device-id';
  try {
    const existing = storage?.getItem(KEY);
    if (existing && /^[0-9a-f-]{36}$/i.test(existing)) return existing;
    const id = typeof crypto !== 'undefined' && crypto.randomUUID
      ? crypto.randomUUID()
      : 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, c => {
        const r = (Math.random() * 16) | 0;
        return (c === 'x' ? r : (r & 0x3) | 0x8).toString(16);
      });
    storage?.setItem(KEY, id);
    return id;
  } catch {
    return 'unknown-device';
  }
}
