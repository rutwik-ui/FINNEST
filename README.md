# FINNEST

FINNEST is an Indian home-buying guidance product, delivered in small, reviewable phases.

## Current Android scope

`android/` is a native Kotlin/XML guest application. The New Property flow currently includes a dashboard, locally saved requirements, developer checks, a shortlist of project names, visit observations, resident feedback, an illustrative loan guide, legal and financial due diligence, final review, journey summary, in-app dated reminders, an online published-guidance library, informational expert roles, a verbal-promise risk guide an About screen, a device-only profile and scoped local data deletion. Checklist status labels are provisional because the references show closed menus. These are user entries, not independently verified findings or professional advice.

The separate Resale overview and the six prototype subject areas are available as local planning and checklist notes, with a resale summary. This is not a complete resale journey: the prototype and supporting document have an unresolved journey mapping. Society and handover statuses and lawyer review status use free text because their expanded options are not visible; other checklist labels are provisional.

The app is incomplete: Android authentication, cloud sync, the remaining Resale journey, comparison details, personalization, real notifications, expert referrals and several shared screens are not yet implemented. Emulator checks cover selected persistence and navigation scenarios; visual, accessibility and real-device testing remain necessary.
### Run

Open `android/` in Android Studio, install Android SDK 35 and JDK 17, sync Gradle and run the `app` configuration on an API 24+ emulator or device. Or run `cd android && ./gradlew assembleDebug` with a configured Android SDK and JDK 17. Run the Room instrumentation test with `./gradlew connectedDebugAndroidTest` on an emulator or device. The Android guidance library uses the bundled public Supabase project key; no service-role credentials are required.

### Storage and limitations

Onboarding completion and the selected journey use private Android preferences. Journey draft fields use a local Room database scoped to New Property; reminder notes use a separate device-local scope. Nothing is uploaded. Uninstalling the app removes local data; device backup is disabled. Account migration and cloud sync require future work. Android reads published guidance from Supabase, but does not upload buyer drafts; the private draft table and document bucket are prepared for a future sync phase.

## Website foundation

`web/` contains a responsive React/Vite user website, with a guest dashboard, New Property requirements, developer-check, and visit-observation forms, a locally saved shortlist of up to five project names, expert-category guidance, and a profile page. Run it with:

```sh
cd web
npm install
cp .env.example .env.local
npm run dev
```

The website supports Supabase email/password sign-in and registration on the Profile page. Guest data still stays only in browser localStorage: signing in does not upload or sync it, and clearing browser data removes it. The connected project requires email confirmation and currently has its Auth Site URL set to localhost. Configure the real site URL and allowed redirect URLs in Supabase Auth before testing email confirmation; do not set a production URL to a temporary preview. The client configuration in `.env.example` is a publishable key, not a service-role key. Project comparison details and cross-device synchronization are not implemented. The `/guidance` route displays published guidance from Supabase, with a category filter and 50-item pages ordered by update time and ID. It does not show drafts, and renders content as plain text. Refresh to see content that changed while browsing; live PostgREST pagination and authorization still require end-to-end testing. The `/Adminlogin` route now provides an early role-restricted guidance editor (details below). It does not yet manage users, buyer data, Android forms, experts or projects; do not use it for production operations. The web frontend is React/Vite; the master brief's proposed Laravel backend/admin and a shared API remain future work. The roadmap artwork is AI-generated, based on the supplied dashboard reference.

## Supabase foundation

Migration `supabase/migrations/20261109073922_finnest_private_foundation.sql` was applied to the connected project. It creates an RLS-protected per-user/per-device draft table and a private document bucket restricted to each authenticated user's UUID folder, PDF/JPEG/PNG files and 10 MiB per file. There is no application upload, download, draft backup, migration, Android integration or admin access yet. Never put a service-role/secret key in the app or browser. Before enabling data sync, implement and test account ownership, conflict handling, explicit guest migration and two-user RLS isolation.

