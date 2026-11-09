# Phase plan and reference inventory

## Current Android dashboard increment
- New Property now has a local home dashboard with direct links to each implemented step. Existing users with that journey selected land there on launch; Save & Home and the final review return there only after pending saves succeed. No completion percentage is inferred.
- Reminders are device-local dated notes, accessible from the dashboard. Adding and deleting notes updates one local draft atomically. No OS notifications, notification preferences or cloud sync are implemented.
- Bundled Inter is the default font and Montserrat is used for major headings. These changes improve consistency but are not a claim that every screen is production-polished or visually validated on a device.
- Android 14 emulator CI now runs Room instrumentation and a dashboard → final review → save → dashboard → reopen persistence scenario. This is not full journey, visual, accessibility, real-device or upgrade testing. Dashboard data is local; missing common tabs, profile, resale journey and cloud synchronization remain open. The New Property journey summary now displays local status counts, a checklist filter, project names and direct step links; its statuses are user entries, not verified progress.

## Delivered so far
- Native Android entry foundation, introductory screens and journey selection.
- New Property step 1, Requirements & Budget: optional budget fields, location and configuration selections, carpet area and accessibility needs, stored locally in Room.
- Step 4A provides 14 visit-observation prompts on Android and web, with local status, notes, source and date. Step 4B provides device-local resident feedback notes on Android using a free-text topic; web support is not yet implemented.
- Step 8 provides an Android-only review of saved checklist statuses, notes, project names, assumptions, pending items and locally saved next actions. It does not verify entries or provide expert referrals; device testing is outstanding.
- Step 7 provides Android-only initial and recurring cost estimates, cost prompts and one lender-term record. Totals sum entered amounts only and identify missing inputs; status labels are provisional. Device testing remains outstanding.
- Step 6 provides nine legal due diligence prompts on Android, with locally saved status, notes, source and follow-up date. Status labels are provisional, and no expert referral service is connected.
- Step 5 provides an Android-only illustrative fixed-rate EMI calculator and lender prompts. Input limits and lender-document status labels are explicit implementation choices; status labels are provisional because the prototype menu is closed. No eligibility or approval is implied. Device UI testing remains outstanding.
- Step 3 offers a local shortlist of up to five project names, with rename and removal, on Android and web. Comparison fields remain unimplemented because the expanded project form is not visible in the supplied screenshot.
- Step 2 developer checks include the six screenshot prompts, optional status, note, source and follow-up date, saved locally. Neither step claims independent verification. Step-one priority tiers are not implemented yet.
- The screen 8 screenshot does not expose the expanded status menu. The non-default labels are provisional and use the introductory legend from screen 4; confirm them with the user before treating these as the final prototype options.

## Reference inventory
- New Buy screens 1–5: splash, three introductions, journey selection.
- Screen 6: personalization (name, city, optional family situation, children, elderly parents and first-time buyer). The family dropdown options are not visible in this screenshot; screen not implemented yet.
- Screen 7: initial dashboard with an eight-step New Property roadmap.
- Screen 8: New Property step 2, developer checks.
- Screen 9: New Property step 1, requirements and budget (implemented in this increment, except tiering).
- Screen 10: step 3, shortlist empty state (up to five projects); expanded project form is not shown.
- Screen 11: step 4A, visit observations.
- Screen 12: step 4B, resident feedback notes; topic dropdown options are not visible.
- Screens 13–16 and Resale/common screens 17–32 were visually reviewed at overview level; see `screen-review-12-32.md`. Detailed field extraction and all hidden menu states remain outstanding. The six resale subject areas in screen 17 are explicitly incomplete and differ from the supporting document’s 12-step tracker; confirm mapping before implementing it.

## Proposed next increments
1. Confirm the expanded developer-check status dropdown and confirm expanded project form fields and the remaining closed dropdowns; resident feedback uses free text by user authorization.
2. Complete missing step-one priority tiers and personalization once their expected options/interaction are confirmed.
3. Continue other New Buy steps in small slices; then review and implement Resale, dashboard and common tabs.
4. Introduce secure authentication and idempotent guest-to-account migration after the data model and authorization policies are reviewed.
5. Add admin, website, notifications, testing and production hardening in later increments.

