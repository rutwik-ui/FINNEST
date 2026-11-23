import { useCallback, useEffect, useRef, useState, type ReactNode } from 'react';
import { WifiOff, RotateCw } from 'lucide-react';

// navigator.onLine only knows a network exists, so confirm FINNEST's own server answers.
// robots.txt is never cached by the service worker, so this always reaches the network.
export async function hasInternet(timeoutMs = 6000): Promise<boolean> {
  if (typeof navigator !== 'undefined' && !navigator.onLine) return false;
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeoutMs);
  try {
    const res = await fetch(`/robots.txt?online=${Date.now()}`, { method: 'HEAD', cache: 'no-store', signal: controller.signal });
    return res.ok || res.status === 405;
  } catch {
    return false;
  } finally {
    clearTimeout(timer);
  }
}

/** FINNEST is an online-only service: nothing below this renders without a working connection. */
export function OnlineGate({ children }: { children: ReactNode }) {
  const [online, setOnline] = useState(() => typeof navigator === 'undefined' || navigator.onLine);
  const [checking, setChecking] = useState(false);
  const busy = useRef(false);

  const check = useCallback(async () => {
    if (busy.current) return;
    busy.current = true; setChecking(true);
    const ok = await hasInternet();
    busy.current = false; setChecking(false); setOnline(ok);
  }, []);

  useEffect(() => {
    const goOffline = () => setOnline(false);
    const maybeOnline = () => { void check(); };
    window.addEventListener('offline', goOffline);
    window.addEventListener('online', maybeOnline);
    document.addEventListener('visibilitychange', maybeOnline);
    void check();
    return () => {
      window.removeEventListener('offline', goOffline);
      window.removeEventListener('online', maybeOnline);
      document.removeEventListener('visibilitychange', maybeOnline);
    };
  }, [check]);

  // While blocked, keep trying quietly so the app returns as soon as the connection does.
  useEffect(() => {
    if (online) return;
    const id = setInterval(() => { void check(); }, 8000);
    return () => clearInterval(id);
  }, [online, check]);

  return <>
    <div style={{ display: online ? undefined : 'none' }}>{children}</div>
    {!online && <main className="offline-gate" role="alert" aria-live="assertive">
      <div className="offline-gate-card">
        <span className="offline-gate-icon" aria-hidden="true"><WifiOff/></span>
        <h1>No internet connection</h1>
        <p>FINNEST needs the internet to work. Turn on mobile data or Wi-Fi, then try again.</p>
        <button type="button" className="offline-gate-retry" onClick={() => { void check(); }} disabled={checking}>
          <RotateCw aria-hidden="true" className={checking ? 'is-spinning' : undefined}/>{checking ? 'Checking…' : 'Try again'}
        </button>
        <small>We'll reconnect automatically when you're back online.</small>
      </div>
    </main>}
  </>;
}
