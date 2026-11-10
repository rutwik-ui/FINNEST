import React from 'react';
import { render, fireEvent, screen, waitFor } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { canImport, checkImportFile, importRequestSchema, previewImport, sheetTable, suggestMapping } from '@/finnest/bulkImport';
import { readImportFile } from '@/finnest/importFile';
import { persistImport } from '@/finnest/bulkImport.server';
import { NEW_ROADMAP, RESALE_ROADMAP, roadmapFor } from '@/finnest/roadmap';

const saveFn = vi.hoisted(() => vi.fn());
vi.mock('@tanstack/react-start', () => ({ useServerFn: () => saveFn }));
vi.mock('@/finnest/bulkImport.functions', () => ({ importAdminRows: vi.fn() }));
import BulkImport from '@/finnest/AdminBulkImport';

const ids: [string, string] = ['11111111-1111-4111-8111-111111111111', '22222222-2222-4222-8222-222222222222'];
const guidance = { id: ids[0], title: 'Resale review', body: 'Ask a qualified lawyer about title records.', category: 'resale_property' as const, status: 'published' as const };

describe('Bulk guidance and expert imports', () => {
  it('reads quoted CSV fields, including multiline guidance', async () => {
    const file = { name: 'guidance.csv', size: 100, text: async () => 'title,body,category,status\r\n"Title, with comma","First line\nSecond line",resale_property,published\r\n' } as File;
    const sheets = await readImportFile(file);
    const table = sheetTable(sheets[0]?.data ?? []);
    const rows = previewImport('guidance', table, suggestMapping('guidance', table.headers), ids);
    expect(rows[0]?.data).toMatchObject({ title: 'Title, with comma', body: 'First line\nSecond line', category: 'resale_property', status: 'published' });
  });
  it('leaves imported items private when status is omitted', () => {
    const table = sheetTable([['title', 'body'], ['Planning', 'Keep notes.']]);
    expect(previewImport('guidance', table, suggestMapping('guidance', table.headers), ids)[0]?.data?.status).toBe('draft');
  });
  it('rejects missing required data without sending it for storage', () => {
    expect(importRequestSchema.safeParse({ kind: 'guidance', rows: [{ ...guidance, body: '' }] }).success).toBe(false);
    expect(importRequestSchema.safeParse({ kind: 'experts', rows: [{ id: ids[0], name: 'Expert', specialty: 'Property lawyer', email: 'not-an-email' }] }).success).toBe(false);
  });
  it('rejects duplicate file rows and unsupported files', () => {
    const table = sheetTable([['title', 'body'], ['A', 'B'], ['A', 'B']]);
    expect(previewImport('guidance', table, suggestMapping('guidance', table.headers), ids)[1]?.errors.length).toBeGreaterThan(0);
    expect(() => checkImportFile({ name: 'old.xls', size: 10 })).toThrow('.xlsx');
  });
  it('permits guidance imports only for verified content managers and super admins', () => {
    expect(canImport(['content_manager'], 'aal2', 'guidance')).toBe(true);
    expect(canImport(['super_admin'], 'aal2', 'guidance')).toBe(true);
    expect(canImport(['expert_manager'], 'aal2', 'guidance')).toBe(false);
    expect(canImport(['support_manager'], 'aal2', 'guidance')).toBe(false);
    expect(canImport(['super_admin'], 'aal1', 'guidance')).toBe(false);
  });
  it('permits expert imports only for verified expert managers and super admins', () => {
    expect(canImport(['expert_manager'], 'aal2', 'experts')).toBe(true);
    expect(canImport(['super_admin'], 'aal2', 'experts')).toBe(true);
    expect(canImport(['content_manager'], 'aal2', 'experts')).toBe(false);
    expect(canImport([], 'aal2', 'experts')).toBe(false);
  });
  it('saves rows atomically and verifies readback with stable IDs for retries', async () => {
    const upsert = vi.fn().mockResolvedValue({ error: null });
    const read = vi.fn().mockResolvedValue({ data: [guidance], error: null });
    const client = { from: vi.fn(() => ({ upsert, select: () => ({ in: read }) })) };
    // The mock implements only the Supabase operations this persistence function uses.
    const saved = await persistImport(client as never, { kind: 'guidance', rows: [guidance] });
    expect(upsert).toHaveBeenCalledWith([guidance], { onConflict: 'id', ignoreDuplicates: true });
    expect(read).toHaveBeenCalledWith('id', [ids[0]]);
    expect(saved).toEqual({ count: 1, published: 1 });
    await persistImport(client as never, { kind: 'guidance', rows: [guidance] });
    expect(upsert.mock.calls[1]?.[0][0].id).toBe(ids[0]);
  });
  it('never reports successful import when saved data does not match', async () => {
    const client = { from: () => ({ upsert: async () => ({ error: null }), select: () => ({ in: async () => ({ data: [{ ...guidance, body: 'Different content' }], error: null }) }) }) };
    await expect(persistImport(client as never, { kind: 'guidance', rows: [guidance] })).rejects.toThrow('Nothing was overwritten');
  });
  it('does not send public rows until publication is explicitly approved', async () => {
    saveFn.mockResolvedValue({ count: 1, published: 1 });
    render(<BulkImport kind="guidance" />);
    fireEvent.click(screen.getByRole('button', { name: 'Import CSV / Excel' }));
    const file = new File(['title,body,category,status\nResale review,Ask a lawyer,resale_property,published'], 'guidance.csv', { type: 'text/csv' });
    Object.defineProperty(file, 'text', { value: async () => 'title,body,category,status\nResale review,Ask a lawyer,resale_property,published' });
    fireEvent.change(screen.getByLabelText('CSV or Excel file'), { target: { files: [file] } });
    await waitFor(() => expect(screen.getByRole('button', { name: 'Import 1 records' })).toBeDisabled());
    expect(saveFn).not.toHaveBeenCalled();
    fireEvent.click(screen.getByRole('checkbox'));
    fireEvent.click(screen.getByRole('button', { name: 'Import 1 records' }));
    await waitFor(() => expect(saveFn).toHaveBeenCalledTimes(1));
    expect(saveFn.mock.calls[0]?.[0].data.rows[0]).toMatchObject({ title: 'Resale review', category: 'resale_property', status: 'published' });
    await screen.findByText('1 records saved');
  });
});

describe('New and resale roadmap destinations', () => {
  it('keeps all eight original new-property destinations', () => {
    expect(roadmapFor('New Property')).toBe(NEW_ROADMAP);
    expect(NEW_ROADMAP.map(step => step.path)).toEqual(['/journey', '/journey', '/projects', '/visit', '/loan', '/legal', '/financial', '/review']);
  });
  it('includes all six existing resale areas and the resale summary', () => {
    expect(roadmapFor('Resale Property')).toBe(RESALE_ROADMAP);
    expect(RESALE_ROADMAP.map(step => step.path)).toEqual(['/resale-requirements', '/resale-condition', '/resale-seller', '/resale-society', '/resale-legal', '/resale-handover', '/resale-summary']);
  });
});