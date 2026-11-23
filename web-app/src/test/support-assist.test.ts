import { describe, it, expect } from 'vitest';
import { canUseSupportAssist } from '@/finnest/supportAssist.functions';
describe('support assistant access', () => {
  it('allows support managers without an authenticator requirement', () => expect(canUseSupportAssist(['support_manager'])).toBe(true));
  it('allows super admins without an authenticator requirement', () => expect(canUseSupportAssist(['super_admin'])).toBe(true));
  it('blocks content managers', () => expect(canUseSupportAssist(['content_manager'])).toBe(false));
  it('blocks buyers with no administrator role', () => expect(canUseSupportAssist([])).toBe(false));
});
