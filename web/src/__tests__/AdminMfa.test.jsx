// @vitest-environment jsdom
import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';

const mfa = vi.hoisted(() => ({listFactors:vi.fn(), enroll:vi.fn(), unenroll:vi.fn(), challengeAndVerify:vi.fn()}));
vi.mock('../supabase', () => ({supabase:{auth:{mfa}}}));
import AdminMfa from '../AdminMfa';

describe('administrator authenticator flow', () => {
  beforeEach(()=>{vi.resetAllMocks()});
  afterEach(()=>cleanup());
  it('enrolls, shows a setup key, and verifies a six-digit code', async () => {
    mfa.listFactors.mockResolvedValue({data:{all:[]},error:null});
    mfa.enroll.mockResolvedValue({data:{id:'factor-1',totp:{qr_code:'data:image/svg+xml,example',secret:'SETUPKEY'}},error:null});
    mfa.challengeAndVerify.mockResolvedValue({data:{},error:null});
    const done=vi.fn();render(<AdminMfa onVerified={done}/>);
    fireEvent.click(await screen.findByText('Set up authenticator'));
    expect(await screen.findByText('SETUPKEY')).toBeTruthy();
    fireEvent.change(screen.getByLabelText('Six-digit code'),{target:{value:'123456'}});
    fireEvent.click(screen.getByText('Verify and continue'));
    await waitFor(()=>expect(done).toHaveBeenCalledTimes(1));
    expect(mfa.challengeAndVerify).toHaveBeenCalledWith({factorId:'factor-1',code:'123456'});
  });
  it('allows an already enrolled factor and reports a failed verification', async () => {
    mfa.listFactors.mockResolvedValue({data:{all:[{id:'existing',factor_type:'totp',status:'verified',friendly_name:'Phone'}]},error:null});
    mfa.challengeAndVerify.mockResolvedValue({error:new Error('Invalid code')});
    const done=vi.fn();render(<AdminMfa onVerified={done}/>);
    fireEvent.change(await screen.findByLabelText('Six-digit code'),{target:{value:'222222'}});
    fireEvent.click(screen.getByText('Verify and continue'));
    expect(await screen.findByRole('alert')).toHaveProperty('textContent','Invalid code');
    expect(done).not.toHaveBeenCalled();
  });
  it('removes only a stale incomplete FINNEST enrollment when restarting setup', async () => {
    mfa.listFactors.mockResolvedValue({data:{all:[{id:'old',factor_type:'totp',status:'unverified',friendly_name:'FINNEST admin'},{id:'other',factor_type:'totp',status:'unverified',friendly_name:'Other'}]},error:null});
    mfa.unenroll.mockResolvedValue({error:null});
    mfa.enroll.mockResolvedValue({data:{id:'new',totp:{qr_code:'data:image/svg+xml,example',secret:'NEWKEY'}},error:null});
    render(<AdminMfa onVerified={()=>{}}/>);
    fireEvent.click(await screen.findByText('Set up authenticator'));
    expect(await screen.findByText('NEWKEY')).toBeTruthy();
    expect(mfa.unenroll).toHaveBeenCalledTimes(1);
    expect(mfa.unenroll).toHaveBeenCalledWith({factorId:'old'});
  });
  it('does not offer enrollment when factor lookup fails and can retry', async () => {
    mfa.listFactors.mockResolvedValueOnce({data:null,error:new Error('Offline')}).mockResolvedValueOnce({data:{all:[]},error:null});
    render(<AdminMfa onVerified={()=>{}}/>);
    expect(await screen.findByRole('alert')).toBeTruthy();
    expect(screen.queryByText('Set up authenticator')).toBeNull();
    fireEvent.click(screen.getByText('Retry'));
    expect(await screen.findByText('Set up authenticator')).toBeTruthy();
    expect(mfa.enroll).not.toHaveBeenCalled();
  });
});
