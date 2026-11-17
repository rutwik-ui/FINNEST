import { createServerFn } from '@tanstack/react-start';
import { z } from 'zod';
import { requireSupabaseAuth } from '@/integrations/supabase/auth-middleware';
// @ts-expect-error untyped JS module
import { STEP_KEYS as RAW_STEPS } from './cloudSync';
const STEP_KEYS = RAW_STEPS as Record<'new' | 'resale', [string, string[]][]>;

const SUPPORT_ROLES = ['super_admin', 'support_manager'];
export const canUseSupportAssist = (roles: string[]) => roles.some(r => SUPPORT_ROLES.includes(r));

const input = z.object({ userId: z.string().uuid(), notes: z.string().max(4000).default('') });
const schema = {
  type: 'object', additionalProperties: false, required: ['summary', 'blockers', 'next_action', 'message_to_buyer'],
  properties: {
    summary: { type: 'string' },
    blockers: { type: 'array', items: { type: 'object', additionalProperties: false, required: ['issue', 'step', 'severity'], properties: { issue: { type: 'string' }, step: { type: 'string' }, severity: { type: 'string', enum: ['high', 'medium', 'low'] } } } },
    next_action: { type: 'string' },
    message_to_buyer: { type: 'string' },
  },
};

export const supportAssist = createServerFn({ method: 'POST' })
  .middleware([requireSupabaseAuth])
  .inputValidator(v => input.parse(v))
  .handler(async ({ data, context }) => {
    const { data: roleRows, error: roleError } = await context.supabase.from('finnest_admin_roles').select('role').eq('user_id', context.userId);
    if (roleError || !canUseSupportAssist((roleRows ?? []).map(r => r.role))) throw new Error('Support assistant needs a support administrator role.');
    const { data: rows, error } = await (context.supabase as any).from('finnest_journeys').select('journey_type,payload,completed_steps,current_step,updated_at').eq('user_id', data.userId);
    if (error) throw new Error('Could not read this buyer’s progress.');
    const progress = (['new', 'resale'] as const).map(t => {
      const r = (rows ?? []).find((x: any) => x.journey_type === t);
      if (!r) return { journey: t, started: false };
      return { journey: t, started: true, last_saved: r.updated_at, last_screen: r.current_step,
        steps: STEP_KEYS[t].map(([label, keys]) => ({ step: label, status: r.completed_steps.includes(label) ? 'completed' : 'pending', answers: Object.fromEntries(keys.map((k: string) => [k, r.payload?.[k]]).filter(([, v]: unknown[]) => v != null)) })) };
    });
    const key = process.env['LOVABLE_API_KEY'];
    if (!key) throw new Error('AI is not configured.');
    const res = await fetch('https://ai.gateway.lovable.dev/v1/responses', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${key}`, 'X-Lovable-AIG-SDK': 'fetch' },
      body: JSON.stringify({
        model: 'openai/gpt-6-astra', stream: true, store: false, reasoning: { effort: 'low' },
        instructions: 'You assist FINNEST support staff helping Indian home buyers (new and resale property). From the buyer progress JSON and staff notes, identify concrete blockers (missing steps, risky or incomplete answers, legal/financial gaps) and recommend ONE clear next action for staff. Offer guidance, never claim verification. Be concise. Write message_to_buyer as a short, friendly note staff could send.',
        input: [{ role: 'user', content: `Buyer progress:\n${JSON.stringify(progress).slice(0, 30000)}\n\nSupport notes:\n${data.notes || '(none)'}` }],
        text: { format: { type: 'json_schema', name: 'support_assist', strict: true, schema } },
      }),
    });
    if (!res.ok || !res.body) {
      const msg = res.status === 402 ? 'AI credits are used up. Add credits in Settings → Plans & credits.' : res.status === 429 ? 'AI is busy. Please try again in a minute.' : `AI request failed (${res.status}).`;
      throw new Error(msg);
    }
    const reader = res.body.getReader(), dec = new TextDecoder();
    let buf = '', text = '', refusal = '';
    for (;;) {
      const { done, value } = await reader.read();
      if (done) break;
      buf += dec.decode(value, { stream: true });
      let i; while ((i = buf.indexOf('\n')) >= 0) {
        const line = buf.slice(0, i).trim(); buf = buf.slice(i + 1);
        if (!line.startsWith('data:')) continue;
        const p = line.slice(5).trim(); if (!p || p === '[DONE]') continue;
        try { const ev = JSON.parse(p); if (ev.type === 'response.output_text.delta') text += ev.delta; if (ev.type === 'response.refusal.delta') refusal += ev.delta; if (ev.type === 'response.failed' || ev.type === 'error') throw new Error('AI could not complete this review.'); } catch (e) { if (e instanceof Error && e.message.startsWith('AI')) throw e; }
      }
    }
    if (refusal || !text) throw new Error('AI declined or returned nothing for this review.');
    await (context.supabase as any).from('finnest_activity').insert({ user_id: context.userId, event: 'support_ai_review', detail: { buyer: data.userId }, platform: 'web' }).then(() => {}, () => {});
    return JSON.parse(text) as { summary: string; blockers: { issue: string; step: string; severity: string }[]; next_action: string; message_to_buyer: string };
  });
