import Dexie, { type Table } from 'dexie';

export interface PublicCache { key: string; value: unknown; savedAt: number }
export interface JourneyCache { key: string; userId: string; type: 'new' | 'resale'; payload: Record<string, unknown>; base: Record<string, unknown>; version: number; pending: boolean }
class FinnestOfflineStore extends Dexie {
  declare publicContent: Table<PublicCache, string>;
  declare journeys: Table<JourneyCache, string>;
  declare preferences: Table<{ key: string; value: unknown }, string>;
  constructor() {
    super('finnest-offline-v1');
    this.version(1).stores({ publicContent: 'key,savedAt', journeys: 'key,userId,pending', preferences: 'key' });
  }
}
let store: FinnestOfflineStore | undefined;
export function offlineStore() { store ??= new FinnestOfflineStore(); return store; }
export async function readPublicCache<T>(key: string): Promise<T | undefined> {
  try { return (await offlineStore().publicContent.get(key))?.value as T | undefined; } catch { return undefined; }
}
export async function writePublicCache(key: string, value: unknown) {
  try { await offlineStore().publicContent.put({ key, value, savedAt: Date.now() }); } catch { /* Network results remain usable without a cache. */ }
}

export function sameJourney(a: unknown, b: unknown): boolean {
  if (a === b) return true;
  if (a === null || b === null || typeof a !== 'object' || typeof b !== 'object') return false;
  if (Array.isArray(a) || Array.isArray(b)) return Array.isArray(a) && Array.isArray(b) && a.length === b.length && a.every((v,i) => sameJourney(v,b[i]));
  const left = a as Record<string, unknown>, right = b as Record<string, unknown>;
  const keys = Object.keys(left);
  return keys.length === Object.keys(right).length && keys.every(k => Object.hasOwn(right,k) && sameJourney(left[k],right[k]));
}

// Three-way merge: untouched fields follow the server; local edits (including deletes) win conflicts.
export function mergeJourney(base: Record<string, unknown>, mine: Record<string, unknown>, remote: Record<string, unknown>): Record<string, unknown> {
  const merged = { ...remote };
  const object = (v: unknown): v is Record<string, unknown> => Boolean(v) && typeof v === 'object' && !Array.isArray(v);
  for (const key of new Set([...Object.keys(base), ...Object.keys(mine)])) {
    if (sameJourney(mine[key],base[key])) continue;
    if (!Object.hasOwn(mine, key)) { delete merged[key]; continue; }
    merged[key] = object(mine[key]) && object(remote[key])
      ? mergeJourney(object(base[key]) ? base[key] : {}, mine[key], remote[key]) : mine[key];
  }
  return merged;
}

export const accountDraftKey = (userId?: string) => userId ? `finnest-account-draft:${userId}` : 'finnest-web-v1';