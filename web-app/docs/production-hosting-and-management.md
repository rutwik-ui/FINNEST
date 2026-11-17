# FINNEST — Production hosting, shared accounts and release guide

Prepared: 13 November 2026 (UTC)

## 1. Recommendation and current delivery status

**Recommended: Vercel for the website and admin panel, the existing Supabase project for accounts/data/private files, and the existing native Android project for the APK.** Vercel has documented TanStack Start + Nitro support and Git-based deployments. Hostinger is also viable if you prefer a persistent Node.js server or already own a suitable plan; use its Node.js web-app service, not ordinary static/PHP hosting. No hosting subscription or new Supabase project is required just to continue development.

This is an implementation guide, not a certificate that the project is finished. No production hosting, domain connection, authentication-setting change, new APK or remote source update was performed while preparing this document.

| Area | Verified on 13 November 2026 | Remaining before final delivery |
| --- | --- | --- |
| Lovable preview | A preview address exists; no published or canonical production address is returned by the project URL service. | Configure the chosen host and verify its actual public address. |
| Existing FINNEST repository | `rutwik-ui/FINNEST`, branch `main`, revision `4ed56b6c66819bcaab6306a1bea02da89c606ebe`; folders include `web/`, `web-app/`, `android/`, `supabase/`. | Deliver latest website/admin sync source into `web-app/` without overwriting `web/` or `android/`. |
| Website/account sync source | The Lovable working copy has journey save/load, Realtime subscriptions, activity, documents and Buyer progress. Repository `web-app/` does **not** contain the new `cloudSync.js` source at the checked revision. | Fix and test the sync lifecycle and deliver it to GitHub; signed-in cross-device behavior is not yet verified. |
| GitHub checks | Four workflows exist. Website checks run against **`web/`**, not the current **`web-app/`**. No website deployment workflow appeared in that listing. | Add `web-app/` checks and connect deployment. A host-side Git integration could exist independently; its settings were not available to verify. |
| Admin accounts | `rutwik@paarami.com` and `digitalrazorpay@gmail.com` both have `super_admin` in the connected database. | Test both with authorized sessions; passwords are private and must not be added to this guide. |
| Hosted buyer auth settings | Public Supabase settings returned HTTP 200: email enabled, sign-up allowed, Google disabled, **email auto-confirm disabled**. | Turn off Confirm email in the existing project's Email provider settings, then test immediate sign-up/sign-in. |
| Native Android | Repository configuration is version **0.4.0**, a design/test build. No shared-account or journey-sync implementation was found in the inspected Android source. | Implement native auth, reliable offline/live sync, experts/support/documents and release tests. |
| Final APK/live links | Not available as a verified integrated production release. | Complete the release gates below; never relabel an older debug APK as final. |

## 2. What hosting on Node.js means

Extracting a source ZIP does not automatically deploy a full application. The web app must install its locked dependencies, compile both browser assets and server handlers for the selected host, receive its environment variables, and run with HTTPS. Uploading only `dist` or copying static pages would omit server functions used by operations such as protected bulk imports.

This project is **TanStack Start**, not a static React-only website. Its Vite wrapper already includes Nitro, React, routing and styling. Lovable's hosted server target is Cloudflare Workers; that is not a persistent Node.js backend. Outside Lovable, Nitro can compile the same server functions for Vercel or a Node server. Do not run a previously generated Cloudflare Worker bundle with Node or add a second set of Vite plugins.

**Hostinger Node can run the application**, after the Node-target build is configured and tested. **Vercel runs its Node server functions as managed functions**, not as an always-on Express server. If a later requirement needs a continuously running Node service, long-running jobs, custom sockets or operating-system access, use Hostinger Node/VPS or another dedicated service for that requirement. Supabase already provides the cross-device Realtime service; neither hosting choice needs to run those sockets itself.

## 3. One connected product

```text
Website + secure admin panel     Native Android APK
             |                         |
             +---- same Supabase ------+
                  Auth / Database
                  Private Storage
                  Realtime
```

