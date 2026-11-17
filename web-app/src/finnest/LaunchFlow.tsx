import { useState } from 'react';
import { ArrowLeft, ArrowRight } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Wordmark } from './BrandMark';
import { AccountForm } from './Authentication';
import type { LaunchPhase } from './launchRules';
import building from '@/assets/home-building.jpg';
import planning from '@/assets/planning-couple.jpg';
import family from '@/assets/family-keys.jpg';
import { onboardingSlides } from './launchRules';
const images = { building, planning, family };
type Slide = { title: string; body: string; image: 'building' | 'planning' | 'family' };
export default function LaunchFlow({ phase, slides = onboardingSlides, guestEnabled, onAccount, onComplete }: { phase: LaunchPhase; slides?: readonly Slide[]; guestEnabled: boolean; onAccount: () => void; onComplete: () => void }) {
  const [slide, setSlide] = useState(0), [touchStart, setTouchStart] = useState<number | null>(null);
  const current = slides[Math.min(slide, slides.length - 1)];
  if (phase === 'splash') return <div className="launch-splash" role="status" aria-label="Loading FINNEST"><Wordmark/><span className="launch-loading"/></div>;
  return <div className="launch-shell"><header className="launch-header"><Wordmark/>{phase === 'onboarding' && <Button variant="ghost" onClick={onAccount}>Skip</Button>}</header>
    {phase === 'account' ? <main className="launch-account"><span className="eyebrow">YOUR HOME-BUYING JOURNEY</span><h1>Your journey, connected<span className="red-dot">.</span></h1><p>Keep your plans and questions together in your FINNEST account.</p><AccountForm onSuccess={onComplete}/>{guestEnabled && <Button variant="outline" className="launch-guest" onClick={onComplete}>Continue as guest</Button>}</main> : current ? <main className="launch-intro" onTouchStart={e => setTouchStart(e.touches[0]?.clientX ?? null)} onTouchEnd={e => { const end = e.changedTouches[0]?.clientX; if (touchStart !== null && end !== undefined && Math.abs(end - touchStart) > 60) setSlide(v => Math.max(0, Math.min(slides.length - 1, v + (end < touchStart ? 1 : -1)))); setTouchStart(null); }}>
      <img className="launch-photo" src={images[current.image]} alt={current.image === 'building' ? 'An apartment building' : current.image === 'planning' ? 'A couple planning their home purchase' : 'A family receiving home keys'} width={1200} height={800}/>
      <div className="launch-copy" aria-live="polite"><span className="eyebrow">YOUR HOME-BUYING JOURNEY</span><h1>{current.title}</h1><p>{current.body}</p></div>
      <div className="launch-progress" aria-label={`Introduction ${slide + 1} of ${slides.length}`}>{slides.map((_, i) => <span key={i} className={i === slide ? 'active' : ''}/>)}</div>
      <div className="launch-controls">{slide > 0 && <Button variant="outline" size="icon" aria-label="Previous introduction" onClick={() => setSlide(v => Math.max(0, v - 1))}><ArrowLeft/></Button>}<Button onClick={() => slide === slides.length - 1 ? onAccount() : setSlide(v => Math.min(slides.length - 1, v + 1))}>{slide === slides.length - 1 ? 'Get Started' : 'Continue'}<ArrowRight/></Button></div>
    </main> : null}
  </div>;
}