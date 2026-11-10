// @vitest-environment jsdom
import React from 'react';
import {afterEach,beforeEach,expect,it,vi} from 'vitest';
import {cleanup,fireEvent,render,screen,within} from '@testing-library/react';
vi.mock('../supabase',()=>({supabase:null}));
import {App} from '../main';
import {DRAFT_KEY} from '../localDraft';
beforeEach(()=>{localStorage.clear();history.replaceState({},'','/visit');vi.spyOn(window,'scrollTo').mockImplementation(()=>{})});
afterEach(()=>{cleanup();vi.restoreAllMocks()});
it('saves unfinished feedback, adds and deletes only after confirmation, and restores on reopen',()=>{
 const view=render(<App/>);fireEvent.click(screen.getByText('Continue to resident feedback'));
 expect(screen.getByText('Add feedback note').disabled).toBe(true);
 fireEvent.change(screen.getByLabelText('Topic'),{target:{value:'Water'}});
 fireEvent.change(screen.getByLabelText('What was shared?'),{target:{value:'Pressure varies'}});
 view.unmount();history.replaceState({},'','/feedback');const reopened=render(<App/>);
 expect(screen.getByLabelText('Topic').value).toBe('Water');expect(screen.getByLabelText('What was shared?').value).toBe('Pressure varies');
 fireEvent.click(screen.getByText('Add feedback note'));expect(screen.getByText('Pressure varies')).toBeTruthy();
 const confirm=vi.spyOn(window,'confirm').mockReturnValue(false);fireEvent.click(screen.getByText('Delete note'));expect(screen.getByText('Pressure varies')).toBeTruthy();
 confirm.mockReturnValue(true);fireEvent.click(screen.getByText('Delete note'));expect(screen.queryByText('Pressure varies')).toBeNull();
 expect(JSON.parse(localStorage.getItem(DRAFT_KEY)).feedbackNotes).toEqual([]);reopened.unmount();
});
it('calculates and restores loan assumptions and lender notes, then saves legal entries',()=>{
 history.replaceState({},'','/loan');const view=render(<App/>);
 fireEvent.change(screen.getByLabelText('Loan amount (₹)'),{target:{value:'1000000'}});
 fireEvent.change(screen.getByLabelText('Annual interest rate (%)'),{target:{value:'8'}});
 fireEvent.change(screen.getByLabelText('Tenure (years)'),{target:{value:'20'}});
 expect(screen.getByText('₹8,364.40 / month')).toBeTruthy();
 fireEvent.change(screen.getByLabelText('Note / source for Identity and address requirements'),{target:{value:'Ask bank'}});
 fireEvent.click(screen.getByText('Continue to legal review'));
 const card=screen.getByText('Ownership & title chain').closest('section');
 fireEvent.change(within(card).getByLabelText('Status for Ownership & title chain'),{target:{value:'Pending / unknown'}});
 fireEvent.change(within(card).getByLabelText('Note / observation'),{target:{value:'Discuss with lawyer'}});
 view.unmount();history.replaceState({},'','/legal');const reopened=render(<App/>);
 const restored=screen.getByText('Ownership & title chain').closest('section');
 expect(within(restored).getByLabelText('Note / observation').value).toBe('Discuss with lawyer');
 expect(within(restored).getByLabelText('Status for Ownership & title chain').value).toBe('Pending / unknown');
 fireEvent.click(screen.getByText('Back'));
 expect(screen.getByText('₹8,364.40 / month')).toBeTruthy();expect(screen.getByLabelText('Note / source for Identity and address requirements').value).toBe('Ask bank');reopened.unmount();
});
it('does not display New Property forms while Resale is selected',()=>{
 localStorage.setItem(DRAFT_KEY,JSON.stringify({journey:'Resale Property'}));history.replaceState({},'','/legal');render(<App/>);
 expect(screen.getByText('New Property screen')).toBeTruthy();expect(screen.queryByText('Ownership & title chain')).toBeNull();
});
