import { z } from 'zod';
export const homeSections = ['journey', 'nextAction', 'progress', 'guidance'] as const;
export const settingsSchema = z.object({
  homeTitle: z.string().trim().min(1).max(120), homeSubtitle: z.string().max(300),
  guestEnabled: z.boolean(), maintenanceEnabled: z.boolean(), maintenanceMessage: z.string().trim().min(1).max(500),
  sections: z.array(z.enum(homeSections)).length(4).refine(v => new Set(v).size === 4, 'Keep every home section once'),
});
export type AppSettings = z.infer<typeof settingsSchema>;
export const defaultSettings: AppSettings = { homeTitle: 'Your home-buying journey', homeSubtitle: 'A clearer path to a place of your own.', guestEnabled: true, maintenanceEnabled: false, maintenanceMessage: 'FINNEST is temporarily unavailable. Please try again shortly.', sections: [...homeSections] };
export const canManageSettings = (roles: string[]) => roles.includes('super_admin');