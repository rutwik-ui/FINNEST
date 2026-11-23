import { useEffect, useState } from 'react';
import { afterSplash, ONBOARDING_KEY, type LaunchPhase } from './launchRules';
export function useLaunchFlow({ signedIn, authLoading, version, settingsReady }: { signedIn: boolean; authLoading: boolean; version: number; settingsReady: boolean }) {
  const [phase, setPhase] = useState<LaunchPhase>('splash');
  const [elapsed, setElapsed] = useState(false);
  useEffect(() => { const timer = window.setTimeout(() => setElapsed(true), 1100); return () => window.clearTimeout(timer); }, []);
  useEffect(() => {
    if (!elapsed || authLoading || !settingsReady || phase !== 'splash') return;
    let completedVersion = 0;
    try { completedVersion = Number(localStorage.getItem(ONBOARDING_KEY)) || 0; } catch { /* Browsing stays available without storage. */ }
    setPhase(afterSplash({ signedIn, completedVersion, onboardingVersion: version }));
  }, [elapsed, authLoading, signedIn, version, phase, settingsReady]);
  const complete = () => { try { localStorage.setItem(ONBOARDING_KEY, String(version)); } catch { /* The introduction may repeat when browser storage is blocked. */ } setPhase('ready'); };
  return { phase, complete, account: () => signedIn ? complete() : setPhase('account'), replay: () => setPhase('onboarding') };
}