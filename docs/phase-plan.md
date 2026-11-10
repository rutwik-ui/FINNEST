# Phase plan and reference inventory

## Current Android dashboard increment
- New Property now has a local home dashboard with direct links to each implemented step. Existing users with that journey selected land there on launch; Save & Home and the final review return there only after pending saves succeed. No completion percentage is inferred.
- Reminders are device-local dated notes, accessible from the dashboard. Adding and deleting notes updates one local draft atomically. No OS notifications, notification preferences or cloud sync are implemented.
- Bundled Inter is the default font and Montserrat is used for major headings. These changes improve consistency but are not a claim that every screen is production-polished or visually validated on a device.
- Android 14 emulator CI now runs Room instrumentation and a dashboard → final review → save → dashboard → reopen persistence scenario. This is not full journey, visual, accessibility, real-device or upgrade testing. Dashboard data is local; missing common tabs, profile, resale journey and cloud synchronization remain open. The New Property journey summary now displays local status counts, a checklist filter, project names and direct step links; its statuses are user entries, not verified progress.

## Delivered so far
- Native Android entry foundation, introductory screens and journey selection.
- New Property step 1, Requirements & Budget: optional budget fields, location and configuration selections, carpet area and accessibility needs, stored locally in Room.
- Step 4A provides 14 visit-observation prompts on Android and web, with local status, notes, source and date. Step 4B provides local resident feedback notes on Android and website using a free-text topic.
- Step 8 provides an Android and website review of saved checklist statuses, notes, project names, assumptions, pending items and locally saved next actions. It does not verify entries or provide expert referrals; device testing is outstanding.
- Step 7 provides Android and website initial and recurring cost estimates, cost prompts and one lender-term record. Totals sum entered amounts only and identify missing inputs; status labels are provisional. Device testing remains outstanding.
- Step 6 provides nine legal due diligence prompts on Android and the website, with locally saved status, notes, source and follow-up date. Status labels are provisional, and no expert referral service is connected.
- Step 5 provides an Android and website illustrative fixed-rate EMI calculator and lender prompts. Input limits and lender-document status labels are explicit implementation choices; status labels are provisional because the prototype menu is closed. No eligibility or approval is implied. Device UI testing remains outstanding.
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
- A responsive React/Vite guest-facing website now includes a public published-guidance library (category filter and paginated published items) and covers a dashboard, New Property screens through review and summary, local project names, expert categories, and profile information. Browser drafts are separate from Android Room data.
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

### Android local profile and privacy
- Android provides optional device-only name and city, accurate privacy information, links to reminders and About, and a typed-confirmation reset for New Property, Resale, reminders and local profile scopes. Reset does not alter selected journey or onboarding preferences, website data, Supabase records or an account. Device saving is always on; no fabricated marketing, analytics, contact-consent or notification switches are offered. Database deletion is one SQL statement serialized with draft writes, and successful deletion clears stale activity stacks. Real-device, failure-injection and accessibility testing remain release gates.

### Isolated backend security checks
- A disposable PostgreSQL CI workflow applies the repository migrations and exercises draft ownership, simulated Storage object policies, and guidance/admin MFA/role policies, including publication, audit and role revocation. Tests supply simulated claims and a minimal storage schema; they do not contact or change the connected Supabase project and do not verify real Auth, MFA, Storage APIs or uploads. Real staging tests and a security review remain necessary.

### Guidance editor reliability
- The limited web guidance editor now distinguishes loading/failure from an empty list, supports retry after role lookup failure, ignores stale list results after account/token changes, and asks before discarding edited content on editor navigation/sign-out (with a browser unload warning). Concurrent edits still require a refresh and manual reconciliation; published items require confirmation. The editor now pages guidance with category/status filters; activity remains limited to 20 recent events, and it is not a complete admin panel. UI tests use mocked Supabase; real sessions, MFA, role revocation and concurrent edits must be verified in staging.

