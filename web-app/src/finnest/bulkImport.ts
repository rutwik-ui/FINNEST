import { z } from 'zod';

export const IMPORT_MAX_ROWS = 500;
export const IMPORT_MAX_BYTES = 5 * 1024 * 1024;
export const EXPERT_SPECIALTIES = ['Property lawyer', 'CA / tax advisor', 'Home loan advisor', 'Property consultant', 'Architect / technical inspector', 'Interior expert', 'Valuation expert'] as const;
export const importKindSchema = z.enum(['guidance', 'experts']);
export type ImportKind = z.infer<typeof importKindSchema>;
const status = z.enum(['draft', 'published']).default('draft');
const optionalText = (max: number) => z.string().trim().max(max).default('');
export const guidanceRowSchema = z.object({
  id: z.string().uuid(),
  title: z.string().trim().min(1, 'Title is required').max(150),
  body: z.string().trim().min(1, 'Guidance is required').max(10000),
  category: z.enum(['general', 'new_property', 'resale_property']).default('general'),
  status,
}).strict();
export const expertRowSchema = z.object({
  id: z.string().uuid(),
  name: z.string().trim().min(1, 'Name is required').max(120),
  specialty: z.enum(EXPERT_SPECIALTIES),
  city: optionalText(120),
  phone: optionalText(40).refine(v => !v || /^[+\d\s().-]+$/.test(v), 'Use a valid phone number'),
  email: optionalText(255).refine(v => !v || z.string().email().safeParse(v).success, 'Use a valid email address'),
  bio: optionalText(5000),
  status,
}).strict();
export const importRequestSchema = z.discriminatedUnion('kind', [
  z.object({ kind: z.literal('guidance'), rows: z.array(guidanceRowSchema).min(1).max(IMPORT_MAX_ROWS) }).strict(),
  z.object({ kind: z.literal('experts'), rows: z.array(expertRowSchema).min(1).max(IMPORT_MAX_ROWS) }).strict(),
]).superRefine((request, ctx) => {
  if (new Set(request.rows.map(row => row.id)).size !== request.rows.length) {
    ctx.addIssue({ code: z.ZodIssueCode.custom, message: 'Duplicate import IDs' });
  }
});

export type ImportRequest = z.infer<typeof importRequestSchema>;
export const IMPORT_FIELDS: Record<ImportKind, { key: string; label: string; required?: boolean }[]> = {
  guidance: [{ key: 'title', label: 'Title', required: true }, { key: 'body', label: 'Guidance', required: true }, { key: 'category', label: 'Category' }, { key: 'status', label: 'Status' }],
  experts: [{ key: 'name', label: 'Name', required: true }, { key: 'specialty', label: 'Speciality', required: true }, { key: 'city', label: 'City' }, { key: 'phone', label: 'Phone' }, { key: 'email', label: 'Email' }, { key: 'bio', label: 'Bio' }, { key: 'status', label: 'Status' }],
};

export function canImport(roles: string[], aal: unknown, kind: ImportKind) {
  return aal === 'aal2' && roles.some(role => role === 'super_admin' || role === (kind === 'guidance' ? 'content_manager' : 'expert_manager'));
}

export function checkImportFile(file: { name: string; size: number }) {
  if (!/\.(csv|xlsx)$/i.test(file.name)) throw new Error('Choose a CSV or Excel (.xlsx) file. Save older .xls files as .xlsx first.');
  if (file.size > IMPORT_MAX_BYTES) throw new Error('Choose a file smaller than 5 MB.');
  if (file.size === 0) throw new Error('The file is empty.');
}

export type ImportSheet = { name: string; data: unknown[][] };
export function sheetTable(data: unknown[][]) {
  if (data.length > IMPORT_MAX_ROWS + 1) throw new Error('Import up to 500 rows at a time. Split this file into smaller files.');
  const [header, ...body] = data;
  if (!header?.length) throw new Error('The worksheet is empty.');
  const headers = header.map((v, i) => String(v ?? '').trim() || `Column ${i + 1}`);
  const rows = body.map((cells, i) => ({ cells, line: i + 2 })).filter(row => row.cells.some(v => v !== null && v !== undefined && String(v).trim() !== ''));
  if (!rows.length) throw new Error('No data rows found below the column headings.');
  return { headers, rows };
}
const normalize = (v: string) => v.toLowerCase().replace(/[\s_-]+/g, '');
export function suggestMapping(kind: ImportKind, headers: string[]) {
  return Object.fromEntries(IMPORT_FIELDS[kind].map(field => [field.key, String(headers.findIndex(h => normalize(h) === normalize(field.key) || normalize(h) === normalize(field.label) || field.key === 'body' && normalize(h) === 'content'))]));
}

export function previewImport(kind: ImportKind, table: ReturnType<typeof sheetTable>, mapping: Record<string, string>, ids: string[]) {
  const schema = kind === 'guidance' ? guidanceRowSchema : expertRowSchema;
  const seen = new Set<string>();
  return table.rows.map((row, index) => {
    const raw: Record<string, unknown> = { id: ids[index] };
    for (const field of IMPORT_FIELDS[kind]) {
      const column = Number(mapping[field.key] ?? -1);
      const value = column >= 0 ? row.cells[column] : undefined;
      if (value !== undefined && value !== null && String(value).trim()) {
        raw[field.key] = String(value).trim();
        if (field.key === 'status') raw[field.key] = String(value).trim().toLowerCase();
        if (field.key === 'category') raw[field.key] = String(value).trim().toLowerCase().replace(/[\s-]+/g, '_');
      }
    }
    const result = schema.safeParse(raw);
    const errors = result.success ? [] : result.error.issues.map(issue => `${issue.path.join('.')}: ${issue.message}`);
    const fingerprint = JSON.stringify(Object.fromEntries(Object.entries(raw).filter(([key]) => key !== 'id')));
    if (seen.has(fingerprint)) errors.push('Duplicate row in this file. Remove the duplicate before importing.');
    seen.add(fingerprint);
    return { line: row.line, raw, data: result.success ? result.data : null, errors };
  });
}

export function templateCsv(kind: ImportKind) {
  return IMPORT_FIELDS[kind].map(field => field.key).join(',') + '\r\n';
}