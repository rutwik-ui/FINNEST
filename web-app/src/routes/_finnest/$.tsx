import { createFileRoute } from "@tanstack/react-router";

export const Route = createFileRoute("/_finnest/$")({
  head: ({ params }) => {
    const slug = params._splat ?? '';
    const names: Record<string, string> = { journey: 'Your journey', projects: 'Shortlisted projects', visit: 'Visit observations', feedback: 'Resident feedback', loan: 'Home loan guide', legal: 'Legal review', financial: 'Financial review', review: 'Final review', summary: 'Journey summary', experts: 'Experts directory', 'expert-detail': 'Expert guidance', profile: 'Profile and settings', guidance: 'Guidance library', reminders: 'Reminders', privacy: 'Privacy and consent', about: 'About FINNEST', concern: 'Understand a concern', Adminlogin: 'Secure admin sign-in', 'resale-requirements': 'Resale requirements and location', 'resale-condition': 'Resale property condition', 'resale-seller': 'Resale seller and ownership', 'resale-society': 'Resale society documents and dues', 'resale-legal': 'Resale legal review', 'resale-handover': 'Resale agreement and handover', 'resale-summary': 'Resale journey summary' };
    const title = `FINNEST — ${names[slug] ?? 'Page not found'}`;
    const description = slug.toLowerCase().startsWith('admin') ? 'Secure FINNEST management access for authorized team members.' : `${names[slug] ?? 'Home-buying guidance'} for your new or resale home in India. Organize your questions and notes with FINNEST.`;
    return {
    meta: [
      { title },
      { name: "description", content: description },
      { property: "og:title", content: title },
      { property: "og:description", content: description },
      { property: "og:type", content: "website" },
      { name: "twitter:card", content: "summary" },
    ],
  };
  },
  component: () => null,
});