Supabase inspection found one healthy project with no application tables. Migration `20261109073922_finnest_private_foundation` adds a user-scoped draft table and private document bucket. Website Profile now offers Supabase email/password authentication, but no drafts or documents are uploaded. The project requires email confirmation and its Auth Site URL is still localhost; configure real site and redirect URLs before production/testing confirmation links. RLS and bucket settings were inspected, but full two-user isolation and upload/download tests remain required. Android auth, guest migration and sync remain future phases. Never put service-role credentials in an app.

## Website and admin foundation
- A responsive React/Vite guest-facing website now includes a public published-guidance library (category filter and paginated published items) and covers a dashboard, the first two New Property screens, visit observations, local project names, expert categories, and profile information. Browser drafts are separate from Android Room data.
- `/Adminlogin` now supports a limited guidance editor backed by Supabase Auth and RLS for server-assigned super_admin/content_manager roles. Drafts are editor-only; published guidance is public. A database trigger logs content create/update metadata. No admin role has been assigned, and the panel does not manage users, app forms, experts or buyer data. Authenticator setup is now present, and content/audit RLS policies require an AAL2 session. Separate admin sessions, recovery, live MFA and full two-user access-control/security testing remain necessary before production use.
- The current frontend is not the Laravel website/backend specified as the recommended stack in the brief. The broader shared backend, API contracts, admin system and Android/web synchronization remain to be designed and implemented; only the private Supabase foundation, website authentication and limited guidance editor have been added.

See `requirements-traceability.md` for the detailed requirements review and production release gates.

### Published guidance pagination
- Buyer-facing website loads published guidance in pages of 50 using updated-at and UUID keyset order. The category and published filters apply on every page; errors preserve existing results and can be retried. Category switches invalidate stale responses. Content changes during browsing may change ordering, so users can refresh for the latest view. UI tests use mocked Supabase; live PostgREST pagination and authorization need end-to-end tests with real roles and non-sensitive data.

### Android published guidance
- Android uses the public Supabase project key over HTTPS to fetch published guidance only, with category filtering and 50-item keyset pagination. It sends no buyer draft and uses no admin credential; database RLS is the authorization boundary. No offline cache is provided, so an offline/error state offers retry. Unit and emulator tests use fixtures/fakes; live Android publishing, pagination and authorization are not yet verified end to end.

### Resale planning increment
- Android now opens a separate resale overview with six reference subject areas and a requirements/location form. Its optional budget, configuration and location fields autosave in the separate `resale_property` local scope. No completion, property inspection, tax rate or independent verification is implied. The visible six areas are now available as local notes; full resale mapping and cloud sync remain outstanding.

### Resale condition increment
- Android records the 12 reference condition prompts, with a provisional user-entered status, optional observation, source and date, in the separate resale local scope. These are not inspection or safety findings. Seller/ownership, society, legal and handover checklists now follow it.

### Remaining resale prototype subject areas
- Android provides seller/ownership (5 prompts), society/dues (7 prompts with optional amount), legal review (8 prompts and lawyer status), and agreement/handover (13 prompts), plus a summary of local entries. All are user notes, not verification, professional assessment or a complete resale journey. Seller/legal use existing provisional checklist labels; society, handover and lawyer-review statuses use free text with visible prototype defaults as placeholders because expanded choices are unknown. Live device QA and the six-area versus 12-step mapping remain unresolved.

### Android common information screens
- Android now has informational expert roles, a deliberately unavailable expert-contact state, an About/disclaimer screen reflecting actual local storage and online guidance, and an illustrative verbal-promise concern. The concern can explicitly mark an existing New Property or Resale checklist item Pending / unknown after confirmation, or open its checklist. No professional profiles, booking, matching, advice or independent verification are provided; expanded reference filter options remain unknown.
