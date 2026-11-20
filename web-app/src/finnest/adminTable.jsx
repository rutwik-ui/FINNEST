import React, { useMemo, useState } from 'react';
import { ArrowDown, ArrowUp, ArrowUpDown, Download } from 'lucide-react';

/** Search + sort for an admin table. `columns` is [[key, label, getter?], ...]. */
export function useTableView(rows, columns, { initialSort = null, searchKeys } = {}) {
  const [query, setQuery] = useState('');
  const [sort, setSort] = useState(initialSort); // { key, dir: 1 | -1 }
  const value = (row, key) => { const col = columns.find(c => c[0] === key); return col?.[2] ? col[2](row) : row?.[key]; };
  const view = useMemo(() => {
    const list = rows || [];
    const terms = query.trim().toLowerCase().split(/\s+/).filter(Boolean);
    const keys = searchKeys ?? columns.map(c => c[0]);
    const filtered = terms.length ? list.filter(r => { const text = keys.map(k => String(value(r, k) ?? '')).join(' ').toLowerCase(); return terms.every(t => text.includes(t)); }) : list;
    if (!sort) return filtered;
    return [...filtered].sort((a, b) => {
      const x = value(a, sort.key) ?? '', y = value(b, sort.key) ?? '';
      return (typeof x === 'number' && typeof y === 'number' ? x - y : String(x).localeCompare(String(y), 'en', { numeric: true, sensitivity: 'base' })) * sort.dir;
    });
  }, [rows, query, sort, columns]);
  const toggle = key => setSort(s => s?.key === key ? (s.dir === 1 ? { key, dir: -1 } : null) : { key, dir: 1 });
  return { view, query, setQuery, sort, toggle, value };
}

export function SortTh({ label, k, table }) {
  const active = table.sort?.key === k;
  const Icon = !active ? ArrowUpDown : table.sort.dir === 1 ? ArrowUp : ArrowDown;
  return <th aria-sort={active ? (table.sort.dir === 1 ? 'ascending' : 'descending') : 'none'}>
    <button type="button" className={'th-sort' + (active ? ' on' : '')} onClick={() => table.toggle(k)}>{label}<Icon size={12} aria-hidden="true"/></button></th>;
}

/** Downloads the rows currently shown (after search and sort) as a CSV that Excel opens correctly. */
export function exportCsv(filename, columns, rows, value) {
  const cell = v => { const s = v == null ? '' : String(v); const safe = /^[=+\-@\t\r]/.test(s) ? `'${s}` : s; return /[",\n\r]/.test(safe) ? `"${safe.replace(/"/g, '""')}"` : safe; };
  const lines = [columns.map(c => cell(c[1])).join(','), ...rows.map(r => columns.map(c => cell(value(r, c[0]))).join(','))];
  const blob = new Blob(['﻿' + lines.join('\r\n')], { type: 'text/csv;charset=utf-8' });
  const url = URL.createObjectURL(blob); const a = document.createElement('a');
  a.href = url; a.download = `${filename}-${new Date().toISOString().slice(0, 10)}.csv`; document.body.appendChild(a); a.click(); a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}

export function TableTools({ table, placeholder, onExport, count, total }) {
  return <div className="table-tools">
    <input className="tool-input" type="search" placeholder={placeholder} aria-label={placeholder} value={table.query} onChange={e => table.setQuery(e.target.value)}/>
    <span className="table-count">{count === total ? `${total} rows` : `${count} of ${total}`}</span>
    <button type="button" className="outline-button" onClick={onExport} disabled={!count}><Download size={14}/> Export CSV</button>
  </div>;
}
