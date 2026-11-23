/**
 * React glue for cross-device sync.
 *
 * Design rules, in priority order:
 *   1. Never lose a keystroke. Local storage writes first and always; the cloud
 *      is a mirror, never the source of truth for the open form.
 *   2. Never lie in the UI. The status line says exactly where the data lives.
 *   3. Never fight another device. Only the fields changed here are pushed, and
 *      a remote change for a field this device is not editing is applied.
 */
import { useCallback, useEffect, useRef, useState } from 'react';
import { supabase } from './supabase';
import { toFieldMap, fromFieldMap, describeSync, fieldKeyFor } from './draftSync';
import { fetchFields, pushFields, subscribeToFields } from './draftSyncClient';

/** Debounce so typing does not generate a request per character. */
const PUSH_DELAY_MS = 900;

export function useDraftSync({ session, data, onRemoteDraft, enabled = true }) {
  const [state, setState] = useState(session ? 'loading' : 'signed-out');
  const [lastSyncedAt, setLastSyncedAt] = useState(null);
  const userId = session?.user?.id || null;

  // Refs keep the async callbacks from capturing stale values.
  const dataRef = useRef(data);
  dataRef.current = data;
  const lastPushedRef = useRef({});
  const localAtRef = useRef(new Date().toISOString());
  const timerRef = useRef(null);
  const applyingRemoteRef = useRef(false);
  const journeyRef = useRef(fieldKeyFor(data?.journey));

  /** Push whatever differs from the last successfully uploaded snapshot. */
  const flush = useCallback(async () => {
    if (!enabled || !supabase || !userId) return;
    const journeyType = fieldKeyFor(dataRef.current?.journey);
    let fields;
    try {
      fields = toFieldMap(dataRef.current);
    } catch {
      setState('error');
      return;
    }
    const diff = {};
    let changed = false;
    for (const [key, value] of Object.entries(fields)) {
      if (lastPushedRef.current[key] !== value) { diff[key] = value; changed = true; }
    }
    if (!changed) return;

    setState('syncing');
    try {
      const at = new Date().toISOString();
      await pushFields(supabase, userId, journeyType, diff, at);
      Object.assign(lastPushedRef.current, diff);
      setLastSyncedAt(at);
      setState('synced');
    } catch {
      // Local storage already holds the change; retrying is safe and idempotent.
      setState(typeof navigator !== 'undefined' && navigator.onLine === false ? 'offline' : 'error');
    }
  }, [enabled, userId]);

  /** Load the account copy once per sign-in / journey switch. */
  useEffect(() => {
    if (!enabled || !supabase || !userId) { setState('signed-out'); return; }
    let active = true;
    const journeyType = fieldKeyFor(dataRef.current?.journey);
    journeyRef.current = journeyType;
    setState('loading');
    (async () => {
      try {
        const { fieldMap } = await fetchFields(supabase, userId, journeyType);
        if (!active) return;
        // Seed the push baseline so loading does not re-upload everything.
        lastPushedRef.current = { ...fieldMap };
        if (Object.keys(fieldMap).length) {
          let local;
          try { local = toFieldMap(dataRef.current); } catch { local = {}; }
          const localWins = Object.entries(local).filter(([k, v]) => fieldMap[k] !== v);
          if (localWins.length) {
            // This browser has unsaved guest work; upload it rather than discard it.
            await pushFields(supabase, userId, journeyType, Object.fromEntries(localWins), new Date().toISOString());
            Object.assign(lastPushedRef.current, Object.fromEntries(localWins));
          }
          applyingRemoteRef.current = true;
          onRemoteDraft(fromFieldMap(fieldMap, dataRef.current));
          // Restore any local field the remote copy did not have.
          if (localWins.length) onRemoteDraft(fromFieldMap({ ...fieldMap, ...Object.fromEntries(localWins) }, dataRef.current));
          applyingRemoteRef.current = false;
        }
        setState('synced');
        setLastSyncedAt(new Date().toISOString());
      } catch {
        if (active) setState(typeof navigator !== 'undefined' && navigator.onLine === false ? 'offline' : 'error');
      }
    })();
    return () => { active = false; };
    // onRemoteDraft is intentionally omitted: it is recreated each render.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [enabled, userId]);

  /** Debounced push whenever the draft object changes. */
  useEffect(() => {
    if (!enabled || !supabase || !userId) return;
    if (applyingRemoteRef.current) return;
    localAtRef.current = new Date().toISOString();
    if (timerRef.current) clearTimeout(timerRef.current);
    timerRef.current = setTimeout(() => { flush(); }, PUSH_DELAY_MS);
    return () => { if (timerRef.current) clearTimeout(timerRef.current); };
  }, [data, enabled, userId, flush]);

  /** Apply edits made on another device, without echoing them back. */
  useEffect(() => {
    if (!enabled || !supabase || !userId) return;
    const unsubscribe = subscribeToFields(supabase, userId, row => {
      if (row.journey_type !== journeyRef.current) return;
      let local;
      try { local = toFieldMap(dataRef.current); } catch { return; }
      if (local[row.field_key] === row.value) return; // our own echo
      const next = { ...local };
      if (row.value === '' && row.deleted) delete next[row.field_key];
      else next[row.field_key] = row.value;
      lastPushedRef.current[row.field_key] = row.value;
      applyingRemoteRef.current = true;
      onRemoteDraft(fromFieldMap(next, dataRef.current));
      applyingRemoteRef.current = false;
      setLastSyncedAt(new Date().toISOString());
      setState('synced');
    });
    return unsubscribe;
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [enabled, userId]);

  /** Flush immediately when the tab is hidden, so closing the laptop is safe. */
  useEffect(() => {
    if (!enabled || !userId) return;
    const onHide = () => { if (typeof document !== 'undefined' && document.visibilityState === 'hidden') flush(); };
    document.addEventListener('visibilitychange', onHide);
    return () => document.removeEventListener('visibilitychange', onHide);
  }, [enabled, userId, flush]);

  useEffect(() => {
    const onOnline = () => flush();
    const onOffline = () => setState('offline');
    window.addEventListener('online', onOnline);
    window.addEventListener('offline', onOffline);
    return () => { window.removeEventListener('online', onOnline); window.removeEventListener('offline', onOffline); };
  }, [flush]);

  const syncNow = useCallback(async () => { await flush(); }, [flush]);

  return {
    state,
    status: describeSync(state),
    lastSyncedAt,
    syncNow,
    /** Exposed for the Profile screen's "is this on my account?" answer. */
    isSyncing: state === 'syncing' || state === 'loading',
    isBackedUp: state === 'synced',
  };
}
