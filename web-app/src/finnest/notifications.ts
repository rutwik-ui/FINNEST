import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import type { Session } from '@supabase/supabase-js';
import { supabase } from './supabase';
import { roadmapFor } from './roadmap';
import { nextJourneyAction } from './journeyActions';
// @ts-expect-error untyped JS module
import { STEP_KEYS as RAW_STEPS } from './cloudSync';
const STEP_KEYS = RAW_STEPS as Record<'new' | 'resale', [string, string[]][]>;
import type { Announcement } from './appSettings';

export type NotificationKind = 'reminders' | 'nudges' | 'security' | 'announcements';
export type NotificationPrefs = Record<NotificationKind, boolean> & { browser: boolean };
export type FeedItem = { id: string; kind: NotificationKind; title: string; body: string; time: number; path: string; section?: string };
type SecurityEvent = { id: string; title: string; body: string; time: number };
type Reminder = { id?: string; title?: string; date?: string; note?: string };

export const DEFAULT_PREFS: NotificationPrefs = { reminders: true, nudges: true, security: true, announcements: true, browser: false };
const META_KEY = 'finnest_notifications';
const DAY = 86_400_000;

const store = {
  read<T>(key: string, fallback: T): T { try { const v = localStorage.getItem(key); return v ? JSON.parse(v) as T : fallback; } catch { return fallback; } },
  write(key: string, value: unknown) { try { localStorage.setItem(key, JSON.stringify(value)); } catch { /* storage full or blocked */ } },
};
const scopeOf = (session: Session | null) => session?.user?.id ?? 'guest';

function cleanPrefs(value: unknown): NotificationPrefs {
  const v = (value && typeof value === 'object' ? value : {}) as Partial<Record<keyof NotificationPrefs, unknown>>;
  const pick = (k: keyof NotificationPrefs) => typeof v[k] === 'boolean' ? v[k] as boolean : DEFAULT_PREFS[k];
  return { reminders: pick('reminders'), nudges: pick('nudges'), security: pick('security'), announcements: pick('announcements'), browser: pick('browser') };
}

function deviceName() {
  const ua = navigator.userAgent;
  const browser = /Edg\//.test(ua) ? 'Edge' : /Chrome\//.test(ua) ? 'Chrome' : /Firefox\//.test(ua) ? 'Firefox' : /Safari\//.test(ua) ? 'Safari' : 'a browser';
  const os = /Android/.test(ua) ? 'Android' : /iPhone|iPad/.test(ua) ? 'iOS' : /Windows/.test(ua) ? 'Windows' : /Mac OS X/.test(ua) ? 'macOS' : /Linux/.test(ua) ? 'Linux' : 'this device';
  return `${browser} on ${os}`;
}

function localDay(date: string) {
  const [y, m, d] = date.split('-').map(Number);
  return y && m && d ? new Date(y, m - 1, d, 9, 0, 0).getTime() : NaN;
}
function weekStart(now: number) {
  const d = new Date(now); const day = (d.getDay() + 6) % 7;
  return new Date(d.getFullYear(), d.getMonth(), d.getDate() - day, 10, 0, 0).getTime();
}

