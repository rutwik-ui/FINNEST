import { Home, SlidersHorizontal, FileText, CircleHelp, UserRound, Bookmark, Clock3, Info, ShieldAlert } from 'lucide-react';
export const mainNavigation = [
  { path: '/', label: 'Home', icon: Home },
  { path: '/journey', label: 'Journey', icon: SlidersHorizontal },
  { path: '/projects', label: 'Projects', icon: FileText },
  { path: '/experts', label: 'Experts', icon: CircleHelp },
  { path: '/profile', label: 'Profile', icon: UserRound },
];
export const secondaryNavigation = [
  { path: '/guidance', label: 'Guidance', icon: Bookmark },
  { path: '/reminders', label: 'Reminders', icon: Clock3 },
  { path: '/about', label: 'About FINNEST', icon: Info },
  { path: '/privacy', label: 'Privacy & consent', icon: ShieldAlert },
];
export type NavigationMode = 'mobile' | 'tablet' | 'desktop';
export function navigationMode(width: number): NavigationMode {
  return width >= 1024 ? 'desktop' : width >= 768 ? 'tablet' : 'mobile';
}