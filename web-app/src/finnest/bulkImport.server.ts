import type { SupabaseClient } from '@supabase/supabase-js';
import type { Database } from '@/integrations/supabase/types';
import { importRequestSchema, type ImportRequest } from './bulkImport';

// One statement per file is atomic. Stable preview IDs make a retry safe without changing existing records.
export async function persistImport(client: SupabaseClient<Database>, input: ImportRequest) {
  const request = importRequestSchema.parse(input);
  const ids = request.rows.map(row => row.id);
  const table = request.kind === 'guidance' ? 'finnest_guidance' : 'finnest_experts';
  const write = request.kind === 'guidance'
    ? await client.from('finnest_guidance').upsert(request.rows, { onConflict: 'id', ignoreDuplicates: true })
    : await client.from('finnest_experts').upsert(request.rows, { onConflict: 'id', ignoreDuplicates: true });
  if (write.error) throw new Error('Import was not confirmed. Keep this preview and retry; existing records will not be overwritten.');
  const { data, error } = await client.from(table).select('*').in('id', ids);
  if (error || !data || data.length !== request.rows.length) throw new Error('Could not verify every saved row. Keep this preview and retry safely.');
  const confirmed = new Map(data.map(row => [row.id, row]));
  for (const row of request.rows) {
    const saved = confirmed.get(row.id);
    if (!saved || !Object.entries(row).every(([key, value]) => saved[key as keyof typeof saved] === value)) {
      throw new Error('An import ID already belongs to different data. Nothing was overwritten; choose the file again.');
    }
  }
  return { count: data.length, published: request.rows.filter(row => row.status === 'published').length };
}