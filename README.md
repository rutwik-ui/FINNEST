# FINNEST

FINNEST is an Indian home-buying guidance product, delivered in small, reviewable phases.

## Current Android scope

`android/` is a native Kotlin + XML Android Studio project. It includes the introductory screens, New Property / Resale Property selection, and the New Property **Requirements & Budget** (step 1) and **Questions worth asking** developer checks (step 2). Optional estimates, location and configuration choices are saved locally as they are edited. Reopen the New Property journey to resume the draft. Step 1 continues to step 2; step 2 records a status, note, source, and optional follow-up date for each of six checks. Step 3 offers a local shortlist of up to five project names with rename and remove. Back and Save & Skip return to the preceding screen; step 3 Save & Continue explains that step 4 is not available yet. The prototype shows only the collapsed default status: other status labels are provisionally drawn from its introductory legend and should be confirmed against the expanded dropdown.

The onboarding artwork is cropped from the supplied Drive prototypes. The UI adapts the step-one layout to a single-column Android screen. Priority tiers, personalisation screen, Android dashboard, comparison details, further steps, Resale forms, authentication, and cloud sync are not implemented yet. This app provides general guidance, not legal or financial advice.

### Run

Open `android/` in Android Studio, install Android SDK 35 and JDK 17, sync Gradle and run the `app` configuration on an API 24+ emulator or device. Or run `cd android && ./gradlew assembleDebug` with a configured Android SDK and JDK 17. Run the Room instrumentation test with `./gradlew connectedDebugAndroidTest` on an emulator or device. No Supabase or Firebase credentials are required.

### Storage and limitations

Onboarding completion and the selected journey use private Android preferences. Step-one, step-two, and shortlist draft fields use a local Room database and are scoped to New Property. Nothing is uploaded. Uninstalling the app removes local data; device backup is disabled. Account migration and cloud sync require future work. Android does not yet use Supabase; the connected Supabase project now contains a private draft table and document bucket prepared for a future sync phase.

## Website foundation

`web/` contains a responsive React/Vite user website, with a guest dashboard, New Property requirements and developer-check forms, a locally saved shortlist of up to five project names, expert-category guidance, and a profile page. Run it with:

```sh
cd web
npm install
cp .env.example .env.local
npm run dev
```

The website supports Supabase email/password sign-in and registration on the Profile page. Guest data still stays only in browser localStorage: signing in does not upload or sync it, and clearing browser data removes it. The connected project requires email confirmation and currently has its Auth Site URL set to localhost. Configure the real site URL and allowed redirect URLs in Supabase Auth before testing email confirmation; do not set a production URL to a temporary preview. The client configuration in `.env.example` is a publishable key, not a service-role key. Project comparison details and cross-device synchronization are not implemented. The `/Adminlogin` route is an intentionally locked placeholder: no admin login, role checks, content editing, database access or management functionality has been implemented. Do not deploy this as a production administration system. The web frontend is React/Vite; the master brief's proposed Laravel backend/admin and a shared API remain future work. The roadmap artwork is AI-generated, based on the supplied dashboard reference.

## Supabase foundation

Migration `supabase/migrations/20261109073922_finnest_private_foundation.sql` was applied to the connected project. It creates an RLS-protected per-user/per-device draft table and a private document bucket restricted to each authenticated user's UUID folder, PDF/JPEG/PNG files and 10 MiB per file. There is no application upload, download, draft backup, migration, Android integration or admin access yet. Never put a service-role/secret key in the app or browser. Before enabling data sync, implement and test account ownership, conflict handling, explicit guest migration and two-user RLS isolation.

### Download a test APK from GitHub Actions

The repository's **Android debug APK** workflow builds an Android-only debug APK when Android code changes, and can also be run manually from the Actions tab using **Run workflow**. Open a successful run and download the **FINNEST-android-debug** artifact; unzip it to get `app-debug.apk`. GitHub artifacts require GitHub sign-in and expire after 14 days. Install it only on a test device you trust, enabling installation from your chosen source if Android asks.

This is a debug-signed, incomplete test build, not a Play Store release. It does not include cloud sync, Android login, the full dashboard or the resale journey. GitHub's build runs JVM unit tests, not emulator/instrumentation tests; the app still needs device testing. If the workflow is unavailable, open `android/` in Android Studio and build a debug APK locally as described above.
