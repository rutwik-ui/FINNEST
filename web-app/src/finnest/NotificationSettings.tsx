import { useState } from 'react';
import { BellRing, CalendarClock, Mail, Megaphone, Route, ShieldCheck } from 'lucide-react';
import { Switch } from '@/components/ui/switch';
import type { NotificationKind, NotificationsApi } from './notifications';

const KINDS: { key: NotificationKind; title: string; body: string; icon: typeof BellRing }[] = [
  { key: 'reminders', title: 'Reminders', body: 'When a reminder you added is due, and if it becomes overdue.', icon: CalendarClock },
  { key: 'nudges', title: 'Journey progress', body: 'A weekly prompt about your next unfinished step.', icon: Route },
  { key: 'security', title: 'Account & support', body: 'New sign-ins, password resets, account changes and replies to your support requests.', icon: ShieldCheck },
  { key: 'announcements', title: 'Announcements', body: 'Important updates from the FINNEST team.', icon: Megaphone },
];

export function NotificationSettings({ api, signedIn, openAuth }: { api: NotificationsApi; signedIn: boolean; openAuth: (mode: 'signin' | 'signup') => void }) {
  const { prefs, setPrefs, permission, requestPermission } = api;
  const [status, setStatus] = useState<'' | 'saved' | 'error'>('');
  const save = async (next: typeof prefs) => { setStatus(''); const r = await setPrefs(next); setStatus(r.ok ? 'saved' : 'error'); };
  const browserOn = prefs.browser && permission === 'granted';

  const toggleBrowser = async (on: boolean) => {
    if (!on) { await save({ ...prefs, browser: false }); return; }
    const result = permission === 'granted' ? 'granted' : await requestPermission();
    await save({ ...prefs, browser: result === 'granted' });
  };

  return <>
    <div className="page-head"><span className="eyebrow">SETTINGS</span><h1>Notifications<span className="red-dot">.</span></h1><p>Choose what FINNEST tells you about, and where.</p></div>
    <section className="card form-card notify-settings">
      <h2>Where you’re notified</h2>
      <div className="notify-row">
        <span className="notify-row-icon" aria-hidden="true"><BellRing/></span>
        <div className="notify-row-text"><strong id="nb-title">Device notifications</strong>
          <p id="nb-desc">{permission === 'unsupported' ? 'This browser doesn’t support notifications.'
            : permission === 'denied' ? 'Notifications are blocked for this site. Allow them in your browser’s site settings, then come back here.'
            : 'Pop-up alerts on this device while FINNEST is open in a tab. Everything also appears under the bell.'}</p></div>
        <Switch checked={browserOn} disabled={permission === 'unsupported' || permission === 'denied'} onCheckedChange={v => { void toggleBrowser(v); }} aria-labelledby="nb-title" aria-describedby="nb-desc"/>
      </div>
      <div className="notify-row">
        <span className="notify-row-icon" aria-hidden="true"><Mail/></span>
        <div className="notify-row-text"><strong>Email</strong><p>Account emails, such as confirming your email address and resetting your password, are always sent so you can keep access to your account.</p></div>
        <span className="notify-row-fixed">Always on</span>
      </div>
    </section>
    <section className="card form-card notify-settings">
      <h2>What you’re notified about</h2>
      {KINDS.map(k => <div className="notify-row" key={k.key}>
        <span className="notify-row-icon" aria-hidden="true"><k.icon/></span>
        <div className="notify-row-text"><strong id={`nk-${k.key}`}>{k.title}</strong><p>{k.body}</p></div>
        <Switch checked={prefs[k.key]} onCheckedChange={v => { void save({ ...prefs, [k.key]: v }); }} aria-labelledby={`nk-${k.key}`}/>
      </div>)}
      <p className="notify-status" role="status">{status === 'saved' ? (signedIn ? 'Saved to your account.' : 'Saved on this device.') : status === 'error' ? 'Couldn’t save to your account. Check your connection and try again.' : ''}</p>
    </section>
    {!signedIn && <section className="card form-card notify-settings notify-guest"><p>You’re using FINNEST as a guest, so these settings are kept in this browser only. <button type="button" className="notify-link" onClick={() => openAuth('signin')}>Log in</button> to use them on all your devices.</p></section>}
  </>;
}
