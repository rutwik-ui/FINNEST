# FINNEST

FINNEST is an Indian home-buying guidance product, delivered in small, reviewable phases.

## Current Android scope

`android/` is a native Kotlin + XML Android Studio project. It includes the introductory screens, New Property / Resale Property selection, and the New Property **Requirements & Budget** (step 1) and **Questions worth asking** developer checks (step 2). Optional estimates, location and configuration choices are saved locally as they are edited. Reopen the New Property journey to resume the draft. Step 1 continues to step 2; step 2 records a status, note, source, and optional follow-up date for each of six checks. Back and Save & Skip return to the preceding screen; step 2 Save & Continue explains that step 3 is not available yet. The prototype shows only the collapsed default status: other status labels are provisionally drawn from its introductory legend and should be confirmed against the expanded dropdown.

The onboarding artwork is cropped from the supplied Drive prototypes. The UI adapts the step-one layout to a single-column Android screen. Priority tiers, personalisation screen, dashboard, further steps, Resale forms, authentication, cloud sync, website and admin are not implemented yet. This app provides general guidance, not legal or financial advice.

### Run

Open `android/` in Android Studio, install Android SDK 35 and JDK 17, sync Gradle and run the `app` configuration on an API 24+ emulator or device. Or run `cd android && ./gradlew assembleDebug` with a configured Android SDK and JDK 17. Run the Room instrumentation test with `./gradlew connectedDebugAndroidTest` on an emulator or device. No Supabase or Firebase credentials are required.

### Storage and limitations

Onboarding completion and the selected journey use private Android preferences. Step-one and step-two draft fields use a local Room database and are scoped to New Property. Nothing is uploaded. Uninstalling the app removes local data; device backup is disabled. Account migration and cloud sync require future work. The connected Supabase project's public schema returned no visible base tables at inspection time; no database changes have been made.

## Website foundation

`web/` contains a responsive React/Vite user website, with a guest dashboard, New Property requirements and developer-check forms, a locally saved project shortlist, expert-category guidance, and a profile page. Run it with:

```sh
cd web
npm install
npm run dev
```

The website currently stores guest data only in browser localStorage. It does not sync with Android or Supabase and clearing browser data removes it. The `/Adminlogin` route is an intentionally locked placeholder: no admin login, role checks, content editing, database access or management functionality has been implemented. Do not deploy this as a production administration system. The web frontend is React/Vite; the master brief's proposed Laravel backend/admin and a shared API remain future work. The roadmap artwork is AI-generated, based on the supplied dashboard reference.
