import Papa from 'papaparse';
import { checkImportFile, type ImportSheet } from './bulkImport';

export async function readImportFile(file: File): Promise<ImportSheet[]> {
  checkImportFile(file);
  if (/\.csv$/i.test(file.name)) {
    const result = Papa.parse<unknown[]>(await file.text(), { skipEmptyLines: 'greedy' });
    if (result.errors.length) throw new Error(`CSV could not be read: ${result.errors[0]?.message ?? 'Invalid CSV'}`);
    return [{ name: 'CSV', data: result.data }];
  }
  // Load the pure-JavaScript reader only when an Excel file is selected, never at SSR import time.
  const { default: readExcelFile } = await import('read-excel-file/universal');
  const sheets = await readExcelFile(file);
  return sheets.map(sheet => ({ name: sheet.sheet, data: sheet.data }));
}