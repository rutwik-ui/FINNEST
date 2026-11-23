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
import { VitePWA } from "vite-plugin-pwa";

export default defineConfig({
  plugins: [VitePWA({
    strategies: 'generateSW', filename: 'sw.js', registerType: 'autoUpdate', injectRegister: null,
    outDir: 'dist/client', manifest: false, devOptions: { enabled: false },
    workbox: {
      globPatterns: ['assets/**/*.{js,css,woff2,png,jpg,webp,svg}', 'favicon*.{png,ico}', '*icon*.png'],
      globIgnores: ['**/server/**', '**/ssr/**'], maximumFileSizeToCacheInBytes: 4 * 1024 * 1024,
      navigateFallback: null, importScripts: ['/sw-notifications.js'],
      runtimeCaching: [
        { urlPattern: ({ request, url }) => request.mode === 'navigate' && url.origin === self.location.origin && !url.pathname.startsWith('/~oauth') && !/^\/(?:admin|api|_serverFn)/i.test(url.pathname), handler: 'NetworkFirst', options: { cacheName: 'finnest-pages', networkTimeoutSeconds: 4, expiration: { maxEntries: 35, maxAgeSeconds: 7 * 86400 }, cacheableResponse: { statuses: [200] } } },
        { urlPattern: ({ request, url }) => url.origin === self.location.origin && /^\/assets\/.+-[a-zA-Z0-9_-]{8,}\./.test(url.pathname) && ['script', 'style', 'image', 'font'].includes(request.destination), handler: 'CacheFirst', options: { cacheName: 'finnest-assets', expiration: { maxEntries: 150, maxAgeSeconds: 30 * 86400 } } },
      ],
    },
  })],
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
