import { ClientOnly, Outlet, createFileRoute } from "@tanstack/react-router";
import { App } from "@/finnest/FinnestApp";
import finnestCss from "@/finnest/finnest.css?url";
import finnestProCss from "@/finnest/finnest-pro.css?url";

// The preserved FINNEST web app manages its own screens from the URL and keeps
// drafts in this browser, so it stays mounted across all child paths.
export const Route = createFileRoute("/_finnest")({
  head: () => ({
    links: [
      { rel: "preconnect", href: "https://fonts.googleapis.com" },
      { rel: "preconnect", href: "https://fonts.gstatic.com", crossOrigin: "anonymous" },
      { rel: "stylesheet", href: "https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap" },
      { rel: "stylesheet", href: finnestCss },
      { rel: "stylesheet", href: finnestProCss },
    ],
  }),
  component: FinnestLayout,
});

function FinnestLayout() {
  return (
    <>
      <ClientOnly fallback={<p style={{ padding: 24 }}>Loading FINNEST…</p>}>
        <App />
      </ClientOnly>
      <Outlet />
    </>
  );
}
