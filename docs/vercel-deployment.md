# Deploying FINNEST to Vercel — step by step

This guide is written for someone who is not a deployment engineer. Follow the
steps in order. Every button name below is exactly what Vercel shows on screen.

---

## 1. First, understand what is in this repository

Your repository contains **three separate applications** plus the database
migrations. They do **not** all go to Vercel.

| Folder | What it is | Where it must be deployed |
|---|---|---|
| `web-app/` | The main FINNEST app (TanStack Start). Customer website **and** the admin console. | **Vercel** — this is your production app |
| `web/` | An earlier, simpler copy of the website (plain React + Vite). Same logic, fewer screens, basic admin editor. | **Vercel** — optional, only if you want a second site |
| `android/` | The native Android app (Kotlin). | **NOT Vercel.** Use Firebase App Distribution / Google Play (see `docs/android-release.md`) |
| `supabase/` | SQL migrations + row-level-security tests for your database. | Already applied to your Supabase project. Nothing to deploy. |

### Which one is the "real" app?

`web-app/` is the newer and richer version. Compare:

| Feature | `web/` (older) | `web-app/` (use this one) |
|---|---|---|
| Customer website (dashboard, journey, projects, visit notes, EMI guide, legal & financial due diligence, resale, experts, profile, reminders) | ✅ | ✅ |
| Guidance library (`/guidance`) reading from Supabase | ✅ | ✅ |
| Admin panel (`/Adminlogin`) | ✅ guidance editor only | ✅ **full console** |
| Admin: publish experts (shows on `/experts` in app + Android) | ❌ | ✅ |
| Admin: read & reply to user support tickets | ❌ | ✅ |
| Admin: bulk-import guidance / experts from CSV or Excel | ❌ | ✅ |
| Admin: activity / audit log | ✅ | ✅ |
| Admin: MFA (authenticator app) required | ✅ | ✅ |
| Server-side bulk import (needs a server, so SSR is required) | ❌ | ✅ |
| Modern UI kit (Tailwind + shadcn), images, roadmap artwork | ❌ | ✅ |

**Decision: deploy `web-app/` as your production app.** It contains both the
public website and the admin panel in one deployment, which is exactly what you
asked for ("admin panel hosts and manages all backend info which is shown in the
web and Android app as well").

> In the Vercel screen you screenshotted, Vercel found both apps. **Choose
> `web-app` → "Import single project".** You can always add `web` later as a
> second Vercel project.

### How the three pieces talk to each other

```
   web-app (Vercel, SSR)                android (APK)
        │      │                             │
        │      └── reads published guidance ─┘
        │                                     (public Supabase key, read-only)
        │
        └── writes guidance / experts / tickets ──▶ Supabase (single source of truth)
                                                          │
                                                   web (Vercel, static) also reads
```

Supabase is the shared brain. The admin panel writes to it; the website and the
Android app read from it. That is why the admin panel and the apps stay in sync
without any extra sync code.

---

## 2. What you need before you start

1. A Vercel account — <https://vercel.com/signup> (free tier is fine to start).
2. Your GitHub account (already connected, since the repo is `rutwik-ui/FINNEST`).
3. Access to your Supabase project — <https://supabase.com/dashboard> (already
   connected, project `wyjjzynjoorjmpffndzu`).
4. The **publishable** Supabase key. It is already in the repo:
   `sb_publishable_Nzb2UydbWvi6wJTtdt0_oA_pRuAWfWf`
   and the project URL `https://wyjjzynjoorjmpffndzu.supabase.co`.
   > This is a *publishable* key. It is safe in a browser. Never use a
   > `service_role` / `secret` key in the website.

---

## 3. The Vercel configuration is already in the repository

You do not need to write any config by hand. The following files are already
committed and pushed:

| File | Purpose |
|---|---|
| `web-app/vercel.json` | `"framework": "tanstack-start"` + Bun install/build. With this preset Vercel knows to consume the Nitro build output automatically — **no Output Directory needed.** |
| `web-app/vite.config.ts` | Pins Nitro to the **Vercel** preset so the build produces a real server function (this is the fix for the "404 on every page" problem) |
| `web-app/.env.example` | The exact list of environment variables, with notes on which are public and which are server-only |
| `web/vercel.json` | Vite SPA settings + the rewrite rule so deep links like `/guidance` and `/Adminlogin` work on refresh |

