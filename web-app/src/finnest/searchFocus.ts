/**
 * Hands a search result to the page that shows it. Search sets the target and navigates; the
 * Experts or Guidance page takes it when it mounts, or straight away if that page is already open.
 */
export type FocusKind = 'expert' | 'guide';
export type FocusTarget = { kind: FocusKind; id: string; label: string };

const EVENT = 'finnest-search-focus';
let pending: FocusTarget | null = null;

export function setSearchFocus(target: FocusTarget) {
  pending = target;
  if (typeof window !== 'undefined') window.dispatchEvent(new CustomEvent(EVENT));
}

export function takeSearchFocus(kind: FocusKind): FocusTarget | null {
  if (!pending || pending.kind !== kind) return null;
  const target = pending;
  pending = null;
  return target;
}

/** Calls `apply` with a target for this page, on mount and whenever search picks one while the page is open. */
export function onSearchFocus(kind: FocusKind, apply: (target: FocusTarget) => void): () => void {
  const check = () => { const target = takeSearchFocus(kind); if (target) apply(target); };
  check();
  window.addEventListener(EVENT, check);
  return () => window.removeEventListener(EVENT, check);
}

/** Scrolls an element into view once it renders and briefly highlights it. */
export function revealElement(id: string, attempts = 20) {
  const el = document.getElementById(id);
  if (!el) { if (attempts > 0) window.setTimeout(() => revealElement(id, attempts - 1), 150); return; }
  el.scrollIntoView({ behavior: window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth', block: 'center' });
  el.classList.add('search-focus');
  window.setTimeout(() => el.classList.remove('search-focus'), 2400);
}
