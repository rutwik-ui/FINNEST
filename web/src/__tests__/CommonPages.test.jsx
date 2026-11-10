// @vitest-environment jsdom
import React from 'react';
import {afterEach,beforeEach,expect,it,vi} from 'vitest';
import {cleanup,fireEvent,render,screen,within} from '@testing-library/react';
vi.mock('../supabase',()=>({supabase:null}));
import {App} from '../main';
import {DRAFT_KEY} from '../localDraft';
beforeEach(()=>{localStorage.clear();history.replaceState({},'','/profile');vi.spyOn(window,'scrollTo').mockImplementation(()=>{})});
afterEach(()=>{cleanup();vi.restoreAllMocks()});
it('saves local profile and reminders, preserves unfinished reminder and confirms deletion',()=>{
 const view=render(<App/>);fireEvent.change(screen.getByLabelText('Name'),{target:{value:'A buyer'}});fireEvent.change(screen.getByLabelText('City / area'),{target:{value:'Mumbai'}});
 fireEvent.click(screen.getByRole('button',{name:'Reminders'}));
 expect(screen.getByText('Add reminder').disabled).toBe(true);
 fireEvent.change(screen.getByLabelText('Reminder'),{target:{value:'Ask lawyer'}});fireEvent.change(screen.getByLabelText('Date'),{target:{value:'2027-02-28'}});fireEvent.change(screen.getByLabelText('Note (optional)'),{target:{value:'Bring papers'}});
 view.unmount();history.replaceState({},'','/reminders');const again=render(<App/>);
 expect(screen.getByLabelText('Reminder').value).toBe('Ask lawyer');fireEvent.click(screen.getByText('Add reminder'));
 expect(screen.getByText('Ask lawyer')).toBeTruthy();expect(screen.getByText('2027-02-28')).toBeTruthy();
 const confirm=vi.spyOn(window,'confirm').mockReturnValue(false);fireEvent.click(screen.getByText('Delete reminder'));expect(screen.getByText('Ask lawyer')).toBeTruthy();
 confirm.mockReturnValue(true);fireEvent.click(screen.getByText('Delete reminder'));expect(screen.queryByText('Ask lawyer')).toBeNull();
 fireEvent.click(screen.getByText('Back to profile'));expect(screen.getByLabelText('Name').value).toBe('A buyer');expect(JSON.parse(localStorage.getItem(DRAFT_KEY)).localProfile.city).toBe('Mumbai');again.unmount();
});
it('accurately explains network and notification limitations and deletion removes new local data',()=>{
 localStorage.setItem(DRAFT_KEY,JSON.stringify({localProfile:{name:'Me'},reminders:[{id:'one',title:'Meeting',date:'2026-01-01',note:''}]}));
 render(<App/>);fireEvent.click(screen.getByText('Privacy & data use'));
 expect(screen.getByText(/Website drafts are not uploaded/)).toBeTruthy();
 expect(screen.getByText(/No expert contact, marketing subscription/)).toBeTruthy();
 fireEvent.click(screen.getByText('Go to profile & deletion'));
 fireEvent.click(screen.getByText('Delete local browser data'));fireEvent.change(screen.getByLabelText('Type DELETE to confirm'),{target:{value:'DELETE'}});fireEvent.click(screen.getByText('Delete browser data'));
 expect(localStorage.getItem(DRAFT_KEY)).toBeNull();expect(screen.getByLabelText('Name').value).toBe('');
});
it('illustrative concern does not alter recorded status',()=>{
 const draft=JSON.stringify({checks:{track:{status:'Checked by you'}}});localStorage.setItem(DRAFT_KEY,draft);
 render(<App/>);fireEvent.click(screen.getByText('Understand a concern'));
 expect(screen.getByText(/Illustrative pattern only/)).toBeTruthy();
 expect(localStorage.getItem(DRAFT_KEY)).toBe(draft);
});
