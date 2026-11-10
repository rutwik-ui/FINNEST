import { CATEGORIES } from './admin.js';

export const ADMIN_PAGE_SIZE = 50;
const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
const TIMESTAMP = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d{1,6})?(?:Z|[+-]\d{2}:\d{2})$/;
const validCursor = c => c && UUID.test(c.id) && TIMESTAMP.test(c.updated_at);

// Keyset ordering remains deterministic for rows with identical timestamps. Cursors
// are validated before interpolation into PostgREST's filter grammar.
export async function loadAdminGuidance(client, {category, status}, cursor = null) {
  if (!(category === 'all' || CATEGORIES.includes(category)) || !['all', 'draft', 'published'].includes(status)) throw new Error('Invalid filter');
  let query = client.from('finnest_guidance').select('id,title,body,category,status,updated_at');
  if (category !== 'all') query = query.eq('category', category);
  if (status !== 'all') query = query.eq('status', status);
  if (cursor) {
    if (!validCursor(cursor)) throw new Error('Invalid cursor');
    query = query.or(`updated_at.lt."${cursor.updated_at}",and(updated_at.eq."${cursor.updated_at}",id.lt.${cursor.id})`);
  }
  const {data,error} = await query.order('updated_at',{ascending:false}).order('id',{ascending:false}).limit(ADMIN_PAGE_SIZE + 1);
  if(error) throw error;
  if(!Array.isArray(data)) throw new Error('Invalid response');
  const items=data.slice(0,ADMIN_PAGE_SIZE);
  const last=items.at(-1);
  if(data.length > ADMIN_PAGE_SIZE && !validCursor(last)) throw new Error('Invalid pagination data');
  return {items, cursor:data.length > ADMIN_PAGE_SIZE ? {id:last.id, updated_at:last.updated_at} : null};
}
