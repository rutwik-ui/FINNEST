import { createFileRoute } from "@tanstack/react-router";

export const Route = createFileRoute("/_finnest/")({
  head: () => ({
    meta: [
      { title: "FINNEST — Your home-buying journey" },
      { name: "description", content: "Plan, organise and track every step of buying a new or resale home in India." },
      { property: "og:title", content: "FINNEST — Your home-buying journey" },
      { property: "og:description", content: "Plan, organise and track every step of buying a new or resale home in India." },
      { property: "og:type", content: "website" },
      { name: "twitter:card", content: "summary_large_image" },
    ],
  }),
  component: () => null,
});
