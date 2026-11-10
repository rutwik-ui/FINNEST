# FINNEST web app — Lovable build (2026-11-10)

This folder holds the web application and administrator console built in Lovable on
top of the existing FINNEST product. It is a port of `web/` onto the TanStack Start
framework, keeping the same screens, journey steps, browser-saved drafts and the
same connected Supabase project.

## What is inside

- `src/finnest/` — the FINNEST screens, journey logic, roadmap definitions,
  administrator console and the CSV / Excel bulk import.
- `src/routes/` — the web routes (one file per address, plus page metadata).
- `src/assets/` — the banner photo and other images used by the app.
- `supabase/migrations/` — the two database changes added for this build
  (experts / support requests / activity log / saved journeys, plus the
  administrator access rules).

## Running it

```text
bun install
bun run dev
```

The app reads the connected Supabase project from `.env`
(`VITE_SUPABASE_URL`, `VITE_SUPABASE_ANON_KEY`).

## Database state at the time of this commit

Connected Supabase project `wyjjzynjoorjmpffndzu`, 7 tables, row level security
enabled on all of them:

`finnest_guidance`, `finnest_experts`, `finnest_profiles`, `finnest_admin_roles`,
`finnest_admin_audit`, `finnest_support_tickets`, `finnest_user_drafts`.

Applied migrations: the three originals plus
`20261110133952`, `20261110134537`, `20261110150816`.

Two accounts exist and both hold `super_admin`:
`rutwik@paarami.com`, `digitalrazorpay@gmail.com`.

Content tables are still empty (no guidance items, experts or support requests
yet), so nothing has been imported.

## Verified / not verified

- Verified in a browser: home page, journey navigation and back button, resale
  roadmap steps, mobile layout without sideways scrolling, administrator sign-in
  gate, banner photo loading.
- Not verified: an actual signed-in administrator session completing an import,
  because this external Supabase project cannot supply a managed test session and
  MFA is required. This needs an authorized administrator to run it.

## Untouched

`web/` (the original React/Vite app), `android/`, the existing CI workflows and
the original three migrations are unchanged by this commit.
