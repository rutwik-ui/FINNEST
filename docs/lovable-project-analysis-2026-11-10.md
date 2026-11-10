# FINNEST: analysis and required delivery approach

Reviewed 10 November 2026 at a4180a5f33fa0ecd85973da1a651d1b7902de2a0. The uploaded ZIP matches all 152 repository files.

## Requirements
- Node.js is required for the production backend, replacing the older Laravel proposal.
- Keep the existing Supabase project for shared database, authentication and storage; configure the complete schema and ownership rules during initial development. Do not migrate to Lovable Cloud.
- Web and admin first; native Android integration later. All use the same backend, database, accounts and versioned form definitions.
- Maintain source, migrations, configuration and documentation in this FINNEST repository. Preserve existing features and approved screen references.

## Findings
Web contains browser-local New Property and six Resale areas, EMI/cost calculations, profile auth and public guidance. Admin manages guidance with roles and TOTP only. Android is Kotlin/Room with guest drafts and public guidance, not authenticated buyer sync. Projects are names only; experts and reminders lack operational requests/notifications. Four Supabase tables and a private document bucket exist. No buyer draft sync is implemented. Four CI workflows passed at the reviewed revision; mocked/shim tests do not prove live account security.

## Delivery boundaries
Lovable can build React/TanStack web/admin, but cannot host a full Node.js server or preview native Android. Node.js needs separate hosting; retain native Android tooling. The authorized GitHub API connection reports push permission; API commits are not automatic Lovable Git sync.

## Next phase
Define the shared schema and Node.js API contract. Preserve stable field IDs, historical answers, guest data and current guidance consumers. Add account-safe durable sync, linked property records, versioned forms, operational admin, experts/support, attachments and real progress. Resolve resale step-count, roadmap-order and hidden-menu discrepancies before publishing final rules.

Release requires real auth/recovery/MFA, two-user isolation, guest migration/retry, saved-data readback, file access, accessibility and backup/restore checks. No application code or database changes were made during analysis; Node.js deployment remains a dependency.
