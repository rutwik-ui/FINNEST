# FINNEST

FINNEST is an Indian home-buying guidance product, delivered in small, reviewable phases.

## Current Android scope

`android/` is a native Kotlin + XML Android Studio project. It includes the introductory screens, New Property / Resale Property selection, and the New Property **Requirements & Budget** (step 1) and **Questions worth asking** developer checks (step 2). Optional estimates, location and configuration choices are saved locally as they are edited. Reopen the New Property journey to resume the draft. Step 1 continues to step 2; step 2 records a status, note, source, and optional follow-up date for each of six checks. Step 3 offers a local shortlist of up to five project names with rename and remove. Step 4A records visit observations for 14 topics with optional notes, sources, statuses and follow-up dates. Back and Save & Skip return to the preceding screen; step 4A Save & Continue explains that resident feedback (step 4B) is not yet available. The prototype shows only the collapsed default status: other status labels, including those for visit observations, are provisionally drawn from its introductory legend and should be confirmed against the expanded dropdown.

The onboarding artwork is cropped from the supplied Drive prototypes. The UI adapts the step-one layout to a single-column Android screen. Priority tiers, personalisation screen, Android dashboard, comparison details, further steps, Resale forms, authentication, and cloud sync are not implemented yet. This app provides general guidance, not legal or financial advice.

### Run

Open `android/` in Android Studio, install Android SDK 35 and JDK 17, sync Gradle and run the `app` configuration on an API 24+ emulator or device. Or run `cd android && ./gradlew assembleDebug` with a configured Android SDK and JDK 17. Run the Room instrumentation test with `./gradlew connectedDebugAndroidTest` on an emulator or device. No Supabase or Firebase credentials are required.

### Storage and limitations

Onboarding completion and the selected journey use private Android preferences. Step-one, step-two, shortlist, and visit-observation draft fields use a local Room database and are scoped to New Property. Nothing is uploaded. Uninstalling the app removes local data; device backup is disabled. Account migration and cloud sync require future work. Android does not yet use Supabase; the connected Supabase project now contains a private draft table and document bucket prepared for a future sync phase.

## Website foundation

`web/` contains a responsive React/Vite user website, with a guest dashboard, New Property requirements, developer-check, and visit-observation forms, a locally saved shortlist of up to five project names, expert-category guidance, and a profile page. Run it with:

```sh
cd web
npm install
cp .env.example .env.local
npm run dev
```

The website supports Supabase email/password sign-in and registration on the Profile page. Guest data still stays only in browser localStorage: signing in does not upload or sync it, and clearing browser data removes it. The connected project requires email confirmation and currently has its Auth Site URL set to localhost. Configure the real site URL and allowed redirect URLs in Supabase Auth before testing email confirmation; do not set a production URL to a temporary preview. The client configuration in `.env.example` is a publishable key, not a service-role key. Project comparison details and cross-device synchronization are not implemented. The `/Adminlogin` route now provides an early role-restricted guidance editor (details below). It does not yet manage users, buyer data, Android forms, experts or projects; do not use it for production operations. The web frontend is React/Vite; the master brief's proposed Laravel backend/admin and a shared API remain future work. The roadmap artwork is AI-generated, based on the supplied dashboard reference.

## Supabase foundation

Migration `supabase/migrations/20261109073922_finnest_private_foundation.sql` was applied to the connected project. It creates an RLS-protected per-user/per-device draft table and a private document bucket restricted to each authenticated user's UUID folder, PDF/JPEG/PNG files and 10 MiB per file. There is no application upload, download, draft backup, migration, Android integration or admin access yet. Never put a service-role/secret key in the app or browser. Before enabling data sync, implement and test account ownership, conflict handling, explicit guest migration and two-user RLS isolation.

### Download a test APK from GitHub Actions

The repository's **Android debug APK** workflow builds an Android-only debug APK when Android code changes, and can also be run manually from the Actions tab using **Run workflow**. Open a successful run and download the **FINNEST-android-debug** artifact; unzip it to get `app-debug.apk`. GitHub artifacts require GitHub sign-in and expire after 14 days. Install it only on a test device you trust, enabling installation from your chosen source if Android asks.

This is a debug-signed, incomplete test build, not a Play Store release. It does not include cloud sync, Android login, the full dashboard or the resale journey. GitHub's build runs JVM unit tests, not emulator/instrumentation tests; the app still needs device testing. If the workflow is unavailable, open `android/` in Android Studio and build a debug APK locally as described above.

## Early administrator content editor

`/Adminlogin` uses Supabase email/password authentication. Only accounts with a server-assigned `super_admin` or `content_manager` role can create or edit guidance. The database enforces these permissions with RLS; regular users cannot grant roles. An append-only-to-clients audit records content create/update metadata. Drafts are editor-only; **published guidance is publicly readable**. Do not enter personal information or secrets. Guidance does not yet appear in the website or Android app.

No account has automatically been made an administrator. To bootstrap a trusted account, first create and confirm that account through Supabase Auth, then a project owner can use the Supabase SQL editor to insert its verified `auth.users.id` and role into `public.finnest_admin_roles`. Do not grant access to an unverified account. No service-role key belongs in the web app. The migration is `20261109092048_finnest_admin_content_foundation.sql`.

**Not production-ready:** the admin UI does not enforce MFA or separate sessions, and full two-user RLS tests, security review, recovery, monitoring, backups, content approval, full admin operations and capacity tests remain outstanding. The initial role and any subsequent grants must be managed by a trusted operator, not by the browser.
