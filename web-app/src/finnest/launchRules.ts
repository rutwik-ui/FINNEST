export type LaunchPhase = 'splash' | 'onboarding' | 'account' | 'ready';
export const ONBOARDING_KEY = 'finnest:onboarding-completed-version';
export function afterSplash({ signedIn, completedVersion, onboardingVersion }: { signedIn: boolean; completedVersion: number; onboardingVersion: number }): LaunchPhase {
  if (completedVersion > 0 && completedVersion < onboardingVersion) return 'onboarding';
  if (signedIn || completedVersion >= onboardingVersion) return 'ready';
  return 'onboarding';
}
export const onboardingSlides = [
  { title: 'Buying a home is a big decision.', body: 'There are questions to ask, documents to understand and choices to make. You do not have to figure it all out at once.', image: 'building' },
  { title: 'Know what to check, one step at a time.', body: 'Keep your own questions and notes together, and see what still needs attention.', image: 'planning' },
  { title: 'Move forward with more clarity.', body: 'A checklist is a way to stay oriented—not an approval to buy. Get qualified help when you need it.', image: 'family' },
] as const;