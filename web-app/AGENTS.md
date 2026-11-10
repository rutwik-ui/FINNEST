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

## Journey navigation and bulk imports

- Define both public roadmap step lists in one browser-safe module and link only existing journey screens; this prevents new/resale navigation from diverging.
- Parse CSV/XLSX locally with established readers, share Zod import schemas with authenticated server functions, and save using the caller's Supabase client after role and MFA checks; this keeps public imports off-limits without privileged bypasses.
- Bulk imports are atomic new-record inserts with stable preview UUIDs, ignored ID conflicts and field-by-field readback; this permits safe retries without overwriting existing data.
- Keep request/CSRF middleware and attach the browser session token only on server-function calls; the server remains responsible for verifying identity.
