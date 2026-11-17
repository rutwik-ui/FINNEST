import { z } from 'zod';
import { onboardingSlides } from './launchRules';
export const onboardingSlideSchema = z.object({ title: z.string().trim().min(1).max(120), body: z.string().trim().min(1).max(500), image: z.enum(['building', 'planning', 'family']) });
export const homeSections = ['journey', 'nextAction', 'progress', 'guidance'] as const;
export const settingsSchema = z.object({
  homeTitle: z.string().trim().min(1).max(120), homeSubtitle: z.string().max(300),
  guestEnabled: z.boolean(), maintenanceEnabled: z.boolean(), maintenanceMessage: z.string().trim().min(1).max(500),
  sections: z.array(z.enum(homeSections)).length(4).refine(v => new Set(v).size === 4, 'Keep every home section once'),
  onboardingVersion: z.number().int().min(1).max(10000).default(1),
  onboardingSlides: z.array(onboardingSlideSchema).min(2).max(4).default([...onboardingSlides]),
});
export type AppSettings = z.infer<typeof settingsSchema>;
export const defaultSettings: AppSettings = { homeTitle: 'Your home-buying journey', homeSubtitle: 'A clearer path to a place of your own.', guestEnabled: true, maintenanceEnabled: false, maintenanceMessage: 'FINNEST is temporarily unavailable. Please try again shortly.', sections: [...homeSections], onboardingVersion: 1, onboardingSlides: [...onboardingSlides] };
export const canManageSettings = (roles: string[]) => roles.includes('super_admin');