### Guidance administration browsing
- The guidance editor now provides server-side category and publication-status filters and 50-item keyset pagination using updated timestamp plus UUID for deterministic ties. Refresh restarts the list, and load-more failures can be retried without losing loaded items. Changing filters does not discard editor work; stale responses are ignored. Since updated timestamps can change during browsing, pagination is not a snapshot: refresh to find recent changes. The audit view is still limited to 20 events. Unit and UI tests use mocked responses; real PostgREST pagination, concurrent updates, permissions and production-scale performance remain to be validated in an authorized staging environment.

### Website local-data safety and roadmap
- Website profile now provides typed-confirmation deletion of its browser-local journey draft, without touching authentication, Supabase or Android data. The reader preserves unreadable stored values rather than overwriting them on startup; errors and detected other-tab changes are shown, and unsaved changes produce a browser unload warning. Cross-tab comparison is best effort rather than atomic; simultaneous writes may still race, and browser storage is not a backup. The New Property roadmap routes the implemented web steps to their respective screens; resident feedback (4B) is linked from the visit screen. Tests cover local persistence, malformed data, write failures, cancellation, typed reset and detected conflicts in jsdom/unit tests; real-browser multi-tab, accessibility and physical-device QA remain outstanding. No account sync or safe migration has been implemented.

### Website New Property steps 4B–6
- Website now provides resident feedback, an illustrative fixed-rate home-loan guide with lender-document prompts, and legal due diligence notes. Topic is free text per the user's prior authorization; topic and what was shared are required. Unfinished feedback and added notes are browser-local; note deletion requires confirmation. Loan inputs and lender notes remain local, and the calculation uses the same 0–100% rate, positive amount up to ₹1 trillion, and whole-year 1–50 limits as Android. The estimate excludes fees and is not an offer or an eligibility decision. Legal and lender statuses are provisional, and no legal verification or expert referral is provided. Website New Property forms are not shown when the selected journey is Resale. Mocked/jsdom and pure calculation tests cover persistence and navigation, not real-browser visual/accessibility QA, Android/web syncing or live services. Website financial due diligence and final review have since been added; The website now offers all six prototype Resale subject areas as notes; the complete journey remains unspecified.

### Website financial review and journey summary
- Website Step 7 records 11 initial and three recurring amounts, ten cost/document prompts and one lender-term set. Totals use integer paise, count entered values, leave blanks unknown and reject invalid or over-limit entries rather than presenting partial totals. Step 8 reviews user-entered checklist statuses, notes, project names, loan assumptions and cost totals, with local next-action notes. My Journey provides checklist filters and direct links; no status is represented as an independent verification or completed professional review. These records remain in browser storage and are not synced. New Property dashboard counts are labeled as developer-only and hidden for Resale. Unit and jsdom UI tests cover totals, persistence, filtering and navigation; real-browser visual/accessibility and full end-to-end testing remain outstanding. Complete Resale website parity, project comparison details, expert referral and cloud synchronization remain incomplete.

### Website Resale overview, planning and condition
- Website now provides the six-area Resale overview, optional requirements and location inputs, and 12 condition-observation prompts. The four remaining prototype subject areas were subsequently added. New Property and Resale data use separate fields within the same browser draft; the existing local deletion removes both. Switching journey routes the buyer home and direct links are gated to the selected journey. Provisional condition statuses represent user notes, not an inspection or verification. Tests cover navigation, persistence, separation, input-shape validation and detected storage conflicts using unit tests and jsdom; real multi-tab races, browser/device visual accessibility QA, cloud sync and the prototype's six-area versus 12-step ambiguity remain outstanding.

### Website Resale seller, society, legal, handover and summary
- Website now offers the four remaining prototype subject areas (5 seller/title, 7 society/dues, 8 legal and 13 handover prompts) and a Resale summary. Seller/legal statuses use provisional labels; society/handover statuses and lawyer review are optional free text because the prototype dropdowns are closed. Placeholders are not persisted as statuses, amounts are user entries rather than calculated or verified dues, and unknown values remain unknown. Summary shows planning, checklist entries and links without interpreting arbitrary free text as completion. All six areas remain browser-local, separate from New Property fields but in the same deletable browser draft. Unit and jsdom tests cover persistence, navigation, malformed data and summary classification; no real-browser/device QA, cross-device sync or independent legal/technical verification. The reference's six-area versus 12-step ambiguity and the wider journey scope remain unresolved.

