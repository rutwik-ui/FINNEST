import { useEffect, useMemo, useState, type ComponentType } from 'react';
import * as DialogPrimitive from '@radix-ui/react-dialog';
import { Command } from 'cmdk';
import { Search, CornerDownLeft, ArrowRight, BookOpen, UserRound } from 'lucide-react';
import { mainNavigation, secondaryNavigation } from './navigation';
import { roadmapFor, type RoadmapStep } from './roadmap';
import { ROLES } from './Experts';
import { supabase } from './supabase';

type Step = RoadmapStep;
type Article = { id: string; title: string; category: string | null };
type Props = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  journey: string;
  go: (path: string) => void;
  openStep: (step: Step) => void;
};

const PAGE_HINTS: Record<string, string> = {
  '/': 'Dashboard and next step',
  '/journey': 'Requirements, budget and developer checks',
  '/projects': 'Your shortlisted projects',
  '/experts': 'Lawyers, loan advisors, inspectors and more',
  '/profile': 'Account, data and settings',
  '/guidance': 'Articles on buying a home',
  '/reminders': 'Notes for things to follow up',
  '/about': 'What FINNEST does',
  '/privacy': 'How your data is used',
};

let articleCache: Article[] | null = null;

// Every typed word must appear in the item's title or description; title matches rank first.
function matchScore(_value: string, search: string, keywords?: string[]) {
  const terms = search.toLowerCase().split(/\s+/).filter(Boolean);
  if (!terms.length) return 1;
  const [title = '', ...rest] = (keywords ?? []).map(k => k.toLowerCase());
  const all = `${title} ${rest.join(' ')}`;
  if (!terms.every(t => all.includes(t))) return 0;
  const words = title.split(/[^a-z0-9]+/);
  if (terms.every(t => words.some(w => w.startsWith(t)))) return 1;
  if (terms.every(t => title.includes(t))) return 0.8;
  return 0.4;
}

export function SiteSearch({ open, onOpenChange, journey, go, openStep }: Props) {
  const [query, setQuery] = useState('');
  const [articles, setArticles] = useState<Article[]>(articleCache ?? []);
  const steps = useMemo(() => roadmapFor(journey), [journey]);

  useEffect(() => { if (!open) setQuery(''); }, [open]);

  // Published guidance is loaded once, when search is first opened.
  useEffect(() => {
    if (!open || articleCache) return;
    let active = true;
    supabase.from('finnest_guidance').select('id,title,category').eq('status', 'published')
      .order('updated_at', { ascending: false }).limit(200)
      .then(({ data, error }) => {
        if (error || !Array.isArray(data)) return;
        articleCache = data as Article[];
        if (active) setArticles(articleCache);
      });
    return () => { active = false; };
  }, [open]);

  const run = (action: () => void) => { onOpenChange(false); action(); };
  const pages = [...mainNavigation, ...secondaryNavigation];

  return <DialogPrimitive.Root open={open} onOpenChange={onOpenChange}>
    <DialogPrimitive.Portal>
      <DialogPrimitive.Overlay className="site-search-overlay"/>
      <DialogPrimitive.Content className="site-search" aria-describedby={undefined}>
        <DialogPrimitive.Title className="sr-only">Search FINNEST</DialogPrimitive.Title>
        <Command label="Search FINNEST" loop filter={matchScore}>
          <div className="site-search-field">
            <Search aria-hidden="true"/>
            <Command.Input value={query} onValueChange={setQuery} placeholder="Search FINNEST" autoFocus/>
            <kbd>Esc</kbd>
          </div>
          <Command.List className="site-search-list">
            <Command.Empty className="site-search-empty">
              <strong>No results for “{query}”</strong>
              <span>Try a step name like “loan”, “legal” or “visit”.</span>
            </Command.Empty>

            <Command.Group heading={`Your journey · ${journey}`}>
              {steps.map((step, i) => <Command.Item key={step.label} value={`step ${step.label}`} keywords={[step.label, `step ${i + 1}`, journey]} onSelect={() => run(() => openStep(step))}>
                <span className="site-search-step">{i + 1}</span>
                <span className="site-search-text"><span>{step.label}</span><small>Step {i + 1} of {steps.length}</small></span>
                <ArrowRight className="site-search-go" aria-hidden="true"/>
              </Command.Item>)}
            </Command.Group>

            <Command.Group heading="Pages">
              {pages.map(page => <Item key={page.path} value={`page ${page.label}`} keywords={[page.label, PAGE_HINTS[page.path] ?? '']} icon={page.icon} title={page.label} hint={PAGE_HINTS[page.path] ?? page.label} onSelect={() => run(() => go(page.path))}/>)}
            </Command.Group>

            <Command.Group heading="Experts">
              {ROLES.map(([role, about]) => <Item key={role} value={`expert ${role}`} keywords={[role, about, 'expert']} icon={UserRound} title={role} hint={about} onSelect={() => run(() => go('/experts'))}/>)}
            </Command.Group>

            {articles.length > 0 && <Command.Group heading="Guidance">
              {articles.map(article => <Item key={article.id} value={`guide ${article.title} ${article.id}`} keywords={[article.title, article.category ?? '', 'guidance']} icon={BookOpen} title={article.title} hint={article.category ?? 'Guidance'} onSelect={() => run(() => go('/guidance'))}/>)}
            </Command.Group>}
          </Command.List>
          <div className="site-search-foot" aria-hidden="true">
            <span><kbd>↑</kbd><kbd>↓</kbd>to move</span>
            <span><kbd><CornerDownLeft/></kbd>to open</span>
          </div>
        </Command>
      </DialogPrimitive.Content>
    </DialogPrimitive.Portal>
  </DialogPrimitive.Root>;
}

function Item({ value, keywords = [], icon: Icon, title, hint, onSelect }: { value: string; keywords?: string[]; icon: ComponentType<{ 'aria-hidden'?: boolean | 'true' }>; title: string; hint: string; onSelect: () => void }) {
  return <Command.Item value={value} keywords={keywords} onSelect={onSelect}>
    <span className="site-search-icon"><Icon aria-hidden="true"/></span>
    <span className="site-search-text"><span>{title}</span><small>{hint}</small></span>
    <ArrowRight className="site-search-go" aria-hidden="true"/>
  </Command.Item>;
}

// Opens search from Ctrl/⌘+K anywhere, or "/" when the user is not typing in a field.
export function useSearchShortcut(setOpen: (open: boolean) => void) {
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      const target = e.target as HTMLElement | null;
      const typing = !!target && (target.isContentEditable || /^(INPUT|TEXTAREA|SELECT)$/.test(target.tagName));
      if ((e.key === 'k' || e.key === 'K') && (e.metaKey || e.ctrlKey)) { e.preventDefault(); setOpen(true); }
      else if (e.key === '/' && !typing && !e.metaKey && !e.ctrlKey && !e.altKey) { e.preventDefault(); setOpen(true); }
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [setOpen]);
}
