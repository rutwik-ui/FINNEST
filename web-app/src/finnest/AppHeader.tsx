import { useEffect, useState } from 'react';
import { Menu, UserRound, ChevronDown, Search, LogOut } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Sheet, SheetTrigger, SheetContent, SheetTitle, SheetDescription } from '@/components/ui/sheet';
import { DropdownMenu, DropdownMenuTrigger, DropdownMenuContent, DropdownMenuItem, DropdownMenuSeparator } from '@/components/ui/dropdown-menu';
import { Wordmark } from './BrandMark';
import { mainNavigation, secondaryNavigation, type NavigationMode } from './navigation';
import { supabase } from './supabase';
import { SiteSearch, useSearchShortcut } from './SiteSearch';
import { NotificationBell } from './NotificationBell';
import type { NotificationsApi } from './notifications';

import type { RoadmapStep as Step } from './roadmap';
type Props = { path: string; go: (path: string) => void; mode: NavigationMode; signedIn: boolean; journey: string; chooseJourney: () => void; openAuth: (mode: 'signin' | 'signup') => void; openStep: (step: Step) => void; notify: NotificationsApi };
const isMac = typeof navigator !== 'undefined' && /Mac|iPhone|iPad/.test(navigator.platform);

export default function AppHeader({ path, go, mode, signedIn, journey, chooseJourney, openAuth, openStep, notify }: Props) {
  const [open, setOpen] = useState(false);
  const [searching, setSearching] = useState(false);
  const [scrolled, setScrolled] = useState(false);
  useSearchShortcut(setSearching);
  useEffect(() => { setOpen(false); }, [path, mode]);
  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 4);
    onScroll();
    window.addEventListener('scroll', onScroll, { passive: true });
    return () => window.removeEventListener('scroll', onScroll);
  }, []);
  const navigate = (url: string) => { setOpen(false); go(url); };
  const account = (action: 'signin' | 'signup') => { setOpen(false); openAuth(action); };
  const primary = mainNavigation.slice(0, 4);
  const moreActive = secondaryNavigation.some(item => item.path === path);
  const bell = (compact?: boolean) => <NotificationBell api={notify} compact={compact} open={item => openStep({ label: item.title, icon: '', path: item.path, ...(item.section ? { section: item.section } : {}) })} openSettings={() => navigate('/notifications')}/>;
  const search = <SiteSearch open={searching} onOpenChange={setSearching} journey={journey} go={navigate} openStep={openStep}/>;

  return <header className={'topbar' + (scrolled ? ' is-scrolled' : '')}>
    <Button variant="ghost" className="brand" onClick={() => navigate('/')} aria-label="FINNEST home"><Wordmark/></Button>
    {mode === 'desktop' ? <>
      <nav className="desktop-nav" aria-label="Website navigation">
        {primary.map(item => <Button key={item.path} variant="ghost" className="nav-link" aria-current={path === item.path ? 'page' : undefined} onClick={() => navigate(item.path)}>{item.label}</Button>)}
        <DropdownMenu><DropdownMenuTrigger asChild><Button variant="ghost" className="nav-link nav-more" data-active={moreActive || undefined}>More<ChevronDown size={15} aria-hidden="true"/></Button></DropdownMenuTrigger>
          <DropdownMenuContent className="finnest-dropdown nav-menu" align="start" sideOffset={10}>{secondaryNavigation.map(item => <DropdownMenuItem key={item.path} data-current={path === item.path || undefined} onSelect={() => navigate(item.path)}><item.icon/>{item.label}</DropdownMenuItem>)}</DropdownMenuContent>
        </DropdownMenu>
      </nav>
      <div className="header-actions">
        <button type="button" className="search-trigger" onClick={() => setSearching(true)} aria-label="Search FINNEST" aria-keyshortcuts={isMac ? 'Meta+K' : 'Control+K'}>
          <Search aria-hidden="true"/><span className="search-trigger-label">Search</span><kbd className="search-trigger-kbd">{isMac ? '⌘' : 'Ctrl'} K</kbd>
        </button>
        <button type="button" className="journey-switch" onClick={chooseJourney} aria-label={`Journey: ${journey}. Change journey`}>
          <span className="journey-switch-label">Journey</span><span className="journey-switch-value">{journey}</span><ChevronDown size={14} aria-hidden="true"/>
        </button>
        <span className="header-divider" aria-hidden="true"/>
        {bell()}
        {signedIn ? <DropdownMenu><DropdownMenuTrigger asChild><Button variant="ghost" className="account-button" aria-label="Account menu"><UserRound/><ChevronDown size={15}/></Button></DropdownMenuTrigger><DropdownMenuContent className="finnest-dropdown nav-menu" align="end" sideOffset={10}><DropdownMenuItem onSelect={() => navigate('/profile')}><UserRound/>Profile & settings</DropdownMenuItem><DropdownMenuSeparator/><DropdownMenuItem onSelect={() => { void supabase.auth.signOut(); }}><LogOut/>Sign out</DropdownMenuItem></DropdownMenuContent></DropdownMenu>
          : <div className="header-auth"><Button variant="ghost" className="auth-login" onClick={() => account('signin')}>Log in</Button><Button className="auth-signup" onClick={() => account('signup')}>Sign up</Button></div>}
      </div>
      {search}
    </> : <div className="header-actions">
      {bell(true)}
      <Button variant="ghost" className="icon-button" aria-label="Search FINNEST" onClick={() => setSearching(true)}><Search/></Button>
      <Sheet open={open} onOpenChange={setOpen}><SheetTrigger asChild><Button variant="ghost" className="icon-button" aria-label="Open menu"><Menu/></Button></SheetTrigger><SheetContent className="finnest-drawer" aria-describedby="navigation-description"><SheetTitle><Wordmark/></SheetTitle><SheetDescription id="navigation-description" className="sr-only">FINNEST navigation and account</SheetDescription>
        <nav aria-label="Website navigation" className="drawer-links">{[...mainNavigation.filter(item => item.path !== '/profile'), ...secondaryNavigation].map(item => <Button variant="ghost" key={item.path} aria-current={path === item.path ? 'page' : undefined} onClick={() => navigate(item.path)}><item.icon size={20}/>{item.label}</Button>)}</nav>
        <Button variant="outline" className="drawer-journey" onClick={() => { setOpen(false); chooseJourney(); }}>{journey}<ChevronDown size={18}/></Button>
        <div className="drawer-account">{signedIn ? <Button variant="outline" onClick={() => navigate('/profile')}><UserRound/>Profile & settings</Button> : <><Button onClick={() => account('signup')}>Sign up</Button><Button variant="outline" onClick={() => account('signin')}>Log in</Button></>}</div>
      </SheetContent></Sheet>
      {search}
    </div>}
  </header>;
}
