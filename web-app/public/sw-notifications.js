// Opens (or focuses) FINNEST at the right page when a notification is tapped.
self.addEventListener('notificationclick', event => {
  event.notification.close();
  const path = (event.notification.data && event.notification.data.path) || '/';
  const target = new URL(path, self.location.origin).href;
  event.waitUntil((async () => {
    const windows = await self.clients.matchAll({ type: 'window', includeUncontrolled: true });
    for (const client of windows) {
      if (new URL(client.url).origin === self.location.origin) {
        await client.focus();
        if ('navigate' in client) { try { await client.navigate(target); } catch { /* cross-state */ } }
        return;
      }
    }
    await self.clients.openWindow(target);
  })());
});
