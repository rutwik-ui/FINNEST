import { describe, expect, it } from 'vitest';
import { afterSplash } from '@/finnest/launchRules';
import { settingsSchema, defaultSettings } from '@/finnest/appSettings';
describe('FINNEST launch decisions', () => {
  it('requires two to four introduction slides', () => {
    expect(settingsSchema.safeParse({ ...defaultSettings, onboardingSlides: defaultSettings.onboardingSlides.slice(0, 1) }).success).toBe(false);
    expect(settingsSchema.safeParse({ ...defaultSettings, onboardingSlides: defaultSettings.onboardingSlides.slice(0, 2) }).success).toBe(true);
    expect(settingsSchema.safeParse({ ...defaultSettings, onboardingSlides: [...defaultSettings.onboardingSlides, ...defaultSettings.onboardingSlides] }).success).toBe(false);
  });
  it('sends a first-time guest to onboarding instead of the dashboard', () => {
    expect(afterSplash({ signedIn: false, completedVersion: 0, onboardingVersion: 1 })).toBe('onboarding');
  });
  it('lets a returning web guest browse without repeating onboarding', () => {
    expect(afterSplash({ signedIn: false, completedVersion: 1, onboardingVersion: 1 })).toBe('ready');
  });
  it('restores a valid existing account directly', () => {
    expect(afterSplash({ signedIn: true, completedVersion: 0, onboardingVersion: 1 })).toBe('ready');
  });
  it('replays introduction after a published version increases', () => {
    expect(afterSplash({ signedIn: false, completedVersion: 1, onboardingVersion: 2 })).toBe('onboarding');
    expect(afterSplash({ signedIn: true, completedVersion: 1, onboardingVersion: 2 })).toBe('onboarding');
  });
});