### Website common profile, reminder and information screens
- Website has optional local name/city, dated reminder notes with confirmed deletion, privacy/about disclosures, and an illustrative verbal-promise concern guide. Reminder notes have no notification delivery, and the concern guide does not change a checklist status. Dates are valid calendar days from 1900–2100, including past dates. Profile and reminders share the browser draft and are removed by its confirmed reset; neither is synced to Supabase or Android. Account auth and published guidance are network services, and browser storage is not encrypted by FINNEST. No fabricated consent or notification switches are provided. Unit and jsdom tests cover dates, draft persistence, deletion and disclosures, not real-device accessibility or network behavior.

### Guidance editor preview and navigation safety
- The limited web admin editor now offers a plain-text preview without publishing, blocks edits and in-app navigation during a save, warns before discarding changes through the app header and same-document history, and asks users to check the list before retrying an unconfirmed save. The browser's own unload behavior varies, and same-document history restoration is best effort; tests simulate navigation in jsdom, not real browsers. Existing role/MFA and optimistic concurrency checks still depend on server authorization. Live MFA, role revocation, save failure ambiguity and full admin workflows remain staging and production gates; this is not a complete admin panel.

### Website expert roles and concern action
- Website expert directory now lists the seven informational professional roles plus optional personal-preference roles, with a role filter and an explicitly unavailable contact state. This is not a profile, booking or verified expert service; reference filter menus were not expanded. The illustrative concern guide can open a selected checklist item or, after explicit confirmation, set it to Pending / unknown in the currently selected journey while preserving notes. An explicit save reports success only after localStorage accepts the write; cross-tab or storage errors do not mutate the selected status in memory. Unit and jsdom tests cover both journeys, confirmation and conflict, not real-browser/device QA or cloud sync. The user-entered pending status is not a FINNEST risk finding.

### Admin activity pagination
- Guidance activity is paged by descending audit identity with lookahead, retry and stale-session response guards. Refresh is needed to see newer or late-committing transactions; identity order is not necessarily commit order. Only guidance create/update metadata is logged, not a complete security audit. Queries are tested with mocks, not live PostgREST or real MFA sessions.

### Browser draft recovery copy
- Profile offers a user-initiated download of the current raw saved browser draft, including malformed drafts for recovery. The file is unencrypted and may contain sensitive information; unsaved in-memory changes, account data and Android data are excluded. Download completion depends on browser behavior, and in-app import/restore is not implemented. Automated tests use jsdom mocks; real-browser download and recovery remain to be verified.

### Admin independent loading resilience
- Guidance and activity refresh independently: either may remain usable if the other fails, with separate status/error messages and shared refresh. Mocked UI tests cover failed activity, retry, keyset cursor and ignoring a delayed page after refresh. Live PostgREST, real MFA sessions and browser end-to-end behavior remain unverified; this remains a guidance-only admin tool.

### Website local draft restore
- Profile can restore a structurally compatible raw website draft up to 5 MiB after a file selection and typed RESTORE confirmation. It replaces both journeys and profile/reminder data, including unsaved tab edits; it does not merge or affect Android or cloud data. Files are parsed locally, known fields validated, unknown fields preserved, and the write checks for a changed stored value; localStorage comparison is not atomic. Invalid recovery copies can be downloaded but cannot be restored without repair. These are unencrypted raw files without version/authenticity guarantees; validation does not prove their origin or semantic correctness. Unit and jsdom tests cover success, invalid data, quota and conflicts; real-browser round-trip and accessibility QA remain outstanding.

### Additional disposable database policy boundaries
- Expanded the isolated PostgreSQL shim suite with direct role and audit mutation denial, guidance deletion denial, missing-subject AAL2 denial, draft uniqueness, invalid journey, and oversized payload checks. This does not change the connected Supabase project or establish real Auth, Storage, PostgREST or MFA behavior. Hosted PostgreSQL CI is required because local PostgreSQL is not installed.
