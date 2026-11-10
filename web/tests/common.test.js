import test from 'node:test';
import assert from 'node:assert/strict';
import {validReminderDate} from '../src/common.js';
test('validates real calendar days and supported years',()=>{
 for(const date of ['1900-01-01','2000-02-29','2024-02-29','2100-12-31'])assert.equal(validReminderDate(date),true,date);
 for(const date of ['1899-12-31','2101-01-01','1900-02-29','2023-02-29','2024-04-31','2024-13-01','2024-1-01',''])assert.equal(validReminderDate(date),false,date);
});
