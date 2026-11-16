# Shipping the Android app (APK → Play Store)

The Android app is a **native Kotlin application**. It cannot be hosted on
Vercel — Vercel only serves websites. Android apps are distributed as an
installable file (`.apk`) or a bundle (`.aab`).

You already have the pipeline. This document explains what each button does and
how to move from "debug APK for me and my testers" to "published on the Play
Store".

---

## 1. The two workflows that already exist

| Workflow file | Trigger | Output | Who it is for |
|---|---|---|---|
| `.github/workflows/android-debug.yml` | Any push that touches `android/` | `FINNEST-android-debug` artifact (a debug-signed APK) | You, your testers, quick checks |
| `.github/workflows/android-release.yml` | Push a tag like `v0.5.0`, or **Actions → Android release APK → Run workflow** | `FINNEST-android-release` artifact (a release-signed APK) | Real installs, Play Store upload |

### Get a debug APK right now (no setup)

1. Open the **Actions** tab on GitHub.
2. Click **Android debug APK**.
3. Click the newest green run.
4. At the bottom, download **FINNEST-android-debug**.
5. Unzip it → you get `app-debug.apk`.
6. Copy it to your phone and tap it to install (allow "install from unknown
   source" if Android asks).

### Get a signed release APK (one-time setup)

1. **Create an upload keystore** (do this once, keep the file safe forever):

   ```sh
   keytool -genkey -v -keystore release.keystore \
     -alias finnest-upload -keyalg RSA -keysize 2048 -validity 10000
   ```

   It asks for a password — use a strong one and store it in a password
   manager. **If you lose this file you can never update the app on the Play
   Store.**

2. **Upload it to GitHub as a secret**:
   - Base64-encode the file:
     ```sh
     base64 -i release.keystore -o release.keystore.txt
     ```
   - GitHub → **Settings → Secrets and variables → Actions → New repository secret**
   - Add four secrets:
     | Secret name | Value |
     |---|---|
     | `ANDROID_KEYSTORE_BASE64` | the whole content of `release.keystore.txt` |
     | `ANDROID_KEYSTORE_PASSWORD` | the keystore password |
     | `ANDROID_KEY_ALIAS` | `finnest-upload` |
     | `ANDROID_KEY_PASSWORD` | the key password |

3. **Run the workflow**: Actions → **Android release APK** → **Run workflow**.
   The APK appears as the `FINNEST-android-release` artifact.

---

## 2. Firebase App Distribution (recommended for testing)

Free, fast, and no Google review. Best way to share a real installable build
with a handful of people.

1. Create a project at <https://console.firebase.google.com> (use the same
   Firebase project name style as your app; the Google Play developer account
   can be linked later).
2. **Project settings → Your apps → Add app → Android**. Register the package
   name `com.finnest.app`. Download `google-services.json` — **do not commit it**
   (it is not needed for the current build; the app talks to Supabase directly).
3. **Project settings → Service accounts → Generate new private key**
   (JSON). Add it to GitHub secrets as `FIREBASE_SERVICE_ACCOUNT`.
4. **Project settings → General → Service accounts → Firebase App Distribution**
   → add a **tester group** named `testers`, add your email addresses.
5. In Firebase App Distribution, get the **App ID** (looks like
   `1:1234567890:android:abcdef`) and a **Firebase CLI token**
   (`Project settings → Service accounts → Generate new private key` /
   *Firebase distribution* tab → **Generate new token**).
6. Add GitHub secrets `FIREBASE_APP_ID` and `FIREBASE_TOKEN`.

Now every run of the release workflow also pushes the APK to Firebase App
Distribution, and your testers get an email with an install link.

---

## 3. Google Play Store (for real users)

1. **Create a developer account** at <https://play.google.com/console> — this
   costs a one-time USD 25 registration fee. Personal accounts can still list
   free apps.
2. **Create the app** → *Apps → Create app* → name `FINNEST`, default language
   English, "App" type, free, accept the declarations.
3. **Set up the release signing key** in Play Console
   (*Release → Setup → App signing*). Play Console gives you its own
   app-signing key; you upload your **upload** key (the one from §1) and Play
   signs the store build with the app-signing key.
4. **Build an App Bundle (`.aab`)** — the Play Store requires a bundle, not an
   APK:
   ```sh
   cd android
   ./gradlew bundleRelease
   ```
   The file lands at
   `android/app/build/outputs/bundle/release/app-release.aab`.
   Sign it with the same keystore before uploading (or add a `signingConfigs`
   block to `android/app/build.gradle.kts` — see below).
5. **Upload**: Play Console → your app → *Releases → Production → Create new
   release* → upload the `.aab`.
6. **Fill in the store listing**: app name, short description, full
   description, screenshots (phone + at least one 7-inch tablet if you want
   tablet support), privacy policy URL, and the **Data safety** form
   (the app stores journey data on the device only and reads published
   guidance from Supabase — declare that accurately).
7. **Submit for review**. First submissions usually take a few hours to a few
   days.

### Optional: let Gradle sign the release build for you

Create `android/keystore.properties` **on your machine only** (it is
git-ignored by `local.properties` rules — add it to `.gitignore` if not
already):

```properties
storeFile=release.keystore
storePassword=your-store-password
keyAlias=finnest-upload
keyPassword=your-key-password
```

Then add to `android/app/build.gradle.kts`:

```kotlin
val keystoreProperties = java.util.Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    signingConfigs {
        if (keystoreProperties.getProperty("storeFile") != null) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
        }
    }
}
```

> **Caution:** if you change `android/app/build.gradle.kts` by hand, Lovable may
> overwrite it later. Re-check it after any Lovable change to the Android
> project. The CI workflow (which signs the APK outside Gradle) is unaffected by
> this.

---

## 4. Hosting the APK on Vercel (optional)

You can serve the APK as a download from your Vercel site:

1. Put the signed APK in `web-app/public/` (for example
   `web-app/public/finnest-app.apk`).
2. Commit it.
3. It is served at `https://your-domain/finnest-app.apk`.

Vercel serves files from `public/` as static assets. Keep the file under your
plan's deployment size limit and set
`Cache-Control` via `vercel.json` if you want it cached.

> A better long-term home for the APK is Firebase App Distribution (§2) or the
> Play Store — they handle updates, versioning and device installation.

---

## 5. What the Android app reads from the backend today

| Data | Source | Direction |
|---|---|---|
| Published guidance (`finnest_guidance`) | Supabase REST, public publishable key | read only |
| Experts (`finnest_experts`) | Supabase REST | read only (only in the newer `web-app` build) |
| Journey drafts, requirements, visit notes, reminders, profile | On-device Room database + private preferences | never uploaded |

That is why the admin panel and the Android app stay in sync automatically for
anything the admin publishes: the admin writes to Supabase, the Android app
reads the published rows on the next refresh.

**Not implemented yet:** Android sign-in and cloud sync of buyer drafts. Until
those land, a buyer who starts on the phone and continues on the web does not
carry their data across. That is the single biggest product gap to close
next — see `docs/requirements-traceability.md`.
