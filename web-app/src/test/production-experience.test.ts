import { describe, expect, it } from 'vitest';
import { mergeJourney, accountDraftKey, sameJourney } from '@/finnest/offlineStore';
import { settingsSchema, defaultSettings, canManageSettings } from '@/finnest/appSettings';
import { credentialsSchema } from '@/finnest/Authentication';
import { refuseServiceWorker } from '@/lib/pwa';
import { nextJourneyAction } from '@/finnest/journeyActions';

describe('FINNEST production rules', () => {
  it('does not resave identical server answers when object keys are reordered', () => {
    expect(sameJourney({ budget: { down: '100', emi: '40' }, location: [] }, { location: [], budget: { emi: '40', down: '100' } })).toBe(true);
    expect(sameJourney({ down: '100' }, { down: '200' })).toBe(false);
  });
  it('recommends the first step without recorded answers', () => {
    expect(nextJourneyAction({ budget: { down: '375000' } }, [{ label: 'Budget', path: '/journey' }, { label: 'Developer', path: '/journey', section: 'checks' }], [['Budget', ['budget']], ['Developer', ['checks']]])).toEqual({ title: 'Developer', path: '/journey', section: 'checks' });
  });
  it('separates buyer drafts and guest drafts', () => {
    expect(accountDraftKey('buyer-a')).not.toBe(accountDraftKey('buyer-b'));
    expect(accountDraftKey('buyer-a')).not.toBe(accountDraftKey());
  });
  it('keeps offline field changes without dropping other-device fields', () => {
    const base = { budget: { down: '100', loan: '500' }, checks: { title: { note: '' } } };
    const mine = { budget: { down: '200', loan: '500' }, checks: { title: { note: 'Ask lawyer' } } };
    const remote = { budget: { down: '100', loan: '700' }, checks: { title: { note: '', source: 'Document' } } };
    expect(mergeJourney(base, mine, remote)).toEqual({ budget: { down: '200', loan: '700' }, checks: { title: { note: 'Ask lawyer', source: 'Document' } } });
  });
  it('retains intentional local deletions while preserving unrelated remote answers', () => {
    expect(mergeJourney({ budget: { down: '100', loan: '500' } }, { budget: { down: '' } }, { budget: { down: '100', loan: '700', emi: '40' } })).toEqual({ budget: { down: '', emi: '40' } });
  });
  it('allows only super admins to manage shared settings', () => {
    expect(canManageSettings(['super_admin'])).toBe(true);
    expect(canManageSettings(['support_manager'])).toBe(false);
    expect(canManageSettings([])).toBe(false);
  });
  it('preserves every home section once when reordered', () => {
    expect(settingsSchema.safeParse({ ...defaultSettings, sections: ['progress','journey','nextAction','guidance'] }).success).toBe(true);
    expect(settingsSchema.safeParse({ ...defaultSettings, sections: ['journey','journey','nextAction','guidance'] }).success).toBe(false);
  });
  it('rejects passwords below eight characters for account creation', () => {
    expect(credentialsSchema.safeParse({ email: 'buyer@example.test', password: '1234567' }).success).toBe(false);
    expect(credentialsSchema.safeParse({ email: 'buyer@example.test', password: '12345678' }).success).toBe(true);
  });
  it('never registers the offline worker in development, preview or an iframe', () => {
    const base = { production: true, hostname: 'finnest.example.com', iframe: false, search: '' };
    expect(refuseServiceWorker({ ...base, production: false })).toBe(true);
    expect(refuseServiceWorker({ ...base, hostname: 'id-preview--12e20cb0-845a-4f25-bc32-9f8ec393a7d1.lovable.app' })).toBe(true);
    expect(refuseServiceWorker({ ...base, hostname: 'preview-v2--finnest.lovable.app' })).toBe(true);
    expect(refuseServiceWorker({ ...base, iframe: true })).toBe(true);
    expect(refuseServiceWorker({ ...base, search: '?sw=off' })).toBe(true);
    expect(refuseServiceWorker(base)).toBe(false);
    expect(refuseServiceWorker({ ...base, hostname: 'preview-v2--finnest.example.com' })).toBe(false);
  });
});