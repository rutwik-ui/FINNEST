import React, { useEffect, useMemo, useRef, useState } from 'react';
import { useServerFn } from '@tanstack/react-start';
import { Upload, Download, FileSpreadsheet, CheckCircle2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Dialog, DialogContent, DialogDescription, DialogTitle } from '@/components/ui/dialog';
import { IMPORT_FIELDS, previewImport, sheetTable, suggestMapping, templateCsv } from './bulkImport';
import { readImportFile } from './importFile';
import { importAdminRows } from './bulkImport.functions';

export default function BulkImport({ kind, onImported, disabled = false }) {
  const [open, setOpen] = useState(false), [sheets, setSheets] = useState([]), [sheet, setSheet] = useState(0);
  const [mapping, setMapping] = useState({}), [ids, setIds] = useState([]), [filename, setFilename] = useState('');
  const [busy, setBusy] = useState(false), [error, setError] = useState(''), [result, setResult] = useState(null), [attempted, setAttempted] = useState(false);
  const [publishConsent, setPublishConsent] = useState(false);
  const input = useRef(null), request = useRef(0);
  const importRows = useServerFn(importAdminRows);
  const table = useMemo(() => { try { return sheets[sheet] ? sheetTable(sheets[sheet].data) : null; } catch { return null; } }, [sheets, sheet]);
  const preview = useMemo(() => table ? previewImport(kind, table, mapping, ids) : [], [kind, table, mapping, ids]);
  const invalid = preview.filter(row => row.errors.length).length;
  const published = preview.filter(row => row.data?.status === 'published').length;
  const missing = IMPORT_FIELDS[kind].some(field => field.required && Number(mapping[field.key] ?? -1) < 0);
  const ready = preview.length > 0 && !invalid && !missing && (!published || publishConsent);
  useEffect(() => () => { request.current++; }, []);
  useEffect(() => {
    const warn = e => { if (attempted && !result) { e.preventDefault(); e.returnValue = ''; } };
    window.addEventListener('beforeunload', warn);
    return () => window.removeEventListener('beforeunload', warn);
  }, [attempted, result]);
  const configure = (nextSheets, index) => {
    const next = nextSheets[index];
    if (!next) return;
    const parsed = sheetTable(next.data);
    setSheet(index); setMapping(suggestMapping(kind, parsed.headers)); setIds(parsed.rows.map(() => crypto.randomUUID())); setPublishConsent(false); setError('');
  };
  const selectFile = async e => {
    const file = e.target.files?.[0]; e.target.value = '';
    if (!file) return;
    const ticket = ++request.current;
    setBusy(true); setError(''); setResult(null); setSheets([]); setAttempted(false);
    try {
      const next = await readImportFile(file);
      if (ticket !== request.current) return;
      configure(next, 0); setSheets(next); setFilename(file.name);
    } catch (err) { if (ticket === request.current) setError(err?.message || 'This file could not be read.'); }
    finally { if (ticket === request.current) setBusy(false); }
  };
  const save = async () => {
    if (!ready || busy || result) return;
    setBusy(true); setAttempted(true); setError('');
    try {
      const saved = await importRows({ data: { kind, rows: preview.map(row => row.data) } });
      setResult(saved); onImported?.();
    } catch (err) { setError(err?.message || 'Import was not confirmed. Retry this preview safely.'); }
    finally { setBusy(false); }
  };
  const download = () => {
    const url = URL.createObjectURL(new Blob([templateCsv(kind)], { type: 'text/csv;charset=utf-8' }));
    const link = document.createElement('a'); link.href = url; link.download = `finnest-${kind}-template.csv`; link.click(); URL.revokeObjectURL(url);
  };
  const changeOpen = next => {
    if (busy) return;
    if (!next && attempted && !result && !window.confirm('The save is unconfirmed. Closing loses safe retry IDs. Check the saved list before starting another import. Close anyway?')) return;
    request.current++;
    setOpen(next);
    if (next) { setSheets([]); setResult(null); setAttempted(false); setError(''); setFilename(''); setPublishConsent(false); }
  };
  return <>
    <Button variant="outline" className="outline-button" disabled={disabled} onClick={() => changeOpen(true)}><Upload size={14} /> Import CSV / Excel</Button>
    <Dialog open={open} onOpenChange={changeOpen}>
      <DialogContent className="import-dialog" onEscapeKeyDown={e => busy && e.preventDefault()} onInteractOutside={e => busy && e.preventDefault()}>
        <div className="import-heading"><FileSpreadsheet size={22} /><div><DialogTitle>Import {kind === 'guidance' ? 'guidance' : 'experts'}</DialogTitle><DialogDescription>CSV or Excel (.xlsx) · Up to 500 rows · 5 MB</DialogDescription></div></div>
        {result ? <div className="import-result" role="status"><CheckCircle2 size={32} /><h2>{result.count} records saved</h2><p>{result.published} published · {result.count - result.published} drafts</p><p>Published records are available in the website’s {kind === 'guidance' ? 'guidance library' : 'experts directory'}. Drafts remain private.</p><Button className="primary compact" onClick={() => changeOpen(false)}>Done</Button></div> : <>
          <div className="import-tools"><input ref={input} type="file" accept=".csv,.xlsx" hidden onChange={selectFile} aria-label="CSV or Excel file" /><Button variant="outline" className="outline-button" disabled={busy || attempted} onClick={() => input.current?.click()}><Upload size={14} />{filename ? 'Change file' : 'Choose file'}</Button><Button variant="ghost" className="text-link" onClick={download}><Download size={14} /> Download template</Button><span className="import-filename">{filename}</span></div>
          {busy && <p role="status">{attempted ? 'Saving and verifying records…' : 'Reading file…'}</p>}
          {sheets.length > 1 && <label>Worksheet<select value={sheet} disabled={busy || attempted} onChange={e => { try { configure(sheets, Number(e.target.value)); } catch (err) { setSheet(Number(e.target.value)); setError(err.message); } }}>{sheets.map((s, i) => <option key={i} value={i}>{s.name}</option>)}</select></label>}
          {table && <>
            <div className="import-mapping">{IMPORT_FIELDS[kind].map(field => <label key={field.key}>{field.label}{field.required ? ' *' : ''}<select aria-label={`${field.label} column`} value={mapping[field.key] ?? '-1'} disabled={busy || attempted} onChange={e => { setMapping(m => ({ ...m, [field.key]: e.target.value })); setPublishConsent(false); }}><option value="-1">{field.required ? 'Select column' : field.key === 'status' ? 'Default: draft' : field.key === 'category' ? 'Default: general' : 'Leave blank'}</option>{table.headers.map((h, i) => <option key={i} value={i}>{h}</option>)}</select></label>)}</div>
            <div className="import-summary"><strong>{preview.length} rows</strong><span>{invalid} with errors</span><span>{published} to publish</span><span>{preview.length - published} drafts</span></div>
            <div className="table-wrap import-preview"><table className="dtable"><thead><tr><th>Row</th><th>{kind === 'guidance' ? 'Title' : 'Name'}</th><th>{kind === 'guidance' ? 'Category' : 'Speciality'}</th><th>Status</th><th>Validation</th></tr></thead><tbody>{preview.map(row => <tr key={row.line}><td>{row.line}</td><td>{String(row.raw.title ?? row.raw.name ?? '—')}</td><td>{String(row.raw.category ?? row.raw.specialty ?? (kind === 'guidance' ? 'general' : '—'))}</td><td>{String(row.raw.status ?? 'draft')}</td><td>{row.errors.length ? <span className="import-error">{row.errors.join('; ')}</span> : <span className="import-valid">Ready</span>}</td></tr>)}</tbody></table></div>
            {missing && <p className="import-error" role="alert">Match every required column before importing.</p>}
            {published > 0 && <label className="import-consent"><input type="checkbox" checked={publishConsent} disabled={busy || attempted} onChange={e => setPublishConsent(e.target.checked)} /><span>Publish {published} records publicly. I have reviewed their content.</span></label>}
            <div className="import-actions"><p>New records only. Existing entries are not overwritten.</p><Button className="primary compact" disabled={!ready || busy} onClick={save}><Upload size={14} />{busy ? 'Saving…' : attempted ? 'Retry import safely' : `Import ${preview.length} records`}</Button></div>
          </>}
        </>}
        {error && <p className="auth-error" role="alert">{error}</p>}
      </DialogContent>
    </Dialog>
  </>;
}