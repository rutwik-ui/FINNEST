# Requirements traceability and release gates

Review basis: FINNEST master brief supplied with this project, Game of Home Buying content document, and the screen references inventoried in `phase-plan.md`. Screenshot labels and options take precedence for forms. This is a working review, not evidence of feature completeness.

| Requirement | Intended responsibility | Current state | Completion gate |
|---|---|---|---|
| Native Android Kotlin/XML, guest flow, offline draft | Android + Room; account synchronization via backend | Entry, dashboard and New Property steps 1–8 use local storage; no account sync | Exact screen-by-screen parity; persistent drafts, process-death and device tests |
| New and Resale journeys | Published versioned definitions + compatible Android/web form renderers; local draft cache | First New Property screens hard-coded; Resale not built | Inventory remaining prototypes, field IDs/options, conditions; versioned form engine, offline compatibility and migration tests |
| Dynamic form administration | Admin editor, version history, preview, publishing/rollback backed by server authorization | Limited guidance editor only; no form builder or published-form API | Versioning, stable IDs, validation, role permissions, preview, audit, rollback and Android/web parity tests |
| Guest-to-account migration | Local queue, authenticated backend, verified idempotent confirmation | No migration or sync; local guest data stays local | Offline, retry, conflict, account-switch, partial-failure and duplicate tests; no loss of unsynced data |
| Projects, visits, comparisons | Local drafts + authenticated API and private storage | Name-only shortlist and 4A visit prompts, no full comparison or project-specific visit model | Confirm expanded project prototype, implement required fields and all journey workflows |
| Experts, reminders, notifications | Admin-managed verified data + service APIs; local reminders and FCM | Web expert category placeholders; no expert requests, reminders or FCM | Credentials/verification workflow, consent, permissions, notification delivery and device tests |
| Web admin and user data | Backend-enforced role permissions and audit; separate protected admin session | Limited content editor and admin role table. Content/draft/audit policies now require role and MFA; no user management | Separate admin auth/session design, production recovery, audit coverage, two-user RBAC/RLS and abuse tests |
| Supabase and private attachments | Postgres/RLS, private Storage, authenticated APIs | Per-user draft and private bucket foundations; no app upload or sync | Full schema after screen inventory; end-to-end authorization, file validation, backup/restore and deletion tests |
| Production UI/UX | Native Android and responsive web using approved references | Initial screens and web dashboard are early implementations | Screen inventory, accessibility, responsive/layout and physical-device QA against every approved reference |
| 1,000-user production readiness | Staging/production environments, release pipelines, security, operations and observability | Debug APK build only; no capacity evidence | Signed release and secrets process, privacy/legal review, load test with defined concurrent users/SLOs, alerts, incident plan and restore drill |

## Important unresolved decisions
- Expanded developer/visit status menu, personalisation family options, project detail form, and resident-feedback topics are not visible in the screenshots reviewed so far. Do not treat invented alternatives as final.
- Clarify the mismatch between the supporting content's resale step counts and the visual references after remaining screens are individually inventoried.
- The brief recommends Laravel and a separate admin session; the existing frontend uses React and Supabase directly. Decide and document the production backend/admin architecture before building the dynamic form management layer.
- Production domain and Supabase Auth redirect URLs are not configured; email confirmation cannot be verified for the intended deployment.
- MFA enrollment/recovery and administrator bootstrap must be tested with real, authorized staging accounts before granting administrative roles or using the editor operationally.

## Current verification boundaries
GitHub workflows assemble a debug APK, run JVM tests, and exercise selected Android 14 emulator/instrumentation scenarios; full journey and real-device testing remain outstanding. Web unit/UI tests use mocks and do not prove real Supabase login, authorization, email, MFA, storage, or account isolation. Current live database metadata checks verify policy definitions only; they are not a substitute for two-user end-to-end authorization tests. No application code should be described as production-ready until these release gates are met.