export function buildFeed({ data, announcements, security, prefs, signedIn, now }: { data: Record<string, unknown>; announcements: Announcement[]; security: SecurityEvent[]; prefs: NotificationPrefs; signedIn: boolean; now: number }): FeedItem[] {
  const items: FeedItem[] = [];
  if (prefs.reminders) {
    const today = new Date(now); today.setHours(23, 59, 59, 999);
    for (const r of (Array.isArray(data['reminders']) ? data['reminders'] as Reminder[] : [])) {
      if (!r?.date || !r.title) continue;
      const due = localDay(r.date);
      if (Number.isNaN(due) || due > today.getTime() || now - due > 30 * DAY) continue;
      const overdue = new Date(due).toDateString() !== new Date(now).toDateString();
      items.push({ id: `rem:${r.id ?? r.title}:${r.date}`, kind: 'reminders', title: overdue ? 'Reminder overdue' : 'Reminder due today', body: r.title, time: Math.min(due, now), path: '/reminders' });
    }
  }
  if (prefs.nudges) {
    const journey = String(data['journey'] ?? 'New Property');
    const keys = journey === 'Resale Property' ? STEP_KEYS.resale : STEP_KEYS.new;
    const action = nextJourneyAction(data, roadmapFor(journey), keys);
    if (!/^Review your/.test(action.title)) {
      const start = weekStart(now);
      items.push({ id: `nudge:${journey}:${action.title}:${start}`, kind: 'nudges', title: `Next step: ${action.title}`, body: `Pick up your ${journey} journey where you left off.`, time: Math.min(start, now), path: action.path, ...(action.section ? { section: action.section } : {}) });
    }
  }
  if (prefs.security && signedIn) for (const e of security) items.push({ ...e, kind: 'security', path: '/profile' });
  if (prefs.announcements) for (const a of announcements) {
    const time = Date.parse(a.publishedAt);
    if (!Number.isNaN(time) && time <= now) items.push({ id: `ann:${a.id}`, kind: 'announcements', title: a.title, body: a.body, time, path: '/' });
  }
  return items.sort((a, b) => b.time - a.time).slice(0, 50);
}

async function showSystemNotification(item: FeedItem) {
  const options: NotificationOptions = { body: item.body, icon: '/icon-192.png', badge: '/favicon-32.png', tag: item.id, data: { path: item.path } };
  try {
    const reg = await navigator.serviceWorker?.getRegistration();
    if (reg) { await reg.showNotification(item.title, options); return; }
  } catch { /* fall through */ }
  try { const n = new Notification(item.title, options); n.onclick = () => { window.focus(); n.close(); }; } catch { /* unsupported */ }
}

export function permissionState(): NotificationPermission | 'unsupported' {
  return typeof Notification === 'undefined' ? 'unsupported' : Notification.permission;
}

