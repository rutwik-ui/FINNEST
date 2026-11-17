import { describe, it, expect } from 'vitest';
import { renderHook } from '@testing-library/react';
// @ts-ignore
import { splitDraft, completedSteps, useCloudSync } from '@/finnest/cloudSync';
describe('account sync', () => {
  it('keeps signed-out sync inactive without reading a missing controller', () => {
    const { result } = renderHook(() => useCloudSync({ session: null, data: {}, applyRemote: () => {}, currentPath: { current: '/' } }));
    expect(result.current.status).toBe('off');
  });
  it('stores resale answers in the resale journey and others in new', () => {
    const p = splitDraft({ budget: { max: '1' }, resaleSeller: { a: { status: 'x' } }, feedbackForm: { t: 'x' } });
    expect(Object.keys(p.new)).toEqual(['budget']);
    expect(Object.keys(p.resale)).toEqual(['resaleSeller']);
  });
  it('counts a step completed only when it has saved entries', () => {
    expect(completedSteps('new', { budget: { max: '50' }, checks: {} })).toEqual(['Requirements & budget']);
  });
});
