# Phase plan and reference inventory

## Delivered so far
- Native Android entry foundation, introductory screens and journey selection.
- New Property step 1, Requirements & Budget: optional budget fields, location and configuration selections, carpet area and accessibility needs, stored locally in Room.
- Step 3 offers a local shortlist of up to five project names, with rename and removal, on Android and web. Comparison fields and the next step remain unimplemented because the expanded project form is not visible in the supplied screenshot.
- Step 2 developer checks include the six screenshot prompts, optional status, note, source and follow-up date, saved locally. Neither step claims independent verification. Step 4 and step-one priority tiers are not implemented yet.
- The screen 8 screenshot does not expose the expanded status menu. The non-default labels are provisional and use the introductory legend from screen 4; confirm them with the user before treating these as the final prototype options.

## Reference inventory
- New Buy screens 1–5: splash, three introductions, journey selection.
- Screen 6: personalization (name, city, optional family situation, children, elderly parents and first-time buyer). The family dropdown options are not visible in this screenshot; screen not implemented yet.
- Screen 7: initial dashboard with an eight-step New Property roadmap.
- Screen 8: New Property step 2, developer checks.
- Screen 9: New Property step 1, requirements and budget (implemented in this increment, except tiering).
- Screen 10: step 3, shortlist empty state (up to five projects); expanded project form is not shown.
- Screen 11: step 4B, resident feedback notes.
- Screen 12: step 4A, visit observations.
- Screens 13–16 and Resale/common screens 17–32 still need individual review before implementation. The supplied content document describes a 12-step resale tracker while references may group steps differently; resolve mapping during inventory instead of inventing fields.

## Proposed next increments
1. Confirm the expanded developer-check status dropdown and confirm expanded project form fields, and review remaining New Buy references before implementing step 4.
2. Complete missing step-one priority tiers and personalization once their expected options/interaction are confirmed.
3. Continue other New Buy steps in small slices; then review and implement Resale, dashboard and common tabs.
4. Introduce secure authentication and idempotent guest-to-account migration after the data model and authorization policies are reviewed.
5. Add admin, website, notifications, testing and production hardening in later increments.

Supabase inspection found one healthy project, but the public schema had no base tables visible to the connected account. Confirm the intended project/schema before implementing cloud integration. Do not put service-role credentials in the app or modify the live database without a reviewed design.

## Website and admin foundation
- A responsive React/Vite guest-facing website now covers a dashboard, the first two New Property screens, local project names, expert categories, and profile information. Browser drafts are separate from Android Room data.
- `/Adminlogin` deliberately provides no authentication or administrative actions. Secure administrator identity, server-enforced roles, audit logging and management APIs are prerequisites to enabling it.
- The current frontend is not the Laravel website/backend specified as the recommended stack in the brief. The shared backend, API contracts, schema and Android/web synchronization remain to be designed and implemented; nothing has been deployed to Supabase.
