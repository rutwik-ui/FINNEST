// @vitest-environment jsdom
import React from 'react';
import {afterEach,beforeEach,expect,it,vi} from 'vitest';
import {cleanup,fireEvent,render,screen} from '@testing-library/react';
const mock=vi.hoisted(()=>({from:vi.fn(),aal:vi.fn(),onAuthStateChange:vi.fn()}));
vi.mock('../supabase',()=>({supabase:{from:mock.from,auth:{mfa:{getAuthenticatorAssuranceLevel:mock.aal},onAuthStateChange:mock.onAuthStateChange,signOut:vi.fn()}}}));
vi.mock('../AdminMfa',()=>({default:()=>null}));
import {App} from '../main';
const session={access_token:'token',user:{id:'editor',email:'editor@example.com'}};
beforeEach(()=>{
 vi.resetAllMocks();localStorage.clear();history.replaceState({},'','/Adminlogin');vi.spyOn(window,'scrollTo').mockImplementation(()=>{});
 mock.aal.mockResolvedValue({data:{currentLevel:'aal2'},error:null});
 mock.onAuthStateChange.mockImplementation(cb=>{queueMicrotask(()=>cb('INITIAL_SESSION',session));return {data:{subscription:{unsubscribe:vi.fn()}}}});
 mock.from.mockImplementation(table=>{if(table==='finnest_admin_roles')return {select:()=>({eq:()=>({maybeSingle:async()=>({data:{role:'content_manager'},error:null})})})};const q={order:()=>q,eq:()=>q,or:()=>q,limit:async()=>({data:[],error:null})};return {select:()=>q};});
});
afterEach(()=>{cleanup();vi.restoreAllMocks()});
it('guards unsaved admin changes from header navigation',async()=>{
 const confirm=vi.spyOn(window,'confirm').mockReturnValue(false);render(<App/>);
 fireEvent.change(await screen.findByLabelText('Title'),{target:{value:'Unsaved'}});
 fireEvent.click(screen.getByLabelText('FINNEST home'));expect(confirm).toHaveBeenCalledTimes(1);expect(screen.getByLabelText('Title').value).toBe('Unsaved');
 fireEvent.click(screen.getByLabelText('Open menu'));fireEvent.click(screen.getByRole('button',{name:'Guidance'}));expect(confirm).toHaveBeenCalledTimes(2);expect(screen.getByLabelText('Title').value).toBe('Unsaved');
 confirm.mockReturnValue(true);fireEvent.click(screen.getByLabelText('FINNEST home'));expect(screen.getByRole('heading',{name:/Your home-buying journey/})).toBeTruthy();
});
it('restores the admin URL and editing screen when in-app browser history navigation is cancelled',async()=>{
 const confirm=vi.spyOn(window,'confirm').mockReturnValue(false);render(<App/>);
 fireEvent.change(await screen.findByLabelText('Title'),{target:{value:'Unsaved'}});
 history.replaceState({},'','/guidance');fireEvent.popState(window);
 expect(confirm).toHaveBeenCalledTimes(1);expect(location.pathname).toBe('/Adminlogin');expect(screen.getByLabelText('Title').value).toBe('Unsaved');
});
