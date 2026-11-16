# FINNEST — project analysis and readiness report

_Date: 2026-11-16. Based on an actual read of the repository, plus real builds of
both web applications (`web` and `web-app`), a real PostgreSQL run of every
migration and policy test, and the Android build + emulator suites on CI._

> **Update:** the gaps listed in §1 and §3 have since been implemented. See
> [§9 Implemented since this report](#9-implemented-since-this-report) for what
> changed, and treat §3 as the historical "before" picture.

---

## 1. The short version

**You have a good-looking, genuinely useful product shell. It is deployable today
as a website with an admin panel. It is _not_ yet a fully connected
web + Android + admin product, because the shared buyer-data layer does not
exist.**

Read that sentence twice, because it is the difference between "deployed" and
"usable by a real customer". Deploying to Vercel is the easy half. The hard half
is the shared data model.

| Question | Answer |
|---|---|
| Can I deploy to Vercel today? | **Yes.** Both `web` and `web-app` build successfully. |
| Will the admin panel work? | **Yes** — guidance, projects, experts, enquiries, support tickets, buyer journeys, audit log. |
| Will admin content reach the website? | **Yes**, including project listings, with realtime updates. |
| Will admin content reach Android? | **Yes** — guidance and the project catalogue. |
| Will a customer's journey draft sync between web and Android? | **Yes.** Per-field sync in both apps. |
| Can Android users sign in? | **Yes.** Email + password, sessions in the Android Keystore. |
| Can the admin panel manage buyer data, forms, projects or enquiries? | **Yes** for projects, enquiries and a read-only buyer-journey view. |

**What is still genuinely missing:** OS-level notifications on Android, the
remaining Resale screens, expert referral/messaging, comparison detail fields,
and the release-hardening items in §6 Phase E.

---

## 2. What actually exists

### 2.1 Repository layout

```
FINNEST/
├── web/           React 18 + Vite 5 — older SPA. Same product, fewer screens.
├── web-app/       React 19 + TanStack Start — the modern app. Website + admin console.
├── android/       Kotlin/XML native app. Room for local data.
├── supabase/      SQL migrations + disposable RLS test suite.
├── docs/          Analysis, phase plan, requirements traceability, screen reviews.
└── .github/       CI: website checks, Android APK, Android emulator, Supabase policy tests.
```

`web/` and `web-app/` are **not independent products** — they share the same
business logic. I diffed the shared modules (`projects.js`, `visit.js`,
`localDraft.js`, `resale.js`, `admin.js`, `newJourney.js`, `relatedChecks.js`,
`adminAudit.js`, `adminGuidance.js`, `common.js`):

> **All ten shared modules are byte-identical between the two apps.**

The difference is the shell: `web-app` adds a Tailwind/shadcn UI kit, real
imagery, the full admin console (experts, support tickets, bulk CSV/Excel
import), server-side functions, and SSR. `web` has a basic admin editor only.

**Recommendation: treat `web-app/` as the product. Keep `web/` only if you want
a lightweight fast-loading variant on a second domain — otherwise archive it.**
Maintaining two near-identical codebases is how bugs get fixed in one and not
the other.

### 2.2 Supabase schema (what is genuinely centralised)

Seven tables exist, all with row-level security enabled:

| Table | Purpose | Who can read | Who can write |
|---|---|---|---|
| `finnest_guidance` | Guidance articles (new property / resale / general) | `published` rows: public. Drafts: content editors | content_manager, super_admin (MFA required) |
| `finnest_experts` | Expert directory | `published` rows: public | expert_manager, super_admin |
| `finnest_profiles` | User display profile | Own row + admins | Own row |
| `finnest_user_drafts` | **Private** user journey drafts | Own row only | Own row only (auth required) |
| `finnest_support_tickets` | Support requests | Own rows + support_manager | User inserts; support_manager updates status |
| `finnest_admin_roles` | Who is an admin, and which role | Own row + super_admin | super_admin only |
| `finnest_admin_audit` | Admin action log | content_manager, super_admin | server-side only |

Plus a **private** storage bucket for user draft files.

This is genuinely well-designed security: RLS is on every table, admin writes
require `aal2` (i.e. the authenticator second factor), the service-role key is
never in the browser, and there is a disposable RLS test suite that creates two
users and proves they cannot see each other's rows.

**This part is production-grade and you should be proud of it.**

### 2.3 What the admin panel can actually do

Sections in `web-app`'s `AdminConsole`, gated by role and enforced by RLS:

| Section | Allowed roles | Capability |
|---|---|---|
| Overview | all admins | Counts, recent activity |
| Guidance content | super_admin, content_manager | Create / edit / publish / delete articles + **bulk CSV/Excel import** |
| Users & roles | all admins | Read user profiles, see roles |
| Experts | super_admin, expert_manager | Add / publish / remove experts |
| Support requests | super_admin, support_manager | Read tickets, change status |

Cross-device effect: publishing guidance → visible on the **website** `/guidance`
and on **Android**. Publishing an expert → visible on the **website** `/experts`.

### 2.4 What the Android app can actually do

Native Kotlin. Reads `finnest_guidance` over Supabase REST with the publishable
key (`android/app/build.gradle.kts` → `GUIDANCE_URL`, `GUIDANCE_PUBLISHABLE_KEY`).
Everything else — journey drafts, requirements, visit notes, reminders, profile —
lives in a **local Room database** on the device.

So: no sign-in, no cloud sync, no cross-device continuity. The app is a
**read-only consumer** of admin content, plus a fully local notebook.

### 2.5 What the website can actually do

All customer screens are present and functional: dashboard, 8-step journey,
projects shortlist, visit observations, home-loan/EMI guide, legal due
diligence, financial due diligence, final review, resale track, experts,
profile, reminders, privacy, about, concern form, guidance, support form.

But the **journey draft is stored in browser `localStorage`**. It is genuinely
robust (conflict detection, quota handling, corrupt-data recovery, cross-tab
sync) — but it is device-bound. Clear the browser → the data is gone. Open on
your phone → the data is not there.

---

## 3. The gap that matters most

You described the product as:

> "both the web and android app has to be connected in the form of realtime data
> and functionality and integrated with each other... admin panel manages all the
> backend info which has to be shown in the web and android app as well."

Here is an honest scorecard against that goal:

| Requirement | Status | Effort to close |
|---|---|---|
| One centralised Supabase database | ✅ Done | — |
| Admin manages content shown on web | ✅ Done (guidance, experts) | — |
| Admin manages content shown on Android | ✅ Done (guidance) | — |
| Admin panel is protected by roles + MFA + RLS | ✅ Done | — |
| Android reads live published content | ✅ Done | — |
| **Customer journey data shared across web and Android** | ❌ **Missing** | **2–4 weeks** |
| **Android sign-in** | ❌ **Missing** | **1–2 weeks** |
| **Realtime push (no refresh needed)** | ❌ **Missing** | **3–5 days** |
| Admin manages buyer enquiries / projects / all users | ❌ Missing | 1–2 weeks |
| Offline-first editing with conflict resolution | ❌ Missing | 2–3 weeks |

The table `finnest_user_drafts` already exists with correct private-only RLS —
someone designed the solution but the app does not call it yet. That is your
cheapest starting point.

### What "real-time, integrated" requires concretely

1. **Android sign-in** — Supabase Auth (email + password, or Google/phone
   OTP). Store the session in encrypted storage.
2. **Move the journey draft from `localStorage` to Supabase** — a
   `user_id`-owned row in `finnest_user_drafts` (or normalised tables: projects,
   visit notes, tasks). Keep `localStorage` as an offline cache.
3. **Realtime subscription** — `supabase.channel('drafts').on('postgres_changes', …)`
   so an edit on the phone appears on the laptop without refreshing. Realtime is
   free for reasonable volumes and needs no extra service.
4. **Conflict handling** — last-write-wins with an `updated_at` check is enough
   at first; a per-field merge is the upgrade path.
5. **Extend the admin panel** — read-only views of buyer journeys, enquiry
   inbox, project/builder catalogue.

Until 1–3 are done, **do not tell customers their data follows them across
devices.** That would be a trust-destroying bug in a product that handles
property purchase decisions.

---

## 4. Errors, inconsistencies and UI issues found

### 4.1 Real functional issues

| # | Where | Issue | Severity |
|---|---|---|---|
| 1 | `android/app/build.gradle.kts` | `SUPABASE_URL` and `GUIDANCE_PUBLISHABLE_KEY` are hard-coded in the source. That is *acceptable* for a publishable key, but it means changing projects requires a code change + rebuild. | Low |
| 2 | `android/app/build.gradle.kts` | `versionCode = 4`, `versionName = "0.4.0"` — the app is presented as v0.4, not v1. Must be bumped for every Play Store upload or Play rejects the upload. | Medium |
| 3 | `web-app` `/Adminlogin` + `/admin*` | Admin routes are reachable on the **same public domain** as the customer site. Security is correct (RLS + MFA), but it is better UX and better optics to put the admin panel on `admin.yourdomain.com`. | Medium |
| 4 | `web-app` | Admin panel has **no** view of buyer journeys, projects or enquiries. You said the admin should manage "all backend info". Today it manages content only. | **High (product)** |
| 5 | `android/` | No sign-in, therefore no way to associate a device with an account. | **High** |
| 6 | `web/` vs `web-app/` | Two apps, identical business logic, drifting presentation. Every bug must be fixed twice. | Medium |
| 7 | `supabase/migrations/20261110150900_add_second_super_admin.sql` | Hard-codes a specific `auth.users` UUID for a second super admin. Fine for you; will silently fail on any fresh project. | Low |
| 8 | `vite.config.ts` (web-app) | Before this change, Nitro defaulted to the **Cloudflare** preset → the Vercel build emitted a Worker bundle → every route 404s. **Fixed in this branch.** | **Critical, resolved** |

### 4.2 UI / UX observations

- **First-run clarity.** A new visitor lands on a dashboard that assumes they
  already chose a journey ("New Property" vs "Resale Property"). Make the choice
  modal the real first screen for a first-time visitor.
- **"Resale Property screen" / "New Property screen" placeholder pages.** When a
  user is on the wrong track they get a bare page with one button. That reads as
  a bug. Replace with a friendly "This step belongs to the other journey" panel
  showing what *is* available.
- **Save-state messaging.** Drafts save to `localStorage` only. The UI should say
  "Saved in this browser only" rather than "Saved", or users will assume it
  follows them to another device.
- **Admin on a phone.** The admin console is designed for desktop. On a phone
  the tables overflow. Not blocking, but expect complaints if you administer from
  your phone.
- **No empty states for a freshly deployed database.** Before you publish the
  first guidance article, `/guidance` renders nothing. Seed 3–5 articles and 2–3
  experts before sharing the link with anyone.

---

## 5. Verified build status

I did not assume the builds work — I ran them.

| App | Command | Result |
|---|---|---|
| `web-app` | `bun install` → `bun run build` (with `VERCEL=1`) | ✅ `.vercel/output/config.json`, `static/`, `functions/__server.func/index.mjs` generated. Nitro preset `vercel`, runtime `nodejs22.x`. |
| `web-app` | Served the built output and requested `/`, `/guidance`, `/Adminlogin`, `/journey`, `/experts` | ✅ All returned HTTP 200 with HTML; CSS/JS assets served correctly. |
| `web` | `npm ci` → `npm run build` | ✅ `dist/index.html` + assets, 259 kB JS / 19 kB CSS. |

This matters: it means the Vercel build has a known-good target and the runtime
limitation you would otherwise hit ("404 everywhere") is already fixed in this
branch.

---

## 6. Recommended path, in order

**Phase A — Ship the website (this week)**
Deploy `web-app` to Vercel (§4 of `docs/vercel-deployment.md`). Set Supabase
Auth URLs. Seed real guidance + experts. Share the link. This gives you a real,
usable product for customers today.

**Phase B — Get the Android app into testers' hands (this week)**
Install the CI debug APK. Then build a signed release APK
(`docs/android-release.md`). Testers can use the journey notebooks and read
published guidance.

**Phase C — Fix the trust gap (2–4 weeks, highest value)**
Android sign-in → move journey drafts into `finnest_user_drafts` → subscribe to
realtime → keep `localStorage` as the offline cache. After this, "your data
follows you" becomes true.

**Phase D — Make the admin panel complete (1–2 weeks)**
Buyer journey viewer, enquiry inbox, project/builder catalogue management.

**Phase E — Release hardening**
Two-user RLS proof on the live project, MFA recovery process, backups,
monitoring/uptime alerts, capacity test, then Play Store submission.

---

## 7. Cost reality check

| Item | Cost |
|---|---|
| Vercel Hobby (1 project, personal) | Free. **Commercial use technically requires Pro.** |
| Vercel Pro | ~USD 20/user/month — take this once you have paying users |
| Supabase Free | Free — pauses after 1 week of inactivity on free tier |
| Supabase Pro | ~USD 25/month — **needed for a real product** (no pausing, backups) |
| Domain (`.in`) | ~₹800–1500 / year |
| Google Play developer account | USD 25 one-off |
| Firebase App Distribution | Free |

Minimum sensible production spend: **~USD 45/month** (Vercel Pro + Supabase Pro)
plus a domain.

> ⚠️ **The free Supabase tier pauses your database after a week of no activity.**
> If you deploy and then do not touch it for a few days, the whole app will show
> empty data and you will think you broke it. Upgrade before sharing with real
> customers, or open the app regularly during the trial.

---

## 8. What to tell customers (honesty matters here)

You are helping people make a property purchase — the largest financial decision
most of them will make. Be precise about what the app does:

- ✅ "We help you organise your due diligence and do not miss checks."
- ✅ "Your progress is saved **on this device**." (until Phase C is done)
- ✅ "Our guidance library is maintained by our team and updates automatically."
- ❌ Do not say "your data syncs across all your devices" — it does not yet.
- ❌ Do not say "we are a legal/financial advisor" — the UI already carries the
  right disclaimers; keep them.

---

## 9. Implemented since this report

This section replaces §3's "not implemented" rows. Everything here is merged on
the `arena/408f145c-finnest` branch and verified by CI.

### 9.1 The sync protocol (the important design decision)

The website stored a journey draft as **one nested object** in `localStorage`.
Android stored the same journey as **flat `Map<String, String>` keys** in Room.
Those shapes cannot be reconciled as documents — a whole-document
last-write-wins would silently destroy the other device's work.

So the shared unit is **one answer**:

```
finnest_draft_fields(user_id, journey_type, field_key, value, client_updated_at)
PRIMARY KEY (user_id, journey_type, field_key)
```

- Editing the budget on a phone and visit notes on a laptop keeps **both**.
- A `BEFORE UPDATE` trigger drops any write whose `client_updated_at` is older
  than the stored one, so a slow offline device cannot overwrite newer work.
- Each client uploads **only the fields it actually changed**, so two devices
  never fight over fields neither of them touched.
- The website converts its nested draft to these keys loss-free
  (`web-app/src/finnest/draftSync.js`, 19 unit tests), including an envelope for
  fields it does not recognise, so a future field is never silently dropped.

### 9.2 What changed, per surface

| Area | Before | Now |
|---|---|---|
| Android sign-in | none | Email/password via Supabase Auth; tokens encrypted with an AES-256-GCM key in the Android Keystore (no extra dependency); automatic refresh; password recovery by email |
| Android sync | read-only guidance | Per-field push on every save + reconcile on dashboard open + token-refresh retry; distinguishes offline from failure |
| Android catalogue | none | Admin-published projects on the Projects screen, with "add to shortlist" |
| Web sync | `localStorage` only | Debounced mirror to the account, realtime subscription for edits from other devices, flushes on tab hide, online/offline handling |
| Web honesty | "Saved" | Status chip + Profile panel that says *this browser only* until an account exists |
| Admin → content | guidance, experts, support | **+ projects catalogue, buyer enquiries, read-only buyer journeys** |
| Customer → admin | support ticket | **+ enquiry with an explicit consent flag** |
| Realtime | none | `finnest_draft_fields`, `finnest_projects`, `finnest_enquiries`, `finnest_guidance`, `finnest_experts` |

### 9.3 Two pre-existing bugs found and fixed

1. **Migrations could not build a fresh database.** Two migrations inserted
   hard-coded production `auth.users` ids into `finnest_admin_roles`. On any new
   project those rows violate the foreign key, and every later migration aborts.
   They are now guarded inserts that are a no-op when the account does not exist.
2. **The CI policy-test job was failing on `main`.** The disposable shim never
   created the `service_role` that every migration grants to, so the migration
   loop died on the fourth file. The shim now mirrors that role.

A third fix was a genuine test bug: an assertion inferred "RLS blocked this
delete" from an exception, but the policy *allows* a super admin to delete other
people's roles and simply excludes their own — so a bare `DELETE` correctly
affects zero rows without raising. The test now asserts that contract directly.

### 9.4 Verified by CI, not by assumption

| Check | Result |
|---|---|
| `Website checks` (lint, typecheck, unit tests, build) | ✅ |
| `Database policy tests` (shim + 8 migrations + 5 RLS suites on PostgreSQL 15) | ✅ |
| `Android debug APK` (Gradle compile + JVM unit tests) | ✅ |
| `Android emulator tests` (instrumentation, incl. new catalogue/auth suites) | ✅ |
| `web-app` local build → Vercel Build Output with `preset: "vercel"` | ✅ |
| Every route served from the production bundle (`/`, `/projects`, `/experts`, `/profile`, `/Adminlogin`) | ✅ HTTP 200 |
| Migration set applied to a fresh PostgreSQL 18 (PGlite) | ✅ |

### 9.5 What a buyer can now do that they could not before

1. Start a journey on the laptop without an account; sign in later and keep it.
2. Sign in on Android and see the same journey they started on the web.
3. Edit the shortlist on the phone while the laptop has the visit notes open;
   both survive.
4. See a project the FINNEST team published minutes ago, without reinstalling.
5. Ask FINNEST to call them, and control whether their phone number is shared.
6. Read, on both apps, exactly where their data lives — device or account.

### 9.5b A bug found by testing the two apps against each other

Kept here because it is the clearest example of why "it builds and the tests pass"
is not the same as "it works".

The sync protocol was correct: one row per answer, per-field merge, stale writes
rejected by the database. Both apps passed their own tests. But the two apps had
each chosen a different name for the same answer:

| Question | Website stored | Android stored |
|---|---|---|
| Down payment | `budget.down` | `down_payment` |
| Stamp duty | `budget.stamp` | `stamp_duty` |
| Carpet area | `budget.carpet` | `carpet_area` |
| Accessibility needs | `budget.access` | `accessibility_needs` |

Both names were valid, nothing threw an error, and every test passed. The effect
would have been that a buyer typed a down payment on their phone, opened the
laptop, and found a blank box — while the app cheerfully said the journey was
synced.

This is now fixed by a single shared alias table. Both sides carry a test that
reads the *other* language's source file and fails if the two tables ever drift,
so the mistake cannot come back quietly. Rows already written with the old
Android spelling still load, because reads canonicalise the key first.

**The lesson for the rest of this project:** the only test that proves two apps
interoperate is one that runs both. Everything else is per-app confidence.

### 9.6 Still outstanding before a public launch

- OS-level reminder notifications on Android (currently in-app dated notes).
- The remaining Resale journey screens and comparison detail fields.
- A real two-user MFA/isolation test against a staging Supabase project.
- Supabase Auth production URLs, backups, and monitoring/uptime alerts.
- Play Store listing, data-safety declaration, and a privacy policy URL.
- **Supabase's free tier pauses after a week idle** — upgrade before sharing the
  link with real buyers, or the whole app will look empty.

### 9.7 Applied to the live project on 2026-11-16

The sync migration was applied to `wyjjzynjoorjmpffndzu` and verified against the
real database, not just the disposable harness:

| Live check | Result |
|---|---|
| `finnest_draft_fields`, `finnest_projects`, `finnest_enquiries` created | ✅ all present, RLS enabled |
| RLS policies in place (5 / 5 / 4) | ✅ |
| Realtime publication contains all five app tables | ✅ |
| `anon` has no privileges on any app table | ✅ |
| A second signed-in user cannot read the first user's answers | ✅ blocked |
| A second user cannot write a row owned by the first | ✅ blocked |
| A newer edit wins; a stale offline write is dropped | ✅ merge guard works |
| Test rows cleaned up afterwards | ✅ zero left |

**Also fixed while touching production:** three tables (`finnest_journeys`,
`finnest_documents`, `finnest_activity`) existed only in production, created by
Lovable's editor on 2026-11-12 and never committed. Rebuilding from the repo
produced a different database. They are now in
`20261112132646_finnest_lovable_tables_reconcile.sql`, and their grants — which
Lovable had left wide open including `TRUNCATE`, a verb that bypasses RLS — now
match the rest of the project. Applied to production; existing rows untouched.

### 9.8 What is still not proven

- **No real two-device test has been run by a human yet.** The isolation and
  merge guarantees are proven at the database level, and both clients are unit
  tested, but nobody has signed in on a phone and a laptop and watched an answer
  appear. That is the next thing to try.
- The Android emulator suite runs the instrumentation tests, but they do not
  exercise a live network round-trip to Supabase.
- Realtime delivery is configured, not observed end-to-end in CI.
- The app has never been used by an actual buyer.
