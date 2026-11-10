// @vitest-environment jsdom
import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
const mocks=vi.hoisted(()=>({from:vi.fn(),aal:vi.fn(),signOut:vi.fn()}));
vi.mock('../supabase',()=>({supabase:{from:mocks.from,auth:{mfa:{getAuthenticatorAssuranceLevel:mocks.aal},signOut:mocks.signOut}}}));
vi.mock('../AdminMfa',()=>({default:()=> <div>Authenticator gate</div>}));
import Admin from '../Admin';
const session={access_token:'token1',user:{id:'user1',email:'editor@example.com'}};
const item={id:'item1',title:'Existing',body:'Original',category:'general',status:'draft',updated_at:'2026-01-01T00:00:00Z'};
function setup({roleResults=[{data:{role:'content_manager'},error:null}],listResults=[{data:[item],error:null}]}={}) {
 let r=0, l=0;
 mocks.aal.mockResolvedValue({data:{currentLevel:'aal2'},error:null});
 mocks.from.mockImplementation(table=>{
  if(table==='finnest_admin_roles')return {select:()=>({eq:()=>({maybeSingle:async()=>roleResults[Math.min(r++,roleResults.length-1)]})})};
  return {select:()=>({order:()=>({limit:async()=>table==='finnest_guidance'?listResults[Math.min(l++,listResults.length-1)]:{data:[],error:null}})})};
 });
}
describe('admin reliability',()=>{
 beforeEach(()=>vi.resetAllMocks());afterEach(()=>{cleanup();vi.restoreAllMocks()});
 it('offers a retry when role lookup fails',async()=>{
  setup({roleResults:[{data:null,error:new Error('offline')},{data:{role:'content_manager'},error:null}]});
  render(<Admin session={session} authLoading={false} go={()=>{}}/>);
  fireEvent.click(await screen.findByText('Retry access check'));
  expect(await screen.findByText('New guidance')).toBeTruthy();
 });
 it('does not report an empty list after a failed load and can retry',async()=>{
  setup({listResults:[{data:null,error:new Error('offline')},{data:[item],error:null}]});
  render(<Admin session={session} authLoading={false} go={()=>{}}/>);
  expect(await screen.findByText(/Could not load guidance and activity/)).toBeTruthy();
  expect(screen.queryByText('No guidance added yet.')).toBeNull();
  fireEvent.click(screen.getByText('Refresh'));
  expect(await screen.findByText('Existing')).toBeTruthy();
 });
 it('protects unsaved work when switching items, cancelling, leaving or signing out',async()=>{
  setup();const go=vi.fn();const confirm=vi.spyOn(window,'confirm').mockReturnValue(false);
  render(<Admin session={session} authLoading={false} go={go}/>);
  fireEvent.click(await screen.findByText('Edit'));
  fireEvent.change(screen.getByLabelText('Title'),{target:{value:'Unsaved edit'}});
  fireEvent.click(screen.getByText('Cancel edit'));
  fireEvent.click(screen.getByText('Edit'));
  fireEvent.click(screen.getByText('Return to FINNEST'));
  fireEvent.click(screen.getByText('Sign out'));
  expect(confirm).toHaveBeenCalledTimes(4);
  expect(screen.getByLabelText('Title').value).toBe('Unsaved edit');
  expect(go).not.toHaveBeenCalled();expect(mocks.signOut).not.toHaveBeenCalled();
  confirm.mockReturnValue(true);fireEvent.click(screen.getByText('Cancel edit'));
  expect(screen.getByLabelText('Title').value).toBe('');
 });
 it('ignores a delayed list response from a previous account',async()=>{
  let resolveOld; const old=new Promise(resolve=>{resolveOld=resolve});
  mocks.aal.mockResolvedValue({data:{currentLevel:'aal2'},error:null});
  mocks.from.mockImplementation(table=>{
   if(table==='finnest_admin_roles')return {select:()=>({eq:(_key,id)=>({maybeSingle:async()=>({data:id==='user1'?{role:'content_manager'}:null,error:null})})})};
   return {select:()=>({order:()=>({limit:()=>table==='finnest_guidance'?old:Promise.resolve({data:[],error:null})})})};
  });
  const view=render(<Admin session={session} authLoading={false} go={()=>{}}/>);
  await screen.findByText('Loading guidance and activity…');
  view.rerender(<Admin session={{access_token:'token2',user:{id:'user2',email:'other@example.com'}}} authLoading={false} go={()=>{}}/>);
  expect(await screen.findByText(/no administrator role/i)).toBeTruthy();
  resolveOld({data:[item],error:null});
  await waitFor(()=>expect(screen.queryByText('Existing')).toBeNull());
 });
 it('requires explicit confirmation before publishing and retains edits on a concurrent update',async()=>{
  const update=vi.fn(()=>({eq:()=>({eq:()=>({select:async()=>({data:[],error:null})})})}));
  mocks.aal.mockResolvedValue({data:{currentLevel:'aal2'},error:null});
  mocks.from.mockImplementation(table=>{
   if(table==='finnest_admin_roles')return {select:()=>({eq:()=>({maybeSingle:async()=>({data:{role:'content_manager'},error:null})})})};
   if(table==='finnest_guidance')return {select:()=>({order:()=>({limit:async()=>({data:[item],error:null})})}),update};
   return {select:()=>({order:()=>({limit:async()=>({data:[],error:null})})})};
  });
  const confirm=vi.spyOn(window,'confirm').mockReturnValue(false);
  render(<Admin session={session} authLoading={false} go={()=>{}}/>);
  fireEvent.click(await screen.findByText('Edit'));
  fireEvent.change(screen.getByLabelText('Status'),{target:{value:'published'}});
  fireEvent.click(screen.getByText('Save guidance'));
  expect(update).not.toHaveBeenCalled();
  confirm.mockReturnValue(true);fireEvent.click(screen.getByText('Save guidance'));
  expect(await screen.findByText(/This item changed or access was removed/)).toBeTruthy();
  expect(screen.getByLabelText('Status').value).toBe('published');
  expect(update).toHaveBeenCalledTimes(1);
 });

});
