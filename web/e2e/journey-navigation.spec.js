import {test,expect} from '@playwright/test';

test('New Property journey returns to dashboard and retains entries',async({page})=>{
 test.setTimeout(60000);await page.goto('/');
 await page.getByRole('button',{name:'Take this step'}).click();
 await page.getByLabel('Approximate loan amount, if known').fill('1000000');
 await page.getByRole('button',{name:'Continue to developer checks'}).click();
 await expect(page.getByRole('heading',{name:'Questions worth asking'})).toBeVisible();
 await page.getByRole('button',{name:'Continue to shortlist'}).click();
 await page.getByRole('button',{name:'Continue to visit notes'}).click();
 await page.getByRole('button',{name:'Continue to resident feedback'}).click();
 await page.getByLabel('Topic',{exact:true}).fill('Water');
 await page.getByLabel('What was shared?').fill('Ask about supply');
 await page.getByRole('button',{name:'Add feedback note'}).click();
 await page.getByRole('button',{name:'Continue to loan guide'}).click();
 await page.getByRole('button',{name:'Continue to legal review'}).click();
 await page.getByRole('button',{name:'Continue to financial review'}).click();
 await page.getByRole('button',{name:'Continue to final review'}).click();
 await page.getByRole('button',{name:'View journey summary'}).click();
 await page.getByRole('button',{name:'Back to dashboard'}).click();
 await expect(page).toHaveURL('/');await expect(page.getByRole('heading',{name:'Your home-buying journey.'})).toBeVisible();
 await page.reload();await page.getByRole('button',{name:'Take this step'}).click();
 await expect(page.getByLabel('Approximate loan amount, if known')).toHaveValue('1000000');
 const draft=await page.evaluate(()=>JSON.parse(localStorage.getItem('finnest-web-v1')));
 expect(draft.feedbackNotes[0].shared).toBe('Ask about supply');
});

test('switching journeys keeps local notes separate and returns home',async({page})=>{
 await page.goto('/');await page.getByRole('button',{name:'Take this step'}).click();
 await page.getByLabel('Approximate loan amount, if known').fill('1000000');
 await page.getByRole('button',{name:'FINNEST home'}).click();
 await page.getByRole('button',{name:'Change journey'}).click();
 await page.getByRole('dialog').getByRole('button',{name:/Resale Property/}).click();
 await expect(page).toHaveURL('/');await page.getByRole('button',{name:'Open resale journey'}).click();
 await page.getByRole('button',{name:'Start / continue resale checks'}).click();
 await page.getByLabel('Budget (₹)').fill('900000');
 await page.getByRole('button',{name:'Continue to condition'}).click();
 await page.getByRole('button',{name:'FINNEST home'}).click();
 await expect(page.getByRole('heading',{name:'Your home-buying journey.'})).toBeVisible();
 await page.reload();
 const draft=await page.evaluate(()=>JSON.parse(localStorage.getItem('finnest-web-v1')));
 expect(draft.journey).toBe('Resale Property');expect(draft.budget.loan).toBe('1000000');expect(draft.resaleBudget.budget).toBe('900000');
});
