import { describe, expect, it } from 'vitest';
import { navigationMode } from '@/finnest/navigation';
describe('FINNEST navigation breakpoints', () => {
  it('uses desktop navigation from exactly 1024px', () => {
    expect(navigationMode(1024)).toBe('desktop'); expect(navigationMode(1023)).toBe('tablet');
  });
  it('uses mobile navigation below 768px', () => {
    expect(navigationMode(767)).toBe('mobile'); expect(navigationMode(768)).toBe('tablet');
  });
});