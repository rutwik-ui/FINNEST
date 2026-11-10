// @vitest-environment jsdom
import React from 'react';
import {afterEach,beforeEach,expect,it,vi} from 'vitest';
import {cleanup,fireEvent,render,screen,within} from '@testing-library/react';
vi.mock('../supabase',()=>({supabase:null}));
import {App} from '../main';
import {DRAFT_KEY} from '../localDraft';
beforeEach(()=>{localStorage.clear();history.replaceState({},'','/');vi.spyOn(window,'scrollTo').mockImplementation(()=>{})});
afterEach(()=>{cleanup();vi.restoreAllMocks()});
it('navigates resale planning and condition and preserves New Property separately after reopening',()=>{
 localStorage.setItem(DRAFT_KEY,JSON.stringify({budget:{loan:'123'},checks:{track:{note:'New draft'}}}));
 const view=render(<App/>);fireEvent.click(screen.getByText('Change journey'));
 fireEvent.click(within(screen.getByRole('dialog')).getByText('Resale Property'));
 expect(screen.getByText('Resale journey')).toBeTruthy();
 fireEvent.click(screen.getByText('Open resale journey'));
 expect(screen.getByText('Seller, ownership & title').closest('button').disabled).toBe(true);
 fireEvent.click(screen.getByText('Start / continue resale checks'));
 fireEvent.change(screen.getByLabelText('Budget (₹)'),{target:{value:'900000'}});
 fireEvent.click(screen.getByRole('button',{name:/Lift/}));
 fireEvent.change(screen.getByLabelText('Building age preference'),{target:{value:'Under 10 years'}});
 fireEvent.click(screen.getByRole('button',{name:/Healthcare/}));
 fireEvent.click(screen.getByText('Continue to condition'));
 const card=screen.getByText('Walls / ceilings / leaks').closest('section');
 fireEvent.change(within(card).getByLabelText('Status for Walls / ceilings / leaks'),{target:{value:'Pending / unknown'}});
 fireEvent.change(within(card).getByLabelText('Note / observation'),{target:{value:'Ask about leaks'}});
 view.unmount();history.replaceState({},'','/resale-condition');const again=render(<App/>);
 expect(screen.getByDisplayValue('Ask about leaks')).toBeTruthy();
 fireEvent.click(screen.getByText('Back to requirements'));
 expect(screen.getByLabelText('Budget (₹)').value).toBe('900000');expect(screen.getByRole('button',{name:/Lift/}).getAttribute('aria-pressed')).toBe('true');
 const saved=JSON.parse(localStorage.getItem(DRAFT_KEY));expect(saved.budget.loan).toBe('123');expect(saved.checks.track.note).toBe('New draft');expect(saved.resaleLocation).toContain('healthcare');
 again.unmount();
});
it('protects resale screen from use with New Property selected',()=>{
 history.replaceState({},'','/resale-condition');render(<App/>);
 expect(screen.getByText('Resale Property screen')).toBeTruthy();expect(screen.queryByText('Walls / ceilings / leaks')).toBeNull();
});
it('does not replace a concurrently changed draft on resale edits',()=>{
 localStorage.setItem(DRAFT_KEY,JSON.stringify({journey:'Resale Property'}));history.replaceState({},'','/resale-requirements');render(<App/>);
 const external=JSON.stringify({journey:'Resale Property',resaleBudget:{budget:'other tab'}});localStorage.setItem(DRAFT_KEY,external);
 fireEvent.change(screen.getByLabelText('Budget (₹)'),{target:{value:'my change'}});
 expect(screen.getByRole('alert').textContent).toMatch(/changed in another tab/);expect(localStorage.getItem(DRAFT_KEY)).toBe(external);
});