### Download a test APK from GitHub Actions

The repository's **Android debug APK** workflow builds an Android-only debug APK when Android code changes, and can also be run manually from the Actions tab using **Run workflow**. Open a successful run and download the **FINNEST-android-debug** artifact; unzip it to get `app-debug.apk`. GitHub artifacts require GitHub sign-in and expire after 14 days. Install it only on a test device you trust, enabling installation from your chosen source if Android asks.

This is a debug-signed, incomplete test build, not a Play Store release. It does not include cloud sync, Android login or the complete resale journey. Separate GitHub workflows run JVM and Android 14 emulator/instrumentation tests; the app still needs real-device and broader end-to-end testing. Debug signing keys can differ between CI builds, so an APK update may fail. Uninstalling to install another build deletes all local drafts. If the workflow is unavailable, open `android/` in Android Studio and build a debug APK locally as described above.

## Early administrator content editor

`/Adminlogin` uses Supabase email/password authentication. Only accounts with a server-assigned `super_admin` or `content_manager` role can create or edit guidance. The database enforces these permissions with RLS; regular users cannot grant roles. An append-only-to-clients audit records content create/update metadata. Drafts are editor-only; **published guidance is publicly readable**. Do not enter personal information or secrets. Published guidance appears in the website and Android guidance libraries. Android uses the public project key, requests only published content and needs internet; no offline cache is provided. Live end-to-end authorization and pagination testing remain outstanding.

No account has automatically been made an administrator. To bootstrap a trusted account, first create and confirm that account through Supabase Auth, then a project owner can use the Supabase SQL editor to insert its verified `auth.users.id` and role into `public.finnest_admin_roles`. Do not grant access to an unverified account. No service-role key belongs in the web app. The migration is `20261109092048_finnest_admin_content_foundation.sql`.

**Not production-ready:** the editor now requires authenticator verification and its database policies require an AAL2 session, but it does not use separate admin sessions. Full real-account MFA and two-user RLS tests, recovery, security review, monitoring, backups, content approval, full admin operations and capacity tests remain outstanding. The initial role and any subsequent grants must be managed by a trusted operator, not by the browser.

## Current review and tests

See `docs/requirements-traceability.md` for the distinction between native, admin-managed and backend functionality and the remaining release gates. Administrator TOTP setup is available after a trusted operator grants a content role; the code and QR are sensitive and should never be shared. If an authenticator is lost, a secure, documented recovery process must be established before production use.

For the web run `npm run test:unit`, `npm run test:ui`, and `npm run build` from `web/`. The MFA UI tests mock Supabase; they do not prove live enrollment or RLS behavior. Migration `20261109095719_finnest_admin_require_mfa.sql` enforces AAL2 for admin draft reads, content writes and audit reads; published guidance remains public.

### Android profile and local deletion

Profile & settings saves optional name and city locally. The app currently always saves guest data locally; it does not offer a session-only switch or Android account sign-in. Privacy & data use describes the current implementation rather than treating prototype-only consent switches as functioning services. Deleting local data requires typing DELETE and removes New Property, Resale, reminders and local profile fields in a single database operation; journey selection and onboarding preferences are kept. It does not delete website data, Supabase data or any account. Device backup is disabled, and the action is irreversible.

### Database authorization checks

The `Database policy tests` GitHub Actions workflow runs the migrations and RLS behavior tests on disposable PostgreSQL with a minimal simulated Supabase Auth/Storage shim. See `supabase/tests/README.md` for coverage and its important limitations. It does not use or change the connected Supabase project, and is not a substitute for real staging Auth, MFA and Storage API tests.

The early guidance editor warns before discarding unsaved work, provides retry on access/list errors, and reports conflicting edits rather than silently overwriting them. This is not autosave or a version history; browser unload prompts depend on browser behavior. The list currently shows only the 100 most recently updated records. The panel remains a prototype and requires real staging authorization, MFA and operational testing.
