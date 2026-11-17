# FINNEST production refactor — implementation phases

## Screens
1. Shared header, footer and mobile navigation; home and journey selection.
2. Email/password Log In, Sign Up, password recovery and Profile.
3. Experts and Guidance: actual published records, filters, loading, retry and offline states.
4. Existing journey forms and summaries: preserve answers and remove redundant home navigation.
5. Administrator settings: home content, guest access, maintenance message, publish workflow and history.
6. Native Android launch, onboarding and account screens in the existing Android project.

## Files
- Branding: `src/finnest/BrandMark.jsx`, `src/assets/`, `public/`, `src/routes/__root.tsx`.
- Web screens: `src/finnest/FinnestApp.jsx`, `Authentication.tsx`, `Experts.jsx`, `Guidance.jsx`, `LiveData.jsx`, `CommonPages.jsx`, shared FINNEST CSS.
- Offline and sync: `src/finnest/cloudSync.js`, browser-safe IndexedDB helpers and service-worker registration; `vite.config.ts`.
- Management: `AdminConsole.jsx`, a focused settings panel, validated settings schema and tool-managed Supabase migration.
- Validation: focused tests under `src/test/`, browser checks and Android CI where available.

## Decisions and limits
- Keep the connected external Supabase and existing administrator roles. No Google/social login or authenticator requirement.
- Preserve the existing native Android project. The generic Capacitor instruction conflicts with the project's explicit native-delivery constraint; no wrapper replacement.
- Register website head tags through TanStack route heads, not a nonexistent Vite SPA `index.html`.
- Network-first HTML protects upgrades; cache-first only immutable hashed assets. Private data and authenticated operations must never enter service-worker caches.
- Never register offline workers in development or Lovable preview. Published offline checks and native release checks are separate gates.
- Preserve journey selection as an existing feature; replace obstructive initial presentation, not the journey capability or community banner.
- Do not manufacture a vector logo from the raster upload. Generate scaled raster icons from the original pixels and disclose any visible source defects.
- Production email autoconfirm and hosting settings require authorized external service management. Source changes alone cannot change them.

## Verification
- Test access rules and concrete limits, account isolation, merge/retry behavior and safe registration guards.
- Exercise public web navigation, auth mode switching/recovery validation, offline edits and reconnect states on desktop and mobile.
- Verify administrator publish/readback with an authorized session when available; otherwise report the authenticated test gate as blocked.
- Build and inspect native artifacts only through the existing Android CI; never rename an old APK as a new release.