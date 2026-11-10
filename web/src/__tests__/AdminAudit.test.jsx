// @vitest-environment jsdom
import React from 'react';
import {afterEach,beforeEach,expect,it,vi} from 'vitest';
import {cleanup,fireEvent,render,screen,waitFor} from '@testing-library/react';
const mock=vi.hoisted(()=>({from:vi.fn(),aal:vi.fn()}));
vi.mock('../supabase',()=>({supabase:{from:mock.from,auth:{mfa:{getAuthenticatorAssuranceLevel:mock.aal},signOut:vi.fn()}}}));
vi.mock('../AdminMfa',()=>({default:()=>null}));
import Admin from '../Admin';
const session={access_token:'token',user:{id:'user',email:'editor@example.com'}};
const row=n=>({id:n,actor_id:'actor',item_id:`item-${n}`,action:'INSERT',occurred_at:'2026-01-01T00:00:00Z'});
let audits,guides;
beforeEach(()=>{vi.resetAllMocks();audits=[];guides=[];mock.aal.mockResolvedValue({data:{currentLevel:'aal2'},error:null});mock.from.mockImplementation(table=>{
 if(table==='finnest_admin_roles')return {select:()=>({eq:()=>({maybeSingle:async()=>({data:{role:'content_manager'},error:null})})})};
 const q={eq:()=>q,or:()=>q,lt:(...args)=>(q.cursor=args[1],q),order:()=>q,limit:()=>new Promise(resolve=>(table==='finnest_admin_audit'?audits:guides).push({resolve,cursor:q.cursor}))};return {select:()=>q};
})});
afterEach(()=>cleanup());
it('keeps guidance available if activity fails and recovers on refresh',async()=>{
 render(<Admin session={session} authLoading={false} go={()=>{}}/>);await waitFor(()=>expect(audits.length).toBe(1));
 guides[0].resolve({data:[],error:null});audits[0].resolve({data:null,error:new Error('offline')});
 expect(await screen.findByText('No guidance matches these filters.')).toBeTruthy();expect(await screen.findByText(/Could not load activity/)).toBeTruthy();
 fireEvent.click(screen.getByText('Refresh'));await waitFor(()=>expect(audits.length).toBe(2));
 guides[1].resolve({data:[],error:null});audits[1].resolve({data:[row(1)],error:null});
 expect(await screen.findByText(/Item item-1/)).toBeTruthy();expect(screen.queryByText(/Could not load activity/)).toBeNull();
});
it('retries the next activity page and ignores old results after refresh',async()=>{
 render(<Admin session={session} authLoading={false} go={()=>{}}/>);await waitFor(()=>expect(audits.length).toBe(1));
 guides[0].resolve({data:[],error:null});audits[0].resolve({data:Array.from({length:21},(_,i)=>row(21-i)),error:null});
 fireEvent.click(await screen.findByText('Load more activity'));await waitFor(()=>expect(audits.length).toBe(2));expect(audits[1].cursor).toBe(2);
 audits[1].resolve({data:null,error:new Error('offline')});fireEvent.click(await screen.findByText('Retry loading activity'));
 await waitFor(()=>expect(audits.length).toBe(3));fireEvent.click(screen.getByText('Refresh'));await waitFor(()=>expect(audits.length).toBe(4));
 guides[1].resolve({data:[],error:null});audits[3].resolve({data:[row(30)],error:null});expect(await screen.findByText(/Item item-30/)).toBeTruthy();
 audits[2].resolve({data:[row(1)],error:null});await waitFor(()=>expect(screen.queryByText(/Item item-1 ·/)).toBeNull());
});