- Keep Supabase project `wyjjzynjoorjmpffndzu`; do not create disconnected accounts or move this project to Lovable Cloud.
- Website and Android authenticate through Supabase Auth. The same credentials work in both, but each device has its own session.
- Journey records use `finnest_journeys`: one row per user and journey type, compatible payload sections, version checks and current-step/progress data.
- Activity uses `finnest_activity`; file metadata uses `finnest_documents`; uploaded files stay in `finnest-private-documents`.
- Profiles, roles, published guidance, experts and support use the existing shared tables. Do not duplicate them per platform.
- The admin panel is a management interface, **not the database or a proxy that must stay open**. Clients still connect to the shared service when no administrator is logged in.
- Administrator roles and access rules enforce supported control centrally. Administrators do not automatically gain the ability to replace installed APK code or arbitrary buyer data.

## 4. Automatic updates: the critical repository distinction

The accessible connection named **Razorpay's GitHub API** is a GitHub API connection. It permits explicit authorized repository operations; it is **not proof that this Lovable project has two-way Git sync to `rutwik-ui/FINNEST`**. Lovable Git sync creates its own repository and cannot attach this project to an already-existing repository. Do not replace the user's source-of-truth repository to conceal that gap.

Required update chain:

```text
Edit in Lovable
    -> verified delivery to rutwik-ui/FINNEST/web-app/
    -> web-app tests and deployment build succeed
    -> Vercel or Hostinger deploys the approved main revision
    -> public website and admin panel show the new version
```

**The middle step is not presently automatic or verified.** Connecting FINNEST to Vercel/Hostinger makes GitHub-to-host deployments automatic; it does not make Lovable-to-FINNEST delivery automatic.

To keep the existing repository, implement a maintained delivery bridge: a reviewed CI workflow in the actual Lovable-synced source repository copies only approved application source into `FINNEST/web-app/`, uses a least-privilege GitHub App or repository-scoped credential stored as a CI secret, checks both revisions, and opens a reviewed change or updates the approved branch after checks. Preserve `web/`, `android/`, all existing history and migration records. Exclude `.env`, `.git`, `.jj`, `.lovable`, dependencies and generated outputs. Prevent sync loops and record the source commit in the delivered revision. Do not use a developer's broad personal token or scrape editor state.

Until that bridge is implemented and tested, delivery to FINNEST must remain an explicit checked operation. Lovable's Project settings → Git identifies the actual synced repository; this was not verified by the GitHub API check.

For a production-level automatic chain, ensure failing `web-app/` tests prevent promotion; a host deployment hook on every push does not by itself wait for independent GitHub checks. Use deployment gates or a workflow which deploys only after tests succeed. Avoid two independent deployment mechanisms racing to publish different commits.

Updates are **not instantaneous on typing**. They appear after a successful commit, checks and deployment, then when clients load the new version. A currently open website may retain its old assets until reload. Lovable's own published site still requires Publish changes; a local commit does not publish it.

## 5. Vercel setup — recommended website host

### Initial setup

1. Deliver the current tested app to `rutwik-ui/FINNEST/web-app/` first. Importing the existing older copy will deploy an older product.
2. In Vercel, import the **existing** FINNEST repository using its GitHub integration.
3. Select root directory **`web-app`** and production branch **`main`**.
4. Use the **TanStack Start** preset, not Next.js or a static Vite preset. The project already contains Nitro; do not install duplicate plugins.
5. Choose Node.js **24**, as required by the current Vercel guide, and pin the tested package-manager version.
6. Use `bun install --frozen-lockfile` and `bun run build` for the existing `bun.lock`. Keep build-time packages installed. Nitro should auto-detect Vercel outside the Lovable build; setting host build variable `NITRO_PRESET=vercel` makes the target explicit. Do not export Lovable-internal environment flags to the external host.
7. Leave the output directory at the framework default. Do not set it to a static `dist` folder; verify server functions are deployed too.
8. Configure the variables below for Production and separately for Preview. Rebuild when a `VITE_` value changes.
9. Deploy to the Vercel-provided address, test public and protected paths, then attach your chosen domain and verify HTTPS.
10. Configure Supabase URLs only once the actual production origin is known.

### Required environment configuration

