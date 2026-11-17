# FINNEST — November 17 corrections and completion plan

## Preserve
- Original F + outline house + NNEST logo, website typography, community banner, journey forms and saved answers.
- Uploaded red house remains the favicon/launcher source. Logo alignment is a correction, not a new identity.
- Existing native Android app and shared Supabase accounts; no Capacitor replacement, OAuth or mandatory authenticator.
- Preview-safe offline support: network-first HTML, no private API caching.

## Screens and files
1. **Header and menu:** `FinnestApp.jsx`, new `AppHeader.tsx`, navigation helpers, `experience.css`. Desktop (1024+) has links and account actions without a hamburger; smaller screens have an accessible drawer with account actions only inside it.
2. **Original logo:** `BrandMark.jsx`, a full outlined lockup asset, shared website placements and native logo layout; retain favicon/launcher artwork.
3. **Welcome and account flow:** a focused web startup module and `LaunchFlow.tsx`, shared `Authentication.tsx`, Profile replay action; native `LaunchActivity.kt`, `MainActivity.kt`, `AccountActivity.kt`. First visit: splash → introduction → account/guest → dashboard; valid returning sessions bypass introduction.
4. **Published settings:** `appSettings.ts`, `AdminSettings.tsx`, existing Supabase settings validation, onboarding content/version with draft/publish and audits; native must read the same published configuration.
5. **Navigation/offline audit:** existing journey controls and public data screens, without deleting functional step navigation or changing form IDs.
6. **Verification/delivery:** focused Vitest flow rules, Playwright at mobile/tablet/desktop, native JVM/device CI and new artifact readback; update `roadmap.md` with verified results.

## Gates and limits
- Check each phase before reporting it done. Public/local testing does not prove signed-in cross-device syncing or super-admin publishing.
- External Supabase cannot mint an authorized test session here; never request or expose passwords.
- Vercel deployment, external-repository source delivery, signed Android release and app-store graphics are separate deliverables, not consequences of a successful website check.
- Full media CMS, article scheduling, suspension, taxonomies, dark-mode coverage, private action queues and native recovery need their own completed checks; preserve explicit open tasks until implemented.