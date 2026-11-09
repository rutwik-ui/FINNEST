// @vitest-environment jsdom
import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render, screen } from '@testing-library/react';
const mocks=vi.hoisted(()=>({from:vi.fn(),aal:vi.fn()}));
vi.mock('../supabase',()=>({supabase:{from:mocks.from,auth:{mfa:{getAuthenticatorAssuranceLevel:mocks.aal},signOut:vi.fn()}}}));
vi.mock('../AdminMfa',()=>({default:()=> <div>Authenticator verification gate</div>}));
import Admin from '../Admin';
const session={access_token:'token1',user:{id:'user1',email:'buyer@example.com'}};
function setup(role,level){
  mocks.aal.mockResolvedValue({data:{currentLevel:level},error:null});
  mocks.from.mockImplementation(table=>{
    if(table==='finnest_admin_roles') return {select:()=>({eq:()=>({maybeSingle:async()=>({data:role?{role}:null,error:null})})})};
    return {select:()=>({order:()=>({limit:async()=>({data:[],error:null})})})};
  });
}
describe('admin access gating',()=>{
 beforeEach(()=>vi.resetAllMocks());afterEach(()=>cleanup());
 it('denies an ordinary authenticated user',async()=>{setup(null,'aal2');render(<Admin session={session} authLoading={false} go={()=>{}}/>);expect(await screen.findByText(/no administrator role/i)).toBeTruthy();expect(screen.queryByText('New guidance')).toBeNull()});
 it('requires MFA for a content manager',async()=>{setup('content_manager','aal1');render(<Admin session={session} authLoading={false} go={()=>{}}/>);expect(await screen.findByText('Authenticator verification gate')).toBeTruthy();expect(screen.queryByText('New guidance')).toBeNull()});
 it('shows the editor only for an authorized verified session',async()=>{setup('content_manager','aal2');render(<Admin session={session} authLoading={false} go={()=>{}}/>);expect(await screen.findByText('New guidance')).toBeTruthy()});
 it('clears prior access when switching accounts',async()=>{setup('content_manager','aal2');const view=render(<Admin session={session} authLoading={false} go={()=>{}}/>);expect(await screen.findByText('New guidance')).toBeTruthy();setup(null,'aal1');view.rerender(<Admin session={{access_token:'token2',user:{id:'user2',email:'other@example.com'}}} authLoading={false} go={()=>{}}/>);expect(screen.queryByText('New guidance')).toBeNull();expect(await screen.findByText(/no administrator role/i)).toBeTruthy()});
});