| Variable | Where needed | Value/source |
| --- | --- | --- |
| `VITE_SUPABASE_URL` | Browser build | `https://wyjjzynjoorjmpffndzu.supabase.co` |
| `VITE_SUPABASE_PUBLISHABLE_KEY` | Browser build | Publishable key from the existing Supabase project; safe for public clients with RLS. |
| `VITE_SUPABASE_PROJECT_ID` | Browser build | `wyjjzynjoorjmpffndzu` |
| `SUPABASE_URL` | Server runtime | The same production Supabase URL; never a sandbox-local address. |
| `SUPABASE_PUBLISHABLE_KEY` | Server runtime | Publishable key from the same project; used with a verified caller session. |
| `SUPABASE_SERVICE_ROLE_KEY` | Server runtime only, if a reviewed privileged handler uses it | Supply securely through the host's secret settings. Do not provide it to normal clients or Android. |

Audit connector-dependent features before moving hosts. Any required `GITHUB_API_KEY`, `LOVABLE_API_KEY` or other service credential is server-only and must be configured for that external deployment with supported access; credentials do not travel in a source ZIP. Do not include credentials in the MD, repository or APK. Current ordinary buyer reads and bulk imports should not need privileged bypasses.

Vercel's Hobby plan is intended for personal/non-commercial use; use an appropriate commercial plan for FINNEST and review current limits, terms and budget. Do not promise unlimited traffic or zero hosting costs.

## 6. Hostinger setup — valid alternative

### Managed Node.js web app

1. Use a Hostinger plan that actually provides **Node.js web apps**, or a VPS if full server administration is required. Verify account-specific availability and limits before purchasing.
2. Use **Import your Git repository** and authorize access to the original `rutwik-ui/FINNEST` repository. Avoid the public-URL cloning option: it creates a new repository copy and breaks the intended source-of-truth update chain.
3. Set root **`web-app`**, branch **`main`**, Node **24**, and application type **Other** when TanStack Start is not detected correctly.
4. Set build-time **`NITRO_PRESET=node-server`** and a build command invoking the existing `build` script. Do not replace the Cloudflare output after compilation; build for Node from source.
5. Hostinger's documented package managers are npm, yarn and pnpm. For its managed pipeline, generate and commit a tested compatible lockfile outside this Lovable workspace, or confirm a supported custom Bun build route. Do not assume `bun.lock` means Hostinger will install Bun. Use the same tested versions in CI and production.
6. Set entry file **`.output/server/index.mjs`**, relative to the `web-app` root, for **Other**. Verify that the actual build emits it. With Other + an entry file, Hostinger deploys the whole app root; do not upload secret files or use static-only output settings.
7. Add the same environment variables as Vercel, set `HOST=0.0.0.0`, and use the port supplied by the hosting service. Ensure the runtime process receives the variables and restarts after deployment.
8. Verify HTTPS, runtime logs, direct page loads, protected server calls, session refresh and saved-record readback.
9. Check the Hostinger **Auto-deployment** indicator and deployment commit. Managed Node Git deployments run install → build → start/restart. The generic Git file-copy feature does not perform this full pipeline.

For a VPS, the operations owner must also configure a process supervisor, reverse proxy, TLS, firewall, security patches, monitoring and backup/restore. A production start script may use `node .output/server/index.mjs`; do not run `vite dev` or use `vite preview` as the production server.

### Why not repeated ZIP uploads?

A ZIP can be used for a one-time deployment, but subsequent Lovable edits will not arrive automatically. Use Git deployment for ongoing maintenance. If a ZIP is needed later, export a clean source archive, excluding secrets, repository metadata, dependency folders and previous build outputs; build specifically for the chosen host.

## 7. Email/password sign-in, without email confirmation

### Buyer policy requested

- Email + password only on web and Android.
- No Google, magic-link or OTP sign-in buttons.
- New buyers can start immediately; no confirmation-email click is required.
- Login is still mandatory for cloud saving and private account data. Guest notes remain on-device until explicitly migrated.
- Retain password strength checks, secure session handling and logout. No-confirmation does not mean passwordless or unrestricted access.

### Required hosted-setting change

