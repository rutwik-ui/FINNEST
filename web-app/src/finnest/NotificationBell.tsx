import { useState } from 'react';
import { Bell, BellRing, CalendarClock, Megaphone, Route, ShieldCheck, Settings2 } from 'lucide-react';
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover';
import type { FeedItem, NotificationKind, NotificationsApi } from './notifications';

const ICONS: Record<NotificationKind, typeof Bell> = { reminders: CalendarClock, nudges: Route, security: ShieldCheck, announcements: Megaphone };

function ago(time: number) {
  const s = Math.max(0, Math.round((Date.now() - time) / 1000));
  if (s < 60) return 'Just now';
  const m = Math.round(s / 60); if (m < 60) return `${m} min ago`;
  const h = Math.round(m / 60); if (h < 24) return `${h} h ago`;
  const d = Math.round(h / 24); if (d < 7) return d === 1 ? 'Yesterday' : `${d} days ago`;
  return new Date(time).toLocaleDateString('en-IN', { day: 'numeric', month: 'short' });
}

function when(item: FeedItem) {
  if (item.kind === 'nudges') return 'This week';
  if (item.kind === 'reminders') {
    const d = new Date(item.time), today = new Date();
    return d.toDateString() === today.toDateString() ? 'Due today' : `Due ${d.toLocaleDateString('en-IN', { day: 'numeric', month: 'short' })}`;
  }
  return ago(item.time);
}

type Props = { api: NotificationsApi; open: (item: FeedItem) => void; openSettings: () => void; compact?: boolean | undefined };
export function NotificationBell({ api, open, openSettings, compact }: Props) {
  const [visible, setVisible] = useState(false);
  const { items, unread, readSet, markRead } = api;
  const label = unread ? `Notifications, ${unread} unread` : 'Notifications';
  return <Popover open={visible} onOpenChange={setVisible}>
    <PopoverTrigger asChild>
      <button type="button" className={compact ? 'icon-button bell-trigger' : 'bell-trigger bell-trigger-desktop'} aria-label={label}>
        {unread ? <BellRing aria-hidden="true"/> : <Bell aria-hidden="true"/>}
        {unread > 0 && <span className="bell-count" aria-hidden="true">{unread > 9 ? '9+' : unread}</span>}
      </button>
    </PopoverTrigger>
    <PopoverContent align="end" sideOffset={10} collisionPadding={12} className="notify-panel">
      <div className="notify-head">
        <h2>Notifications</h2>
        <button type="button" className="notify-link" disabled={!unread} onClick={() => markRead(items.map(i => i.id))}>Mark all as read</button>
      </div>
      {items.length === 0 ? <div className="notify-empty"><span aria-hidden="true"><Bell/></span><strong>You’re all caught up</strong><p>Reminders, journey updates and announcements will appear here.</p></div>
        : <ul className="notify-list">{items.map(item => { const Icon = ICONS[item.kind]; const isNew = !readSet.has(item.id); return <li key={item.id}>
          <button type="button" className={'notify-item' + (isNew ? ' is-new' : '')} onClick={() => { markRead([item.id]); setVisible(false); open(item); }}>
            <span className={`notify-icon notify-${item.kind}`} aria-hidden="true"><Icon/></span>
            <span className="notify-text"><strong>{item.title}</strong><span>{item.body}</span><time dateTime={new Date(item.time).toISOString()}>{when(item)}</time></span>
            {isNew && <span className="notify-dot"><span className="sr-only">Unread</span></span>}
          </button>
        </li>; })}</ul>}
      <button type="button" className="notify-foot" onClick={() => { setVisible(false); openSettings(); }}><Settings2 aria-hidden="true"/>Notification settings</button>
    </PopoverContent>
  </Popover>;
}
