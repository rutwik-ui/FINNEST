import {test,expect} from '@playwright/test';
import {readFile} from 'node:fs/promises';
const key='finnest-web-v1';

test('download, replace, and restore saved browser data',async({page})=>{
 await page.goto('/profile');
 await page.getByLabel('Name',{exact:true}).fill('Recovery Buyer');
 await page.getByLabel('City / area').fill('Mumbai');
 const [download]=await Promise.all([page.waitForEvent('download'),page.getByRole('button',{name:'Download saved browser draft'}).click()]);
 expect(download.suggestedFilename()).toBe('finnest-browser-draft.json');
 const bytes=await readFile(await download.path());
 const draft=JSON.parse(bytes.toString());expect(draft.localProfile).toMatchObject({name:'Recovery Buyer',city:'Mumbai'});
 await page.getByLabel('Name',{exact:true}).fill('Changed after download');
 await page.getByRole('button',{name:'Restore browser draft'}).click();
 await page.getByLabel('Choose a FINNEST browser draft file').setInputFiles({name:'finnest-browser-draft.json',mimeType:'application/json',buffer:bytes});
 await expect(page.getByText('File is structurally compatible.',{exact:false})).toBeVisible();
 await expect(page.getByRole('button',{name:'Replace browser draft'})).toBeDisabled();
 await page.getByLabel('Type RESTORE to confirm').fill('RESTORE');
 await page.getByRole('button',{name:'Replace browser draft'}).click();
 await expect(page.getByText('Browser draft restored from the selected file.')).toBeVisible();
 await page.reload();await expect(page.getByLabel('Name',{exact:true})).toHaveValue('Recovery Buyer');
 expect(await page.evaluate(k=>JSON.parse(localStorage.getItem(k)).localProfile.city,key)).toBe('Mumbai');
});

test('another tab changing the draft prevents restore',async({page,context})=>{
 await page.goto('/profile');await page.getByLabel('Name',{exact:true}).fill('Initial');
 const other=await context.newPage();await other.goto('/profile');
 await other.getByRole('button',{name:'Restore browser draft'}).click();
 await other.getByLabel('Choose a FINNEST browser draft file').setInputFiles({name:'draft.json',mimeType:'application/json',buffer:Buffer.from('{"localProfile":{"name":"Imported"}}')});
 await expect(other.getByText('File is structurally compatible.',{exact:false})).toBeVisible();
 await other.getByLabel('Type RESTORE to confirm').fill('RESTORE');
 await page.getByLabel('Name',{exact:true}).fill('Newer');
 await expect(other.getByRole('alert')).toContainText('changed in another tab');
 await other.getByRole('button',{name:'Replace browser draft'}).click();
 await expect(other.getByText('Could not restore data.',{exact:false})).toBeVisible();
 expect(await page.evaluate(k=>JSON.parse(localStorage.getItem(k)).localProfile.name,key)).toBe('Newer');
});
