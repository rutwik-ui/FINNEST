// @vitest-environment jsdom
import React from 'react';
import {afterEach,beforeEach,expect,it,vi} from 'vitest';
import {cleanup,fireEvent,render,screen} from '@testing-library/react';
vi.mock('../supabase',()=>({supabase:null}));
import {App} from '../main';
import {DRAFT_KEY} from '../localDraft';
beforeEach(()=>{localStorage.clear();history.replaceState({},'', '/profile');vi.spyOn(window,'scrollTo').mockImplementation(()=>{})});
afterEach(()=>{cleanup();vi.restoreAllMocks()});
it('requires typed confirmation and deletes only the FINNEST local draft',()=>{
 const original=JSON.stringify({journey:'New Property',budget:{loan:'900'},projects:[]});
 localStorage.setItem(DRAFT_KEY,original);localStorage.setItem('unrelated','keep');
 render(<App/>);
 fireEvent.click(screen.getByText('Delete local browser data'));
 fireEvent.change(screen.getByLabelText('Type DELETE to confirm'),{target:{value:'delete'}});
 expect(screen.getByText('Delete browser data').disabled).toBe(true);
 fireEvent.click(screen.getByText('Cancel'));expect(localStorage.getItem(DRAFT_KEY)).toBe(original);
 fireEvent.click(screen.getByText('Delete local browser data'));
 fireEvent.change(screen.getByLabelText('Type DELETE to confirm'),{target:{value:'DELETE'}});
 fireEvent.click(screen.getByText('Delete browser data'));
 expect(screen.getByRole('status').textContent).toMatch(/deleted/);expect(localStorage.getItem(DRAFT_KEY)).toBeNull();expect(localStorage.getItem('unrelated')).toBe('keep');
});
it('keeps an unreadable draft intact until explicitly deleted',()=>{
 localStorage.setItem(DRAFT_KEY,'{invalid');render(<App/>);
 expect(screen.getByRole('alert').textContent).toMatch(/has not been overwritten/);
 expect(localStorage.getItem(DRAFT_KEY)).toBe('{invalid');
 fireEvent.click(screen.getByText('Delete local browser data'));
 fireEvent.change(screen.getByLabelText('Type DELETE to confirm'),{target:{value:'DELETE'}});
 fireEvent.click(screen.getByText('Delete browser data'));
 expect(localStorage.getItem(DRAFT_KEY)).toBeNull();expect(screen.queryByRole('alert')).toBeNull();
});
it('blocks deletion when another tab changed data',()=>{
 localStorage.setItem(DRAFT_KEY,'{}');render(<App/>);
 localStorage.setItem(DRAFT_KEY,'{"budget":{"loan":"200"}}');
 fireEvent.click(screen.getByText('Delete local browser data'));
 fireEvent.change(screen.getByLabelText('Type DELETE to confirm'),{target:{value:'DELETE'}});
 fireEvent.click(screen.getByText('Delete browser data'));
 expect(screen.getByRole('alert').textContent).toMatch(/changed in another tab/);
 expect(localStorage.getItem(DRAFT_KEY)).toContain('200');
});
it('persists edits and reopens them, with roadmap links to implemented sections only',()=>{
 history.replaceState({},'', '/');const view=render(<App/>);
 fireEvent.click(screen.getByRole('button',{name:/Developer checks/}));
 expect(screen.getByText('Questions worth asking')).toBeTruthy();
 fireEvent.change(screen.getAllByLabelText('Note / observation')[0],{target:{value:'Ask for delivery records'}});
 expect(JSON.parse(localStorage.getItem(DRAFT_KEY)).checks.track.note).toBe('Ask for delivery records');
 expect(screen.getByRole('button',{name:/Financial due diligence/}).disabled).toBe(false);
 fireEvent.click(screen.getByRole('button',{name:/Requirements & budget/}));
 expect(screen.getByRole('heading',{name:'Requirements & Budget'})).toBeTruthy();
 view.unmount();history.replaceState({},'','/');render(<App/>);
 fireEvent.click(screen.getByRole('button',{name:/Developer checks/}));
 expect(screen.getAllByLabelText('Note / observation')[0].value).toBe('Ask for delivery records');
});
it('downloads only the current saved raw draft, including unreadable drafts',()=>{
 const raw='{invalid';localStorage.setItem(DRAFT_KEY,raw);
 const create=vi.fn(()=> 'blob:test'),revoke=vi.fn();vi.stubGlobal('URL',Object.assign(URL,{createObjectURL:create,revokeObjectURL:revoke}));
 const click=vi.spyOn(HTMLAnchorElement.prototype,'click').mockImplementation(()=>{});
 render(<App/>);fireEvent.click(screen.getByText('Download saved browser draft'));
 expect(create).toHaveBeenCalledOnce();expect(create.mock.calls[0][0]).toBeInstanceOf(Blob);
 expect(click).toHaveBeenCalledOnce();expect(revoke).toHaveBeenCalledWith('blob:test');
 expect(screen.getByRole('status').textContent).toMatch(/Download requested/);expect(localStorage.getItem(DRAFT_KEY)).toBe(raw);
 vi.unstubAllGlobals();
});
it('does not claim a download when no saved draft exists or storage is unavailable',()=>{
 render(<App/>);fireEvent.click(screen.getByText('Download saved browser draft'));
 expect(screen.getByRole('status').textContent).toMatch(/No saved/);
 vi.spyOn(Storage.prototype,'getItem').mockImplementation(()=>{throw new Error('blocked')});
 fireEvent.click(screen.getByText('Download saved browser draft'));
 expect(screen.getByRole('status').textContent).toMatch(/Could not request/);
});
