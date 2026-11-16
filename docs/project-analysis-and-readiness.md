# FINNEST — project analysis and readiness report

_Date: 2026-11-16. Based on an actual read of the repository, plus real builds of
both web applications (`web` and `web-app`) inside the deployment sandbox._

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
| Will the admin panel work? | **Yes**, for guidance, experts, support tickets and audit log. |
| Will admin content reach the website? | **Yes.** Both read published rows from Supabase. |
| Will admin content reach Android? | **Yes**, for published guidance (Android reads it read-only). |
| Will a customer's journey draft sync between web and Android? | **No. Not implemented.** |
| Can Android users sign in? | **No. Not implemented.** |
| Can the admin panel manage buyer data, forms, projects or experts' enquiries? | **No.** Only content + support tickets today. |

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