export function useNotifications({ session, data, announcements }: { session: Session | null; data: Record<string, unknown>; announcements: Announcement[] }) {
  const scope = scopeOf(session);
  const [prefs, setPrefsState] = useState<NotificationPrefs>(DEFAULT_PREFS);
  const [security, setSecurity] = useState<SecurityEvent[]>([]);
  const [read, setRead] = useState<string[]>([]);
  const [now, setNow] = useState(() => Date.now());
  const [permission, setPermission] = useState(permissionState);
  const updatingPrefs = useRef(false);
  const lastUser = useRef<string | null>(null);

  // Load per-account (or guest) state.
  useEffect(() => {
    const local = cleanPrefs(store.read(`finnest.notify.prefs.${scope}`, null));
    setPrefsState(session ? cleanPrefs({ ...local, ...(session.user.user_metadata?.[META_KEY] ?? {}) }) : local);
    setRead(store.read<string[]>(`finnest.notify.read.${scope}`, []));
    const events = store.read<SecurityEvent[]>(`finnest.notify.security.${scope}`, []);
    if (session && !events.some(e => e.id === 'welcome')) {
      const created = Date.parse(session.user.created_at);
      if (!Number.isNaN(created) && Date.now() - created < 7 * DAY) {
        events.unshift({ id: 'welcome', title: 'Welcome to FINNEST', body: 'Your account is ready. Your journey now saves to your account and syncs across devices.', time: created });
        store.write(`finnest.notify.security.${scope}`, events);
      }
    }
    setSecurity(events);
  }, [scope, session]);

  // Record account and security events as they happen in this browser.
  useEffect(() => {
    const { data: { subscription } } = supabase.auth.onAuthStateChange((event, next) => {
      const uid = next?.user?.id ?? null;
      const add = (e: SecurityEvent) => {
        if (!uid) return;
        const key = `finnest.notify.security.${uid}`;
        const list = store.read<SecurityEvent[]>(key, []);
        if (list.some(x => x.id === e.id)) return;
        const updated = [e, ...list].slice(0, 20);
        store.write(key, updated);
        if (uid === lastUser.current || lastUser.current === null) setSecurity(updated);
      };
      const t = Date.now();
      if (event === 'SIGNED_IN' && uid && lastUser.current !== uid) add({ id: `signin:${t}`, title: 'New sign-in', body: `Signed in on ${deviceName()}. If this wasn't you, reset your password.`, time: t });
      if (event === 'USER_UPDATED' && !updatingPrefs.current) add({ id: `updated:${t}`, title: 'Account details updated', body: 'Your email or password was changed. If this wasn’t you, reset your password now.', time: t });
      if (event === 'PASSWORD_RECOVERY') add({ id: `recovery:${t}`, title: 'Password reset requested', body: 'A password reset link was used for your account.', time: t });
      lastUser.current = uid;
    });
    return () => subscription.unsubscribe();
  }, []);

  useEffect(() => { const id = setInterval(() => setNow(Date.now()), 60_000); return () => clearInterval(id); }, []);
  useEffect(() => { const sync = () => setPermission(permissionState()); window.addEventListener('focus', sync); return () => window.removeEventListener('focus', sync); }, []);

  // Replies from the FINNEST team to this account's support requests (sent from the website or the Android app).
  const [replies, setReplies] = useState<FeedItem[]>([]);
  const userId = session?.user?.id;
  useEffect(() => {
    setReplies([]);
    if (!userId) return;
    const load = () => supabase.from('finnest_support_tickets').select('id,subject,admin_note,updated_at').eq('user_id', userId).not('admin_note', 'is', null).order('updated_at', { ascending: false }).limit(10)
      .then(({ data: rows }) => setReplies((rows ?? []).filter(r => r.admin_note).map(r => ({ id: `reply:${r.id}:${r.updated_at}`, kind: 'security' as const, title: 'FINNEST replied to your request', body: `${r.subject}: ${r.admin_note}`.slice(0, 240), time: Date.parse(r.updated_at), path: '/help' }))));
    load();
    const channel = supabase.channel(`notify-support-${userId}-${crypto.randomUUID()}`).on('postgres_changes', { event: 'UPDATE', schema: 'public', table: 'finnest_support_tickets', filter: `user_id=eq.${userId}` }, load).subscribe();
    return () => { supabase.removeChannel(channel); };
  }, [userId]);

  const items = useMemo(() => {
    const feed = buildFeed({ data, announcements, security, prefs, signedIn: Boolean(session), now });
    return prefs.security ? [...feed, ...replies].sort((a, b) => b.time - a.time) : feed;
  }, [data, announcements, security, prefs, session, now, replies]);
  const readSet = useMemo(() => new Set(read), [read]);
  const unread = items.filter(i => !readSet.has(i.id)).length;

  // Show new items as system notifications when the user has allowed it.
  useEffect(() => {
    if (!prefs.browser || permission !== 'granted') return;
    const key = `finnest.notify.shown.${scope}`;
    const shown = new Set(store.read<string[]>(key, []));
    const fresh = items.filter(i => !readSet.has(i.id) && !shown.has(i.id) && Date.now() - i.time < 2 * DAY).slice(0, 3);
    if (!fresh.length) return;
    fresh.forEach(i => { shown.add(i.id); void showSystemNotification(i); });
    store.write(key, [...shown].slice(-300));
  }, [items, prefs.browser, permission, scope, readSet]);

  const markRead = useCallback((ids: string[]) => {
    setRead(prev => { const next = [...new Set([...prev, ...ids])].slice(-300); store.write(`finnest.notify.read.${scope}`, next); return next; });
  }, [scope]);

  const setPrefs = useCallback(async (next: NotificationPrefs) => {
    setPrefsState(next);
    store.write(`finnest.notify.prefs.${scope}`, next);
    if (!session) return { ok: true };
    updatingPrefs.current = true;
    const { error } = await supabase.auth.updateUser({ data: { [META_KEY]: next } });
    setTimeout(() => { updatingPrefs.current = false; }, 1500);
    return { ok: !error };
  }, [scope, session]);

  const requestPermission = useCallback(async () => {
    if (typeof Notification === 'undefined') return 'unsupported' as const;
    const result = await Notification.requestPermission();
    setPermission(result);
    return result;
  }, []);

  return { items, unread, readSet, markRead, prefs, setPrefs, permission, requestPermission };
}
export type NotificationsApi = ReturnType<typeof useNotifications>;