The checked hosted settings still require email confirmation. In the existing [Supabase Email provider settings](https://supabase.com/dashboard/project/wyjjzynjoorjmpffndzu/auth/providers): keep Email enabled and turn **Confirm email off**. Leave Google disabled. Save and re-read the hosted settings to confirm `mailer_autoconfirm=true`, then verify `signUp()` returns an actual session. This change applies to the shared account service immediately, regardless of where the website is hosted. It was **not changed** while preparing this guide.

Review existing unconfirmed accounts separately; do not assume a setting change retroactively confirms them. Without confirmation, an email address is user-supplied and not proof of mailbox ownership; do not use it as a verification claim or to auto-grant staff permissions.

The current website has email/password sign-in and sign-up, but needs a working forgot-password flow plus a public reset-password screen. Add equivalent native recovery handling. Password recovery still requires a real email sender and allowlisted links even when account confirmation is disabled. Do not implement a fixed/shared password or repeat previously exposed passwords from chat.

Set Supabase Site URL to the actual production origin; allowlist the exact required public recovery/callback destinations for production and testing. Native password recovery needs verified Android App Links or a deliberately configured deep-link handler. Do not invent a domain or silently allow every external redirect.

### Administrator policy — deliberately separate

Both existing administrators sign in with their own email/password at **`/Adminlogin`** without an authenticator requirement. Administrator identity and assigned roles are still verified by server functions and database policies. Guidance, imports, activity, buyer progress and private documents use the same role-based authorization; ordinary buyer accounts cannot open the console or grant themselves roles.

**Password-only administrator access is the requested policy.** It removes the additional authenticator protection, so protect each administrator password and use a private password reset if it was exposed. Do not advertise or link the admin panel from the public website. Passwords cannot be recovered from Supabase; administrators use their existing passwords or a secure reset flow.

## 8. Implementation work required for reliable shared sync

### Website repairs before release

The existence of sync code is not proof that it safely handles every account or network state. Complete these checks and repairs against the approved journey contract:

- **Account isolation:** reset account-scoped state on sign-out/user change; partition offline caches by user and guest. Never automatically upload the previous user's browser answers to another account.
- **Guest migration:** offer an explicit once-only import of guest answers after sign-in; never silently merge or overwrite existing cloud records. The current source automatically pushes local data when a remote journey is missing and therefore needs correction.
- **Save serialization:** avoid concurrent pushes, abort/ignore stale responses after a session changes, and handle first-insert conflicts as well as version mismatches.
- **Conflict preservation:** compare changed sections against the last acknowledged base. A Realtime event must not silently replace unsaved local edits. Use a deterministic most-recent accepted save for an overlapping section and preserve non-overlapping edits; database versions/timestamps arbitrate, not untrusted device clocks.
- **Truthful state:** report Saved only after every pending section is acknowledged; report Offline/Pending/Error accurately. Retry on reconnect and lifecycle resume; re-fetch authoritative rows when Realtime reconnects.
- **Durable recovery:** retain pending edits if the app closes or a write fails, including local-storage failure. Do not rely only on a debounce timer or a mutable in-memory flag.
- **Progress/current screen:** keep stable step identifiers across clients, distinguish recorded answers from completed submissions, and save current-screen changes even when answers did not change.
- **Activity/document integrity:** record only acknowledged events with the authenticated user; handle logging failures, avoid duplicate events on retry, clean up or reconcile partial uploads, and show explicit permission/network errors.
- **Accurate wording:** update Profile/privacy copy which currently alternates between “not synced” and promises Android sharing. Show only capabilities present in the installed release.

### Native Android work

Keep the existing Kotlin native app in `android/`. Do not replace it with a website wrapper.

1. Add native sign-up/sign-in/sign-out/recovery with the same Supabase project and public key. Use maintained Android-compatible SDKs and secure credential storage; never persist plaintext passwords.
2. Migrate Room into a user-and-journey-scoped cache; retain guest drafts separately. Add an outbox of durable pending section changes and WorkManager reconnect retries. Tokens are protected on the device and refreshed through Supabase.
3. Implement the same journey payload/version contract as the website; map the existing native form entries without replacing form IDs or save logic. Save to `finnest_journeys`, record platform `android` in activity, and re-fetch on sign-in/resume/reconnect.
4. Subscribe to authorized Realtime changes while active, remove subscriptions on logout/teardown, and avoid echo writes or duplicate channels. Background/offline devices catch up when they reconnect; “Realtime” is not a guarantee of instant delivery while suspended.
5. Connect published guidance and experts, support-ticket creation/status and private documents to the same tables and role-protected rules as the website. File uploads use the user's private folder and signed read links.
6. Match the approved mobile website's logo, banner, typography, form labels, spacing, navigation and save-state feedback. Check narrow, standard and enlarged-text phones, not just one emulator.

### Admin control for both platforms

| Operation | Management boundary |
| --- | --- |
| Guidance and experts | Shared records edited/published by appropriate roles; both clients must refresh or subscribe to see changes. |
| Buyer accounts and staff roles | Current role management exists; account suspension/deletion or other Auth administration requires separately implemented authorized server operations. Do not claim those capabilities already exist. |
| Buyer progress/activity | Shared journey/activity readers; verify authenticated role access, pagination, correct counts and reconnect behavior. |
| Support requests/status | Shared tickets; web and Android need matching status refresh and permitted actions. |
| Documents | Private storage; owner access plus authorized support/super-admin access, signed links, validated metadata and audit/privacy controls. |
| Feature flags or maintenance | Not yet a complete shared feature-control system. If needed, implement a protected shared configuration contract and enforcement in both clients; hiding a button is not authorization. |
| UI/code releases | Web deployment and Android release workflow, **not** an unrestricted admin content editor. |

Admins must not silently rewrite a buyer's answers. If future staff-assisted editing is required, implement explicit authorization, audit events and clear buyer disclosure.

## 9. What updates immediately, and what needs a release

| Change | Delivery behavior |
| --- | --- |
| Published guidance/expert/support data | Shared database change; visible when a client refreshes or receives its implemented subscription. Not every current screen is subscribed. |
| Saved journey answers | Live between signed-in clients once native sync is implemented and verified; offline clients reconcile later. |
| Database schema/policies | Apply to the shared database immediately when a migration is executed; coordinate backward compatibility with installed APKs. Git sync alone does not execute migrations. |
| Website/admin UI or functionality | Commit → checked deployment → new website version; open tabs may need reload. |
| Native Android UI/functionality | Build a new APK/AAB, sign with the same protected release key and distribute/install the update. Website deployment cannot replace native layouts. |
| Server secret changes | Use the chosen host's environment settings and redeploy/restart as required. On Lovable, preview picks them up but the published snapshot needs republishing. |

## 10. Production management checklist

- One owner for production hosting, Supabase, domain/DNS and Android signing; named administrator accounts rather than shared credentials.
- Separate staging from production where possible, with sanitized test data; do not test account isolation or destructive operations on real buyers without authorization.
- Enforce row-level access in the database and revalidate sessions/roles on the server. Use server-side secrets only for narrowly authorized privileged operations.
- Add `web-app/` test/build/security checks. Protect production promotion from failed tests and record deployment revisions; current `main` was reported unprotected at the check.
- Review known security warnings, privileged function grants and leaked-password protection against a fresh scan/settings read. Do not treat earlier warning summaries as current confirmation.
- Configure a real email sender for recovery, monitor delivery and auth errors, and apply existing provider abuse protections suitable for public no-confirmation sign-up.
- Monitor application/server/auth/storage errors without logging passwords, tokens or private financial answers. Set uptime checks, cost alerts and Realtime connection budgets.
- Configure database backups and test restoration; back up private file objects separately, not just their database metadata. Protect release signing credentials and retain a secure offline backup.
- Keep database and journey payload changes compatible with earlier installed native versions; migrate forward deliberately and audit changes.
- Maintain a privacy/retention/deletion process appropriate to buyers' financial details and uploaded documents; local deletion must not pretend to delete cloud data.
- Maintain rollback procedures: restore the previous web deployment; release a higher-version Android correction; avoid destructive schema rollback which loses buyer data.

## 11. Release acceptance tests — final means checked

Use authorized buyer accounts A/B and an authorized administrator. External Supabase does not supply a managed browser test session here; authenticated checks need an authorized testing path. Never bypass authentication or role checks, or use service-role access to impersonate a buyer test.

1. **Immediate account creation:** a new buyer signs up without confirmation, receives a real session, signs out and signs in on website and Android using the same credentials.
2. **Web → Android → admin:** save four real journey steps on web; verify the same values/current progress on Android and Buyer progress. Add another section on Android; read it on web and admin.
3. **Two-way live updates:** both clients open under one account; acknowledge a save and confirm the other displays the changed value without manual refresh. Record observed latency under a specified network, not an unconditional instant-delivery promise.
4. **Offline and restart:** disconnect Android, edit/save, close/reopen, reconnect; verify queued changes persist and are acknowledged exactly once. Repeat browser recovery where applicable.
5. **Conflict handling:** simultaneous different-section edits preserve both; overlapping edits resolve according to the defined version/timestamp rule, with an accurate notice and no hidden loss.
6. **User isolation:** A cannot read/write B's journeys/files. Switching accounts or signing out never migrates private cached answers into a new account or guest draft.
7. **Admin boundary:** buyer/non-admin attempts fail even with a crafted request; role revocation and missing authentication deny protected operations; assigned super admins can sign in directly without an authenticator. Test both existing super-admin accounts with their own authorized sessions.
8. **Shared operations:** publish/import guidance/experts from admin and confirm both clients; create support tickets from both, update status in admin and read it back; upload/read documents with owner/admin permissions and reject other users.
9. **Hosted functionality:** fresh deep links and refreshes for `/`, `/profile`, journey screens and `/Adminlogin` work; no static-only missing-server-function behavior; password recovery, session refresh and sign-out work on the actual host.
10. **Automatic delivery:** make a harmless approved website update in Lovable, trace source/destination commits, pass `web-app/` checks and confirm the chosen public site deploys that exact revision. Deliberately fail a check and confirm production stays on the last good release.
11. **Native release quality:** Android CI passes build/JVM/device tests; inspect captures at multiple widths/font scales. Produce a signed release APK/AAB with a higher versionCode and verified checksum; test installation/update over the previous build.

### Implementation order

1. Repair/test website account isolation, guest consent, conflicts and reliable saved state; build password recovery; configure and verify buyer auth policy.
2. Deliver the tested website/admin/migration records to the existing repository; establish `web-app/` checks and the Lovable delivery bridge.
3. Configure Vercel (or Hostinger), environment variables, actual domain and Supabase URLs; test staging and production independently.
4. Implement native account/shared-data integration and the administrator operations contract.
5. Run the cross-device/access/offline tests, sign the new Android release, verify public addresses and perform the automatic-deployment check.

## 12. What the owner supplies and what final handover contains

**Owner setup needed:** the chosen hosting account/team, repository access for its Git integration, an actual domain if desired, authorization to manage external Supabase Email settings, an email sender for recovery, a secure Android release-signing/distribution arrangement, and an authorized non-production account testing path. Never paste signing keys, service-role secrets or passwords in chat.

**Final handover must include:**

- Verified public website URL and the same origin's `/Adminlogin` URL; no fabricated domains or preview-as-production labels.
- Existing administrator emails above, their own private passwords/reset procedure, and direct password-only access with verified administrator roles.
- A newly integrated signed APK/AAB, version, source revision, checksum and test report; not the old design-only debug file.
- Verified GitHub source revision, deployment revision, migration records and restoration/runbook instructions.
- Explicitly documented remaining limitations, if any; do not label the product final while cross-device/account tests are unverified.

**Current review address only:** https://id-preview--12e20cb0-845a-4f25-bc32-9f8ec393a7d1.lovable.app

**Current administrator review address only:** https://id-preview--12e20cb0-845a-4f25-bc32-9f8ec393a7d1.lovable.app/Adminlogin

These are not verified live production links and normally require Lovable project access. A shared preview link is temporary review access, not publication. A production URL cannot be supplied until a deployment exists and is checked.

## 13. References checked for this guide

- [TanStack Start hosting](https://tanstack.com/start/latest/docs/framework/react/guide/hosting)
- [Vercel: deploy TanStack Start with Nitro](https://vercel.com/kb/guide/deploy-a-tanstack-start-app-to-vercel)
- [Vercel TanStack Start framework support](https://vercel.com/docs/frameworks/full-stack/tanstack-start)
- [Vercel Hobby plan guidance](https://vercel.com/docs/plans/hobby)
- [Hostinger Node Git deployments](https://docs.hostinger.com/node.js/git-deployments)
- [Hostinger Node build settings](https://docs.hostinger.com/node.js/build-settings)
- [Hostinger supported frameworks/Other](https://docs.hostinger.com/node.js/overview-1)
- [Nitro Node deployment](https://nitro.build/deploy/node)
- [Lovable self-hosting](https://docs.lovable.dev/tips-tricks/self-hosting)
- [Lovable Git sync](https://docs.lovable.dev/integrations/git-sync-overview)
- [Supabase password authentication](https://supabase.com/docs/guides/auth/passwords)

Provider limits, pricing and supported runtimes can change. Verify the selected account's settings during setup; examples in this guide are not an executed deployment configuration.