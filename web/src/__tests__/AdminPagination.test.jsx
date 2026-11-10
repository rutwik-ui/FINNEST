// @vitest-environment jsdom
import React from 'react';
import {afterEach, beforeEach, expect, it, vi} from 'vitest';
import {cleanup, fireEvent, render, screen, waitFor} from '@testing-library/react';
const mock=vi.hoisted(()=>({from:vi.fn(),aal:vi.fn()}));
vi.mock('../supabase',()=>({supabase:{from:mock.from,auth:{mfa:{getAuthenticatorAssuranceLevel:mock.aal},signOut:vi.fn()}}}));
vi.mock('../AdminMfa',()=>({default:()=>null}));
import Admin from '../Admin';
const session={access_token:'token',user:{id:'user',email:'editor@example.com'}};
const row=n=>({id:`00000000-0000-4000-8000-${String(n).padStart(12,'0')}`,title:`Guidance ${n}`,body:'body',category:'general',status:'draft',updated_at:'2026-01-01T00:00:00Z'});
let requests;
beforeEach(()=>{
 vi.resetAllMocks();requests=[];mock.aal.mockResolvedValue({data:{currentLevel:'aal2'},error:null});
 mock.from.mockImplementation(table=>{
  if(table==='finnest_admin_roles')return {select:()=>({eq:()=>({maybeSingle:async()=>({data:{role:'content_manager'},error:null})})})};
  const filters=[];const q={eq:(...x)=>(filters.push(x),q),or:x=>(filters.push(['or',x]),q),order:()=>q,limit:()=>{
   if(table==='finnest_admin_audit')return Promise.resolve({data:[],error:null});
   return new Promise(resolve=>requests.push({filters,resolve}));
  }};return {select:()=>q};
 });
});
afterEach(()=>cleanup());
it('loads subsequent pages, retries failures, and resets on filter change without losing the editor',async()=>{
 render(<Admin session={session} authLoading={false} go={()=>{}}/>);
 await waitFor(()=>expect(requests.length).toBe(1));
 requests[0].resolve({data:Array.from({length:51},(_,i)=>row(51-i)),error:null});
 fireEvent.click(await screen.findByText('Load more guidance'));
 await waitFor(()=>expect(requests.length).toBe(2));
 requests[1].resolve({data:null,error:new Error('offline')});
 fireEvent.click(await screen.findByText('Retry loading more'));
 await waitFor(()=>expect(requests.length).toBe(3));
 requests[2].resolve({data:[row(1)],error:null});
 expect(await screen.findByText('Guidance 1')).toBeTruthy();
 fireEvent.change(screen.getByLabelText('Title'),{target:{value:'Unfinished draft'}});
 fireEvent.change(screen.getByLabelText('Filter status'),{target:{value:'published'}});
 await waitFor(()=>expect(requests.length).toBe(4));
 expect(requests[3].filters).toContainEqual(['status','published']);
 requests[3].resolve({data:[],error:null});
 expect(await screen.findByText('No guidance matches these filters.')).toBeTruthy();
 expect(screen.queryByText('Guidance 1')).toBeNull();
 expect(screen.getByLabelText('Title').value).toBe('Unfinished draft');
});
it('ignores an older filter request when a newer filter completes',async()=>{
 render(<Admin session={session} authLoading={false} go={()=>{}}/>);
 await waitFor(()=>expect(requests.length).toBe(1));
 fireEvent.change(screen.getByLabelText('Filter category'),{target:{value:'resale_property'}});
 await waitFor(()=>expect(requests.length).toBe(2));
 requests[1].resolve({data:[],error:null});
 expect(await screen.findByText('No guidance matches these filters.')).toBeTruthy();
 requests[0].resolve({data:[row(1)],error:null});
 await waitFor(()=>expect(screen.queryByText('Guidance 1')).toBeNull());
});
