// @vitest-environment jsdom
import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
const mocks = vi.hoisted(() => ({ from: vi.fn() }));
vi.mock('../supabase', () => ({ supabase: { from: mocks.from } }));
import Guidance, { loadPublishedGuidance } from '../Guidance';

const id = n => `00000000-0000-4000-8000-${String(n).padStart(12, '0')}`;
const item = (n, status = 'published', category = 'general', body = 'Practical information') => ({ id: id(n), status, category, title: `Title ${n}`, body, updated_at: '2026-01-01T10:20:30.123456+00:00' });
function setup(result) {
  const calls = [];
  const query = { select: vi.fn(() => query), eq: vi.fn((...args) => { calls.push(args); return query; }), or: vi.fn(() => query), order: vi.fn(() => query), limit: vi.fn(() => typeof result === 'function' ? result() : Promise.resolve(result)) };
  mocks.from.mockReturnValue(query);
  return { query, calls };
}
describe('published guidance library', () => {
  beforeEach(() => vi.resetAllMocks());
  afterEach(cleanup);
  it('queries published rows explicitly, applies category and excludes unexpected drafts', async () => {
    const { query, calls } = setup({ data: [item(1), item(2, 'draft'), item(3, 'published', 'new_property')], error: null });
    const result = await loadPublishedGuidance({ from: mocks.from }, 'general');
    expect(mocks.from).toHaveBeenCalledWith('finnest_guidance');
    expect(calls).toEqual([['status', 'published'], ['category', 'general']]);
    expect(query.limit).toHaveBeenCalledWith(51);
    expect(result.items.map(x => x.id)).toEqual([id(1)]);
  });
  it('uses both timestamp and uuid for stable pagination and rejects unsafe cursors', async () => {
    const { query } = setup({ data: [], error: null });
    await loadPublishedGuidance({ from: mocks.from }, 'general', item(42));
    expect(query.or).toHaveBeenCalledWith(`updated_at.lt."2026-01-01T10:20:30.123456+00:00",and(updated_at.eq."2026-01-01T10:20:30.123456+00:00",id.lt.${id(42)})`);
    await expect(loadPublishedGuidance({ from: mocks.from }, 'all', { id: 'x),status.eq.draft', updated_at: '2026-01-01T10:20:30Z' })).rejects.toThrow('Invalid cursor');
    await expect(loadPublishedGuidance({ from: mocks.from }, 'all', { id: id(1), updated_at: 'x' })).rejects.toThrow('Invalid cursor');
  });
  it('renders plain text, not HTML, and hides drafts', async () => {
    setup({ data: [item(1, 'published', 'general', '<img src=x onerror=alert(1)>'), item(2, 'draft')], error: null });
    const view = render(<Guidance/>);
    expect(await screen.findByText('<img src=x onerror=alert(1)>')).toBeTruthy();
    expect(view.container.querySelector('img')).toBeNull();
    expect(screen.queryByText('Title 2')).toBeNull();
  });
  it('supports error retry and category filters', async () => {
    let fail = true;
    const { calls } = setup(() => Promise.resolve(fail ? { data: null, error: new Error('offline') } : { data: [], error: null }));
    render(<Guidance/>);
    expect(await screen.findByRole('alert')).toBeTruthy();
    fail = false; fireEvent.click(screen.getByText('Try again'));
    expect(await screen.findByText('No published guidance yet')).toBeTruthy();
    fireEvent.change(screen.getByLabelText('Category'), { target: { value: 'resale_property' } });
    await waitFor(() => expect(calls).toContainEqual(['category', 'resale_property']));
    expect(await screen.findByText('No published guidance yet')).toBeTruthy();
  });
  it('loads additional results, deduplicates IDs and retains results on an incremental error', async () => {
    let n = 0;
    setup(() => Promise.resolve(++n === 1 ? { data: Array.from({ length: 51 }, (_, i) => item(i)), error: null } : n === 2 ? { error: new Error('offline') } : { data: [item(49), item(50), item(51)], error: null }));
    render(<Guidance/>);
    fireEvent.click(await screen.findByText('Load more guidance'));
    expect(await screen.findByRole('alert')).toBeTruthy();
    expect(screen.getByText('Title 0')).toBeTruthy();
    fireEvent.click(screen.getByText('Retry loading more'));
    expect(await screen.findByText('Title 51')).toBeTruthy();
    expect(screen.getAllByText('Title 49')).toHaveLength(1);
    expect(screen.queryByText('Load more guidance')).toBeNull();
  });
  it('discards an in-flight next page after changing category', async () => {
    let resolveMore;
    let n = 0;
    setup(() => {
      n++;
      if (n === 1) return Promise.resolve({ data: Array.from({ length: 51 }, (_, i) => item(i)), error: null });
      if (n === 2) return new Promise(resolve => { resolveMore = resolve; });
      return Promise.resolve({ data: [item(99, 'published', 'resale_property')], error: null });
    });
    render(<Guidance/>);
    fireEvent.click(await screen.findByText('Load more guidance'));
    fireEvent.change(screen.getByLabelText('Category'), { target: { value: 'resale_property' } });
    expect(await screen.findByText('Title 99')).toBeTruthy();
    resolveMore({ data: [item(88)], error: null });
    await waitFor(() => expect(screen.queryByText('Title 88')).toBeNull());
    expect(screen.queryByText('Title 0')).toBeNull();
  });
  it('does not let an older category request overwrite the newer selection', async () => {
    let resolveFirst;
    let n = 0;
    setup(() => ++n === 1 ? new Promise(resolve => { resolveFirst = resolve; }) : Promise.resolve({ data: [item(99, 'published', 'new_property')], error: null }));
    render(<Guidance/>);
    fireEvent.change(screen.getByLabelText('Category'), { target: { value: 'new_property' } });
    expect(await screen.findByText('Title 99')).toBeTruthy();
    resolveFirst({ data: [item(1)], error: null });
    await waitFor(() => expect(screen.queryByText('Title 1')).toBeNull());
  });
});
