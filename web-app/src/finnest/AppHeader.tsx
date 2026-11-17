import { useEffect, useState } from 'react';
import { Menu, UserRound, ChevronDown, Search, LogOut } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Sheet, SheetTrigger, SheetContent, SheetTitle, SheetDescription } from '@/components/ui/sheet';
import { DropdownMenu, DropdownMenuTrigger, DropdownMenuContent, DropdownMenuItem, DropdownMenuSeparator } from '@/components/ui/dropdown-menu';
import { Wordmark } from './BrandMark';
import { mainNavigation, secondaryNavigation, type NavigationMode } from './navigation';
import { supabase } from './supabase';

type Props = { path: string; go: (path: string) => void; mode: NavigationMode; signedIn: boolean; journey: string; chooseJourney: () => void; openAuth: (mode: 'signin' | 'signup') => void };
export default function AppHeader({ path, go, mode, signedIn, journey, chooseJourney, openAuth }: Props) {
  const [open, setOpen] = useState(false);
  useEffect(() => { setOpen(false); }, [path, mode]);
  const navigate = (url: string) => { setOpen(false); go(url); };
  const account = (action: 'signin' | 'signup') => { setOpen(false); openAuth(action); };
  const primary = mainNavigation.slice(0, 4);
  return <header className="topbar">
    <Button variant="ghost" className="brand" onClick={() => navigate('/')} aria-label="FINNEST home"><Wordmark/></Button>
    {mode === 'desktop' ? <>
      <nav className="desktop-nav" aria-label="Website navigation">{primary.map(item => <Button key={item.path} variant="ghost" aria-current={path === item.path ? 'page' : undefined} onClick={() => navigate(item.path)}>{item.label}</Button>)}
        <DropdownMenu><DropdownMenuTrigger asChild><Button variant="ghost">More<ChevronDown size={16}/></Button></DropdownMenuTrigger><DropdownMenuContent className="finnest-dropdown">{secondaryNavigation.map(item => <DropdownMenuItem key={item.path} onSelect={() => navigate(item.path)}><item.icon/>{item.label}</DropdownMenuItem>)}</DropdownMenuContent></DropdownMenu>
      </nav>
      <div className="header-actions"><Button variant="ghost" className="header-search" size="icon" aria-label="Search experts" onClick={() => navigate('/experts')}><Search/></Button><Button variant="ghost" className="journey-pill" onClick={chooseJourney}>{journey}</Button>
        {signedIn ? <DropdownMenu><DropdownMenuTrigger asChild><Button variant="ghost" className="account-button" aria-label="Account menu"><UserRound/><ChevronDown size={16}/></Button></DropdownMenuTrigger><DropdownMenuContent className="finnest-dropdown"><DropdownMenuItem onSelect={() => navigate('/profile')}><UserRound/>Profile & settings</DropdownMenuItem><DropdownMenuSeparator/><DropdownMenuItem onSelect={() => { void supabase.auth.signOut(); }}><LogOut/>Sign out</DropdownMenuItem></DropdownMenuContent></DropdownMenu> : <div className="header-auth"><Button variant="ghost" onClick={() => account('signin')}>Log In</Button><Button onClick={() => account('signup')}>Sign Up</Button></div>}
      </div>
    </> : <div className="header-actions"><Sheet open={open} onOpenChange={setOpen}><SheetTrigger asChild><Button variant="ghost" className="icon-button" aria-label="Open menu"><Menu/></Button></SheetTrigger><SheetContent className="finnest-drawer" aria-describedby="navigation-description"><SheetTitle><Wordmark/></SheetTitle><SheetDescription id="navigation-description" className="sr-only">FINNEST navigation and account</SheetDescription>
      <nav aria-label="Website navigation" className="drawer-links">{[...mainNavigation.filter(item => item.path !== '/profile'), ...secondaryNavigation].map(item => <Button variant="ghost" key={item.path} aria-current={path === item.path ? 'page' : undefined} onClick={() => navigate(item.path)}><item.icon size={20}/>{item.label}</Button>)}</nav>
      <Button variant="outline" className="drawer-journey" onClick={() => { setOpen(false); chooseJourney(); }}>{journey}<ChevronDown size={18}/></Button>
      <div className="drawer-account">{signedIn ? <Button variant="outline" onClick={() => navigate('/profile')}><UserRound/>Profile & settings</Button> : <><Button onClick={() => account('signup')}>Sign Up</Button><Button variant="outline" onClick={() => account('signin')}>Log In</Button></>}</div>
    </SheetContent></Sheet></div>}
  </header>;
}