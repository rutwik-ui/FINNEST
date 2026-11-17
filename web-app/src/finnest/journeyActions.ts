export interface JourneyAction { title: string; path: string; section?: string }
export function nextJourneyAction(data: Record<string, unknown>, steps: { label: string; path: string; section?: string }[], keys: [string,string[]][]): JourneyAction {
  const has = (v: unknown): boolean => v == null ? false : typeof v === 'string' ? v.trim() !== '' : Array.isArray(v) ? v.length > 0 : typeof v === 'object' ? Object.values(v).some(has) : Boolean(v);
  const index = keys.findIndex(([, fields]) => !fields.some(k => has(data[k])));
  const step = steps[index];
  return step ? { title: step.label, path: step.path, ...(step.section ? { section: step.section } : {}) } : { title: 'Review your questions and next actions', path: data['journey'] === 'Resale Property' ? '/resale-summary' : '/summary' };
}