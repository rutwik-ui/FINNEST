import { createClient } from '@supabase/supabase-js';

// Only a publishable key is safe in browser code; authorization is enforced by RLS.
const url = import.meta.env.VITE_SUPABASE_URL;
const key = import.meta.env.VITE_SUPABASE_PUBLISHABLE_KEY;
export const supabase = url && key ? createClient(url, key) : null;
