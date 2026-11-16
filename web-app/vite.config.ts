// @lovable.dev/vite-tanstack-config already includes the following — do NOT add them manually
// or the app will break with duplicate plugins:
//   - TanStack devtools (dev-only, first), tanstackStart, viteReact, tailwindcss, tsConfigPaths,
//     nitro (build-only using cloudflare as a default target), VITE_* env injection, @ path alias,
//     React/TanStack dedupe, error logger plugins, and sandbox detection (port/host/strictPort).
// Never set ssr.external or resolve.external: the Worker has no runtime module resolution, so
// the build fails. Never add compression (compressPublicAssets, vite-plugin-compression):
// hosting already compresses at the edge.
// You can pass additional config via defineConfig({ vite: { ... }, etc... }) if needed.
import { defineConfig } from "@lovable.dev/vite-tanstack-config";

export default defineConfig({
  tanstackStart: {
    // Redirect TanStack Start's bundled server entry to src/server.ts (our SSR error wrapper).
    // nitro/vite builds from this
    server: { entry: "server" },
  },
  // ------------------------------------------------------------------
  // Vercel deployment
  // ------------------------------------------------------------------
  // The Lovable config defaults Nitro to the "cloudflare-module" preset, which
  // produces a Cloudflare Worker bundle that Vercel cannot run. Pin Nitro to
  // the "vercel" preset so `npm run build` emits the Vercel Build Output API
  // into `.vercel/output/` (static assets + the `__server` serverless function
  // that handles every SSR route).
  //
  // Vercel reads `vercel.json` -> `outputDirectory: ".vercel/output"`.
  //
  // NOTE FOR LOVABLE: if Lovable ever rewrites this file, re-add the `nitro`
  // block below — without it the Vercel build produces no server function and
  // every route returns 404.
  nitro: {
    preset: "vercel",
  },
});
