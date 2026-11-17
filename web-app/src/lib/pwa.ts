export function refuseServiceWorker({ production, hostname, iframe, search }: { production: boolean; hostname: string; iframe: boolean; search: string }) {
  const host = hostname.toLowerCase();
  const label = host.split('.')[0] ?? '';
  const lovableDomain = ['lovable.app', 'gpt-eng.com', 'gptengineer.run'].some(d => host.endsWith(`.${d}`));
  return !production || iframe || new URLSearchParams(search).get('sw') === 'off'
    || (lovableDomain && (/^(?:id-preview|preview)(?:-[a-z0-9]+)?--.+$/.test(label) || label === 'project--12e20cb0-845a-4f25-bc32-9f8ec393a7d1-dev'))
    || ['lovableproject.com', 'lovableproject-dev.com', 'beta.lovable.dev'].some(d => host === d || host.endsWith(`.${d}`));
}

export async function registerFinnestOffline() {
  if (!('serviceWorker' in navigator)) return;
  if (refuseServiceWorker({ production: import.meta.env.PROD, hostname: location.hostname, iframe: window.self !== window.top, search: location.search })) {
    const registrations = await navigator.serviceWorker.getRegistrations();
    await Promise.allSettled(registrations.filter(r => [r.active, r.waiting, r.installing].some(w => w && new URL(w.scriptURL).pathname === '/sw.js')).map(r => r.unregister()));
    return;
  }
  const { registerSW } = await import('virtual:pwa-register');
  registerSW({ immediate: true });
}