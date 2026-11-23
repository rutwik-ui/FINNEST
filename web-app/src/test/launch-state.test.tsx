import { act, cleanup, renderHook } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { useLaunchFlow } from '@/finnest/useLaunchFlow';
import { ONBOARDING_KEY } from '@/finnest/launchRules';

afterEach(() => { cleanup(); vi.useRealTimers(); localStorage.clear(); });
describe('Launch state persistence', () => {
  it('waits for published settings before choosing the first-visit path', () => {
    vi.useFakeTimers();
    const { result, rerender } = renderHook(({ ready }) => useLaunchFlow({ signedIn: false, authLoading: false, settingsReady: ready, version: 2 }), { initialProps: { ready: false } });
    act(() => { vi.advanceTimersByTime(1500); });
    expect(result.current.phase).toBe('splash');
    rerender({ ready: true });
    expect(result.current.phase).toBe('onboarding');
    expect(localStorage.getItem(ONBOARDING_KEY)).toBeNull();
  });
  it('stores completion only when the visitor finishes account or guest continuation', () => {
    vi.useFakeTimers();
    const { result } = renderHook(() => useLaunchFlow({ signedIn: false, authLoading: false, settingsReady: true, version: 2 }));
    act(() => { vi.advanceTimersByTime(1500); result.current.account(); });
    expect(result.current.phase).toBe('account');
    expect(localStorage.getItem(ONBOARDING_KEY)).toBeNull();
    act(() => { result.current.complete(); });
    expect(result.current.phase).toBe('ready');
    expect(localStorage.getItem(ONBOARDING_KEY)).toBe('2');
  });
  it('does not ask an already signed-in buyer to sign in again after replay', () => {
    vi.useFakeTimers();
    const { result } = renderHook(() => useLaunchFlow({ signedIn: true, authLoading: false, settingsReady: true, version: 1 }));
    act(() => { vi.advanceTimersByTime(1500); });
    expect(result.current.phase).toBe('ready');
    act(() => { result.current.replay(); });
    expect(result.current.phase).toBe('onboarding');
    act(() => { result.current.account(); });
    expect(result.current.phase).toBe('ready');
  });
});