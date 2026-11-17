<!-- LOVABLE:BEGIN -->
> [!IMPORTANT]
> This project is connected to [Lovable](https://lovable.dev). Avoid rewriting
> published git history — force pushing, or rebasing/amending/squashing commits
> that are already pushed — as it rewrites history on Lovable's side and the
> user will likely lose their project history.
>
> Commits you push to the connected branch sync back to Lovable and show up in
> the editor, so keep the branch in a working state.

## What Lovable manages

Rules outside this block are the project's own. Change the pieces below in
Lovable, not by hand. With the [Lovable MCP](https://mcp.lovable.dev)
connected, `send_message` asks Lovable to do it; otherwise ask the user to
use the editor.

- **Database.** Do not add or edit `supabase/migrations/` or `drizzle/`.
  Ask Lovable to change the schema.
- **Generated files.** Do not edit a file whose header says it is automatically
  generated.
- **AI gateway.** Keep model calls on Lovable's gateway, and keep
  `LOVABLE_API_KEY` out of browser code.
- **Connectors.** Lovable provides native connectors. `list_connectors` shows
  which are enabled; `add_connector` only returns the dashboard link. When a
  connector is enabled, calls go through the connector gateway and need
  `LOVABLE_API_KEY`. When Lovable does not provide one, users can bring their
  own key and store it in Secrets.
- **Secrets.** Lovable manages them. Do not put secret values in the repo.
<!-- LOVABLE:END -->

## FINNEST delivery constraints

- Keep the connected external Supabase project as the shared identity, database and storage infrastructure from the initial development phase; this preserves the user-required centralized integration.
- The requested production backend must run on Node.js outside this project's supported server runtime; do not label server functions as a full Node.js backend or begin an incompatible implementation.
- Keep repository delivery explicit and verified: public GitHub reads and API-based writes are separate from Lovable Git sync, which cannot attach this project to the existing repository.
- External production builds use the existing Nitro integration with a host-specific target and deploy both browser assets and server handlers; static-only exports omit protected application operations.
- Treat delivery into the existing repository, web-app checks, host deployment, and native signed releases as separate verified gates; a connector or successful web deployment does not prove source sync or update installed APKs.
- Keep native Android work in the existing repository's `android/` project and build APKs through its Android CI, not the web runtime; shared native roadmap views derive progress only from durable journey-scoped form entries to avoid false saved-progress claims.
- Native presentation uses a shared web-measured styling layer without replacing form IDs or save logic, and Android CI exports screenshots from app-private storage for visual inspection; this prevents screen drift and unverifiable screenshot claims.

## Journey navigation and bulk imports

- Define both public roadmap step lists in one browser-safe module and link only existing journey screens; this prevents new/resale navigation from diverging.
- Parse CSV/XLSX locally with established readers, share Zod import schemas with authenticated server functions, and save using the caller's Supabase client after identity and role checks; this keeps public imports off-limits without privileged bypasses.
- Bulk imports are atomic new-record inserts with stable preview UUIDs, ignored ID conflicts and field-by-field readback; this permits safe retries without overwriting existing data.
- Keep request/CSRF middleware and attach the browser session token only on server-function calls; the server remains responsible for verifying identity.
- Keep administrator authorization consistent across the console, server functions and RLS through caller-scoped role checks; session-scoped console roles prevent stale permissions crossing account changes.
- Signed-in journey data syncs through one `finnest_journeys` row per user and journey type (version-checked writes, Realtime updates, `finnest_activity` log); web, Android and admin must all use these tables so progress stays identical across platforms.

## Production experience
- Keep authentication in one reusable email/password form; Supabase owns refresh tokens, while account-specific draft keys and IndexedDB outboxes prevent cross-account answer leakage.
- Merge journey updates field-by-field against the last acknowledged version and retry version conflicts without overwriting untouched remote answers.
- Offline registration has one guarded module; previews never register app-shell workers, HTML uses NetworkFirst, and only immutable assets enter CacheFirst caches.
- Cache only explicitly published public content outside the account store; never cache authenticated API responses or private files in the service worker.
- App configuration has separate published/draft rows, optimistic revisions, super-admin RLS and trigger-only audit writes; readers subscribe only to published changes.
- Uploaded raster brand assets retain their pixels; head metadata and scaled app icons derive from that same source instead of recreating a vector logo.
- Native sessions use the Supabase Kotlin auth library with encrypted session storage; Room scopes drafts by account and stores field-level outbox entries atomically with local saves, while WorkManager retries caller-authenticated version-checked writes.
- Render one header variant from shared navigation breakpoints; use the design-system modal drawer for mobile focus trapping and keep account actions out of the compact header to prevent duplicate targets.
- Use one outlined full-logo geometry across web and native placements; keep uploaded launcher/favicon artwork separate so alignment fixes cannot replace the in-app identity.
- Native launch decisions consume validated published onboarding settings cached separately from accounts; mark introductions complete only after account access or explicit guest continuation to keep first visits and expired sessions truthful.
