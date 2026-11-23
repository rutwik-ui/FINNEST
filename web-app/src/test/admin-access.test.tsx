import React from 'react';
import { render, waitFor, cleanup } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { canAccessAdmin } from '@/finnest/admin';

const mocks = vi.hoisted(() => ({
  from: vi.fn(),
  assurance: vi.fn(),
  roles: ['super_admin'] as string[],
}));
vi.mock('@/finnest/supabase', () => ({ supabase: {
  from: mocks.from,
  auth: { mfa: { getAuthenticatorAssuranceLevel: mocks.assurance } },
} }));
vi.mock('@/finnest/Admin', () => ({ default: () => null }));
vi.mock('@/finnest/AdminBulkImport', () => ({ default: () => null }));
vi.mock('@/finnest/AdminBuyers', () => ({ default: () => null }));
import AdminConsole, { canOpen } from '@/finnest/AdminConsole';

beforeEach(() => {
  vi.clearAllMocks();
  mocks.roles = ['super_admin'];
  mocks.from.mockImplementation((table: string) => ({
    select: () => table === 'finnest_admin_roles'
      ? { eq: async () => ({ data: mocks.roles.map(role => ({ role })), error: null }) }
      : { count: 0, eq: async () => ({ count: 0 }), neq: async () => ({ count: 0 }) },
  }));
});
afterEach(cleanup);

describe('password-only administrator authorization', () => {
  it('keeps signed-out visitors outside administrator data', () => {
    render(<AdminConsole session={null} authLoading={false} go={vi.fn()} />);
    expect(mocks.from).not.toHaveBeenCalled();
    expect(mocks.assurance).not.toHaveBeenCalled();
  });
  it('permits an assigned super admin without an authenticator condition', () => {
    expect(canAccessAdmin(['super_admin'])).toBe(true);
    expect(canOpen(['super_admin'], 'buyers')).toBe(true);
    expect(canOpen(['super_admin'], 'users')).toBe(true);
  });
  it('denies unassigned buyer roles and keeps support staff out of role management', () => {
    expect(canAccessAdmin([])).toBe(false);
    expect(canAccessAdmin(['buyer'])).toBe(false);
    expect(canOpen(['support_manager'], 'guidance')).toBe(false);
  });
  it('loads administrator data after a verified role without calling an authenticator', async () => {
    render(<AdminConsole session={{ user: { id: 'test-admin', email: 'admin@example.test' }, access_token: 'test-session' }} authLoading={false} go={vi.fn()} />);
    await waitFor(() => expect(mocks.from).toHaveBeenCalledWith('finnest_profiles'));
    expect(mocks.assurance).not.toHaveBeenCalled();
  });
  it('does not load administrator data for a signed-in buyer without a role', async () => {
    mocks.roles = [];
    render(<AdminConsole session={{ user: { id: 'test-buyer' }, access_token: 'test-session' }} authLoading={false} go={vi.fn()} />);
    await waitFor(() => expect(mocks.from).toHaveBeenCalledWith('finnest_admin_roles'));
    expect(mocks.from).not.toHaveBeenCalledWith('finnest_profiles');
    expect(mocks.assurance).not.toHaveBeenCalled();
  });
});