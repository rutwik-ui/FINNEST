import { createServerFn } from '@tanstack/react-start';
import { requireSupabaseAuth } from '@/integrations/supabase/auth-middleware';
import { canImport, importRequestSchema } from './bulkImport';
import { persistImport } from './bulkImport.server';

export const importAdminRows = createServerFn({ method: 'POST' })
  .middleware([requireSupabaseAuth])
  .inputValidator(input => importRequestSchema.parse(input))
  .handler(async ({ data, context }) => {
    const { data: roleRows, error } = await context.supabase.from('finnest_admin_roles').select('role').eq('user_id', context.userId);
    if (error || !canImport((roleRows ?? []).map(row => row.role), context.claims['aal'], data.kind)) {
      throw new Error('This import needs the correct administrator role and authenticator verification.');
    }
    return persistImport(context.supabase, data);
  });