### What the build actually produces (I verified this locally)

```
web-app/.vercel/output/
├── config.json                       # Vercel routing rules
├── static/                           # CSS, JS, images served from the CDN
│   └── assets/…
└── functions/
    └── __server.func/
        ├── index.mjs                 # the SSR serverless function
        └── .vc-config.json           # runtime: nodejs22.x
```

That `functions/__server.func/` folder is the part that is usually missing and
causes every page to 404. `config.json` routes `/assets/(.*)` to the CDN and
everything else to `/__server`.

**The one thing you must never do:** let Lovable overwrite
`web-app/vite.config.ts` and drop the `nitro: { preset: "vercel" }` block.
Without it the build produces no server function and **every page returns 404**.
See [§9.2](#92-lovable-rewrote-viteconfigts).

---

## 4. Deploy `web-app` (the main app + admin panel)

### Step 4.1 — Create the project

1. Open <https://vercel.com/new>.
2. Find `rutwik-ui/FINNEST` under "Import GitHub Repository" and click **Import**.
3. On the **Configure Project** screen set:

   | Field | Value |
   |---|---|
   | **Project Name** | `finnest` (must be unique on Vercel — add a suffix if taken) |
   | **Root Directory** | `web-app`  ← **the most important field** |
   | **Framework Preset** | `TanStack Start` (if not in the list, pick `Vite` — `vercel.json` overrides it) |
   | **Install Command** | `bun install` |
   | **Build Command** | `bun run build` |
   | **Output Directory** | **leave empty / default** |

   > **Do not type `.vercel/output` into Output Directory.** The official
   > TanStack Start preset already knows to read the Nitro Build Output API
   > bundle. Typing a path there *overrides* the preset and is one of the most
   > common causes of "No Output Directory found" errors in Lovable projects.

4. Click **Deploy**.
5. Wait for the build (2–5 minutes). When it finishes you get a URL like
   `https://finnest.vercel.app`.

> **Why `Root Directory: web-app`?** Your repo is a monorepo (several apps in
> one repository). Vercel needs to know which folder is the app. Because the
> Root Directory is `web-app`, Vercel reads `web-app/vercel.json` automatically.

### Step 4.2 — Add the environment variables

1. In the Vercel project, go to **Settings → Environment Variables**.
2. Add these five (tick both **Production** and **Preview** for each):

   | Key | Value | Needed by |
   |---|---|---|
   | `VITE_SUPABASE_URL` | `https://wyjjzynjoorjmpffndzu.supabase.co` | browser |
   | `VITE_SUPABASE_PUBLISHABLE_KEY` | `sb_publishable_Nzb2UydbWvi6wJTtdt0_oA_pRuAWfWf` | browser |
   | `SUPABASE_URL` | `https://wyjjzynjoorjmpffndzu.supabase.co` | server (bulk import) |
   | `SUPABASE_PUBLISHABLE_KEY` | `sb_publishable_Nzb2UydbWvi6wJTtdt0_oA_pRuAWfWf` | server |
   | `SUPABASE_SERVICE_ROLE_KEY` | **see the warning below** | server (bulk import) |

3. Click **Save**.

> **The fifth one is not optional if you use bulk import.**
> The CSV/Excel bulk importer in the admin console runs on the server and bypasses
> row-level security, so it needs the service-role key. Without it the importer
> throws "Missing Supabase environment variable(s)" and nothing imports. Get it
> from Supabase → **Project Settings → API Keys → secret keys**.
>
> **This key can read every row in your database, ignoring all RLS rules.** Two
> rules that matter:
> - Never put it in a `VITE_*` variable. Those are compiled into the JavaScript
>   that every visitor downloads. A service key in a `VITE_*` variable would hand
>   strangers full read/write access to your database.
> - If it is ever pasted into a chat, a screenshot, an email or a public repo,
>   rotate it in Supabase immediately. Treat it like your bank password.
>
> If you do not want a key that powerful in your host, skip bulk import: leave
> this variable unset and add projects, guidance and experts through the normal
> admin forms, which use the signed-in user's own permissions.
4. Go to the **Deployments** tab → three dots on the top deployment →
   **Redeploy**.

> **Why "redeploy" and not just "save"?**
> `VITE_*` variables are compiled *into* the JavaScript bundle at build time.
> The old bundle does not contain them. A rebuild is required.

### Step 4.3 — Tell Supabase about your new domain (do this or login breaks)

Until you do this, sign-in and email confirmation will not work.

**Your project is currently set to `http://localhost:3000`.** While it stays that
way, the "forgot password" and "confirm your email" links in the emails Supabase
sends will point at your own computer and will not open on a phone. This is the
single most common reason people think sign-up is broken. Fix it in the steps
below.

1. Open <https://supabase.com/dashboard> → your project → **Authentication** in
   the left sidebar → **URL Configuration**.
2. **Site URL**: set it to your live domain, e.g. `https://finnest.vercel.app`.
3. **Redirect URLs**: add these lines:
   ```
   https://finnest.vercel.app/**
   https://*.vercel.app/**
   ```
   (plus your own custom domain later, e.g. `https://finnest.in/**`).
4. Click **Save**.

> **Android note:** the Android app talks to Supabase over its API and does not
> need a redirect URL for email/password sign-in or for password reset by code.
> It only needs the Site URL above to be correct for the *web* links.

### Step 4.4 — Add your own domain (recommended)

1. In Vercel: **Settings → Domains** → type your domain (e.g. `finnest.in`) →
   **Add**.
2. Vercel shows you the DNS records. Add them at your domain registrar
   (GoDaddy / Namecheap / Google Domains / Hostinger …).
3. Wait 5–30 minutes. Vercel shows a green tick when the certificate is issued.
4. Come back to Supabase **Authentication → URL Configuration** and add your
   custom domain to the **Redirect URLs** and set the **Site URL**.

### Step 4.5 — Check that everything works

Open your URL and test these:

| URL | Expected |
|---|---|
| `/` | Dashboard / home page loads, no error screen |
| `/guidance` | Guidance list loads (reads from Supabase) |
| `/experts` | Experts page loads |
| `/Adminlogin` | Admin login screen (sign in with an admin account) |
| `/Adminlogin` → after login | Admin console opens: guidance, experts, tickets, import, activity |

> **A quick sanity check before you even deploy:** you can build it locally.
> ```sh
> cd web-app
> bun install
> bun run build          # must create .vercel/output/
> ls .vercel/output      # config.json  functions/  static/
> ```

---

## 5. Deploy `web` (optional — the older plain site)

Only do this if you want the older site live on its own domain.

1. <https://vercel.com/new> → import `rutwik-ui/FINNEST` again.
2. Project name: `finnest-web`.
3. **Root Directory**: `web`.
4. Framework Preset: `Vite` · Install: `npm ci` · Build: `npm run build` ·
   Output: `dist`.
5. Add the two environment variables
   `VITE_SUPABASE_URL` and `VITE_SUPABASE_PUBLISHABLE_KEY`, then redeploy.
6. Add the new domain to Supabase redirect URLs (same as step 4.3).

---

## 6. The admin panel: signing in and granting access

The admin panel lives at `/Adminlogin` on the deployed site.

- It uses Supabase email + password, and requires a **second factor** (your
  authenticator app) — this is enforced by the database, not just the UI.
- Two super administrators already exist in the database
  (`public.finnest_admin_roles`). Sign in with one of those accounts.
- To give a colleague access:
  1. They sign up on your site and confirm their email.
  2. Open Supabase → **SQL Editor** and run:
     ```sql
     insert into public.finnest_admin_roles (user_id, role)
     values (
       '<paste-their-auth-users-uuid-here>',
       'content_manager'
     )
     on conflict do nothing;
     ```
     Their UUID: Supabase → **Authentication → Users** → click the user → copy
     the `id` in the JSON payload.
  3. Give them the authenticator-app code when they enrol.
- Roles available: `super_admin`, `content_manager`, `support_manager`,
  `expert_manager`.

> Do **not** put the service-role key anywhere in the browser or in a
> `VITE_` variable. The database's row-level security is the real guard.

---

## 7. The Android app — it cannot live on Vercel

Be clear about this: **Vercel hosts websites.** An Android app is an APK that
must be installed on a phone. Vercel cannot build, sign or distribute Android
apps.

What you already have: `.github/workflows/android-debug.yml` builds a debug APK
on every push to `android/` and you can download it from the **Actions** tab.

What I added for you: `.github/workflows/android-release.yml`, which builds a
**signed release APK** and (optionally) publishes it to Firebase App
Distribution. Full instructions are in [`docs/android-release.md`](./android-release.md).

Short version:

1. **Now (for you and testers):** Actions tab → *Android debug APK* → download
   the `FINNEST-android-debug` artifact → unzip → `app-debug.apk` → install on
   the phone.
2. **For real users (Play Store):** one-time setup of a Google Play developer
   account ($25, one-off) → upload an Android App Bundle (`.aab`) → review →
   publish. See `docs/android-release.md`.
3. **To host the APK for download:** you can put the APK file inside
   `web-app/public/` and link to it — Vercel will serve any file in `public/`
   as a static asset. (Keep the file size reasonable; a signed release APK is
   usually 5–15 MB.)

---

## 8. Day-to-day management on Vercel

### 8.1 How updates reach Vercel automatically

```
Lovable  ──push──▶  GitHub  ──webhook──▶  Vercel  ──build──▶  live site
   ▲                                    │
   └────────────── sync back ◀──────────┘
```

- Change something in **Lovable** → Lovable commits to GitHub → Vercel builds
  automatically → your site updates. **Nothing for you to click.**
- Change something **directly in this repository** → push to `main` → Vercel
  builds automatically.
- The only manual step is **redeploy** when you change an environment variable.

### 8.2 The Vercel dashboard, explained

| Screen | What it is for |
|---|---|
| **Deployments** | Every build, newest first. Green = live. Click one to see the build log. The three-dot menu has **Redeploy**, **Promote to Production**, **Instant Rollback**. |
| **Analytics** | Visitors, page views, top pages, geography, devices. |
| **Logs** (Observability) | Live server logs + real visitor requests. Use this to debug "it works for me but not for my customer". |
| **Settings → Environment Variables** | Add/change secrets. Remember: redeploy after changing one. |
| **Settings → Domains** | Add custom domains, see DNS records, renew certificates. |
| **Settings → Git** | Which branch auto-deploys (default: `main`), and "Ignore Build Step" for skipping builds. |
| **Settings → Functions** | The server function runtime, memory, and duration limits. |
| **Settings → Deployment Protection** | Optional password/PUT-only protection for previews. |

### 8.3 Environments explained

| Environment | What it is | When it is used |
|---|---|---|
| **Production** | Your live site | Every push to `main` |
| **Preview** | A throwaway copy per branch/PR | Every push to any other branch, and every PR |
| **Development** | Your own laptop | `bun run dev` |

You can add environment variables for only Production, only Preview, or both —
tick the boxes when you add each one.

### 8.4 Rolling back

Deployments → click the good deployment → **⋯ → Promote to Production**. The bad
build is replaced in seconds, no code change needed. This is your single most
useful button when something breaks.

### 8.5 Branch protection (recommended)

On GitHub, protect `main` so nobody can push a broken build straight to
production: **Settings → Branches → Add rule** → Branch name `main` → enable
**"Require a pull request before merging"**. Then every change goes through a
Preview deploy first, you test it, and then you merge.

### 8.6 Custom domain vs the free `.vercel.app` URL

- `finnest.vercel.app` — free, permanent, works immediately.
- `finnest.in` — costs money (₹700–1500/yr for a `.in`, more for `.com`), looks
  professional, needs DNS setup (~30 minutes).

Do both: keep the `.vercel.app` URL as a fallback.

---

## 9. Troubleshooting

### 9.1 Every page shows "Page not found" / 404

**Cause:** the build did not produce a server function — almost always because
`nitro: { preset: "vercel" }` is missing from `web-app/vite.config.ts`.

**Fix:** re-add that block (see §3), commit, redeploy.

### 9.2 Lovable rewrote `vite.config.ts`

If Lovable regenerates `web-app/vite.config.ts` and the `nitro` block is gone,
the next Vercel build will produce a static-only site and every page 404s.

**Fix:** re-add this to `web-app/vite.config.ts` (it is safe to paste back):

```ts
export default defineConfig({
  tanstackStart: {
    server: { entry: "server" },
  },
  nitro: {
    preset: "vercel",
  },
});
```

**Extra safety net:** in Vercel **Settings → Environment Variables**, add
`NITRO_PRESET` = `vercel`. It is ignored on Lovable's side and acts as a
backup if the config file is ever overwritten again.

### 9.3 "No Output Directory named ... found"

**Cause:** an Output Directory was typed into the dashboard, overriding the
TanStack Start preset.

**Fix:** Settings → Build and Deployment → **Output Directory: clear it /
leave it empty** → Redeploy. `web-app/vercel.json` deliberately contains no
`outputDirectory` key.

If Vercel instead complains that it cannot find the framework, force it:
Settings → Build and Deployment → **Framework Preset → TanStack Start**.

### 9.3b `functions/__server.func` missing / works locally but 404 on Vercel

**Cause:** Nitro fell back to the Cloudflare preset. `@lovable.dev/vite-tanstack-config`
defaults Nitro to `cloudflare-module` unless told otherwise, and a Cloudflare
Worker bundle cannot run on Vercel.

**Fix:** make sure `web-app/vite.config.ts` has the `nitro: { preset: "vercel" }`
block, then redeploy. Locally you should see:

```sh
bun run build
cat .vercel/output/nitro.json | grep preset     # → "preset": "vercel"
```

### 9.4 "Command 'bun install' exited with 1"

**Cause 1:** the 24-hour supply-chain guard in `web-app/bunfig.toml`
(`minimumReleaseAge = 86400`) rejects a dependency that was published less than
24 hours ago. **Fix:** wait ~24 hours, or run `bun install` locally after 24
hours and commit the refreshed `bun.lock`.

**Cause 2:** `bun.lock` and `package.json` disagree (e.g. Lovable changed one
but not the other). **Fix:** run `bun install` in `web-app/` locally and commit
the updated `bun.lock`.

### 9.5 The site loads but sign-in fails / "Invalid API key"

**Cause:** environment variables are missing, or they were added *after* the
build.

**Fix:** confirm all four variables exist under Settings → Environment
Variables (for the right environment), then **Redeploy**.

### 9.6 The site loads but data is missing (empty guidance / experts)

**Cause:** the Supabase project the keys point at has no published rows yet, or
the browser is blocked by an ad-blocker.

**Fix:** sign in at `/Adminlogin`, publish one guidance item and one expert,
then refresh `/guidance`. If it still fails, open the browser console (F12) and
look at the Network tab.

### 9.7 `build` fails with out-of-memory

**Fix:** Settings → Environment Variables → add `NODE_OPTIONS` =
`--max-old-space-size=6144`, then redeploy.

---

## 10. Making changes later — the safe workflow

1. **UI or content change in Lovable** → Lovable commits → GitHub → Vercel
   builds → live. You do nothing.

2. **UI or content change directly in this repo** → commit to a branch → push →
   open a Pull Request → Vercel creates a **Preview** URL → you test it →
   merge to `main` → Vercel deploys to production.

3. **A new database table or column** → write a SQL migration in
   `supabase/migrations/` → run it in Supabase → **SQL Editor** → commit the
   file to GitHub. Never edit tables by hand in the Supabase UI without also
   committing the migration — otherwise the next person cannot reproduce it.

4. **Never** commit a `service_role` / secret key. Never put one in a `VITE_`
   variable. `VITE_` variables are visible to every visitor.

---

## 11. A quick checklist before you call it done

- [ ] `web-app` deploys successfully to Vercel and `/` loads
- [ ] `/guidance`, `/experts`, `/journey`, `/profile` all load (no 404, no error)
- [ ] All four environment variables are set for Production **and** Preview
- [ ] Supabase **Site URL** + **Redirect URLs** contain your live domain
- [ ] You can sign in at `/Adminlogin` with an admin account and the MFA prompt
- [ ] You can publish a guidance item and see it on `/guidance`
- [ ] You can publish an expert and see it on `/experts`
- [ ] A support ticket sent from the app appears in the admin console
- [ ] Custom domain added (optional but recommended)
- [ ] `main` branch protected on GitHub
- [ ] Android APK installable on a real phone
