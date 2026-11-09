// @vitest-environment jsdom
import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
const mocks = vi.hoisted(() => ({ from: vi.fn() }));
vi.mock('../supabase', () => ({ supabase: { from: mocks.from } }));
import Guidance, { loadPublishedGuidance } from '../Guidance';

const item = (id, status = 'published', category = 'general', body = 'Practical information') => ({ id, status, category, title: `Title ${id}`, body, updated_at: '2026-01-01' });
function setup(result) {
  const calls = [];
  const query = { select: vi.fn(() => query), eq: vi.fn((...args) => { calls.push(args); return query; }), order: vi.fn(() => query), limit: vi.fn(() => typeof result === 'function' ? result() : Promise.resolve(result)) };
  mocks.from.mockReturnValue(query);
  return { query, calls };
}
describe('published guidance library', () => {
  beforeEach(() => vi.resetAllMocks());
  afterEach(cleanup);
  it('queries published rows explicitly, applies category before the limit and excludes drafts defensively', async () => {
    const { query, calls } = setup({ data: [item('a'), item('b', 'draft'), item('c', 'published', 'new_property')], error: null });
    const result = await loadPublishedGuidance({ from: mocks.from }, 'general');
    expect(mocks.from).toHaveBeenCalledWith('finnest_guidance');
    expect(calls).toEqual([['status', 'published'], ['category', 'general']]);
    expect(query.limit).toHaveBeenCalledWith(51);
    expect(result.items.map(x => x.id)).toEqual(['a']);
  });
  it('renders plain text, not executable HTML, and hides drafts even if returned', async () => {
    setup({ data: [item('x', 'published', 'general', '<img src=x onerror=alert(1)>'), item('secret', 'draft')], error: null });
    const view = render(<Guidance/>);
    expect(await screen.findByText('<img src=x onerror=alert(1)>')).toBeTruthy();
    expect(view.container.querySelector('img')).toBeNull();
    expect(screen.queryByText('Title secret')).toBeNull();
  });
  it('provides an empty state, category filtering and retry on failure', async () => {
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
  it('never displays more than 50 and labels truncated results', async () => {
    setup({ data: Array.from({ length: 51 }, (_, n) => item(String(n))), error: null });
    render(<Guidance/>);
    expect(await screen.findByText(/Showing the latest 50/)).toBeTruthy();
    expect(screen.queryByText('Title 50')).toBeNull();
  });
  it('does not let an older category request overwrite the newer selection', async () => {
    let resolveFirst;
    let n = 0;
    setup(() => ++n === 1 ? new Promise(resolve => { resolveFirst = resolve; }) : Promise.resolve({ data: [item('new', 'published', 'new_property')], error: null }));
    render(<Guidance/>);
    fireEvent.change(screen.getByLabelText('Category'), { target: { value: 'new_property' } });
    expect(await screen.findByText('Title new')).toBeTruthy();
    resolveFirst({ data: [item('old')], error: null });
    await waitFor(() => expect(screen.queryByText('Title old')).toBeNull());
  });
});
