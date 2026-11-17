import { useEffect, useState } from 'react';
import { ArrowRight, Eye, EyeOff, LoaderCircle } from 'lucide-react';
import { supabase } from './supabase';
import { Button } from '@/components/ui/button';
import { Dialog, DialogContent, DialogTitle, DialogDescription } from '@/components/ui/dialog';
import { BrandMark } from './BrandMark';
import { z } from 'zod';

export type AuthMode = 'signin' | 'signup' | 'reset' | 'recover';
export const credentialsSchema = z.object({ email: z.string().trim().email('Enter a valid email address.'), password: z.string().min(8, 'Use at least eight characters.') });
export function AccountForm({ initialMode = 'signin', onSuccess }: { initialMode?: AuthMode; onSuccess?: () => void }) {
  const [mode, setMode] = useState<AuthMode>(initialMode);
  const [email, setEmail] = useState(''), [password, setPassword] = useState(''), [visible, setVisible] = useState(false);
  const [busy, setBusy] = useState(false), [error, setError] = useState(''), [message, setMessage] = useState('');
  const [online, setOnline] = useState(true);
  useEffect(() => { const update = () => setOnline(navigator.onLine); update(); window.addEventListener('online', update); window.addEventListener('offline', update); return () => { window.removeEventListener('online', update); window.removeEventListener('offline', update); }; }, []);
  async function submit(event: React.FormEvent) {
    event.preventDefault(); if (busy || !online) return;
    setError(''); setMessage('');
    if (mode !== 'recover' && !z.string().email().safeParse(email.trim()).success) return setError('Enter a valid email address.');
    if ((mode === 'signup' || mode === 'recover') && password.length < 8) return setError('Use at least eight characters.');
    setBusy(true);
    try {
      if (mode === 'reset') {
        const result = await supabase.auth.resetPasswordForEmail(email.trim(), { redirectTo: `${location.origin}/profile` });
        if (result.error) throw result.error;
        setMessage('If an account exists, a password-reset link has been sent.');
      } else if (mode === 'recover') {
        const result = await supabase.auth.updateUser({ password }); if (result.error) throw result.error;
        setPassword(''); setMessage('Password updated.'); onSuccess?.();
      } else {
        const result = mode === 'signup' ? await supabase.auth.signUp({ email: email.trim(), password }) : await supabase.auth.signInWithPassword({ email: email.trim(), password });
        if (result.error) throw result.error;
        setPassword('');
        if (result.data.session) onSuccess?.();
        else setMessage('Your account was created, but email confirmation is still enabled for this service. Check your inbox.');
      }
    } catch (e) { setError(e instanceof Error ? e.message : 'Unable to continue. Please try again.'); } finally { setBusy(false); }
  }
  const change = (next: AuthMode) => { setMode(next); setError(''); setMessage(''); setPassword(''); };
  return <div className="account-form">
    {mode !== 'recover' && <div className="auth-tabs" aria-label="Account action"><Button variant="ghost" className={mode === 'signin' ? 'active' : ''} onClick={() => change('signin')}>Log In</Button><Button variant="ghost" className={mode === 'signup' ? 'active' : ''} onClick={() => change('signup')}>Sign Up</Button></div>}
    <form className="auth-form" onSubmit={submit}>
      {mode !== 'recover' && <label>Email<input type="email" required autoComplete="email" value={email} onChange={e => setEmail(e.target.value)} maxLength={254}/></label>}
      {mode !== 'reset' && <label>{mode === 'recover' ? 'New password' : 'Password'}<span className="password-field"><input type={visible ? 'text' : 'password'} autoComplete={mode === 'signin' ? 'current-password' : 'new-password'} required minLength={mode === 'signin' ? 1 : 8} value={password} onChange={e => setPassword(e.target.value)}/><Button type="button" variant="ghost" size="icon" aria-label={visible ? 'Hide password' : 'Show password'} onClick={() => setVisible(v => !v)}>{visible ? <EyeOff/> : <Eye/>}</Button></span></label>}
      {!online && <p className="auth-error" role="status">Connect to the internet to access your account.</p>}
      {error && <p className="auth-error" role="alert">{error}</p>}{message && <p className="auth-message" role="status">{message}</p>}
      <Button type="submit" className="primary" disabled={busy || !online}>{busy ? <LoaderCircle className="animate-spin"/> : null}{busy ? 'Please wait…' : mode === 'signup' ? 'Create account' : mode === 'reset' ? 'Send reset link' : mode === 'recover' ? 'Save new password' : 'Log In'}<ArrowRight/></Button>
      {mode === 'signin' && <Button variant="link" type="button" className="text-link" onClick={() => change('reset')}>Forgot password?</Button>}
      {mode === 'reset' && <Button variant="link" type="button" className="text-link" onClick={() => change('signin')}>Return to Log In</Button>}
    </form>
  </div>;
}

export function AuthDialog({ mode, onClose, onSuccess }: { mode: AuthMode | null; onClose: () => void; onSuccess: () => void }) {
  return <Dialog open={mode !== null} onOpenChange={open => { if (!open) onClose(); }}><DialogContent className="finnest-auth-dialog"><div className="auth-logo"><BrandMark size={56}/><strong>FINNEST</strong></div><DialogTitle>{mode === 'recover' ? 'Choose a new password' : 'Your journey, connected'}</DialogTitle><DialogDescription>Keep your plans and questions together in your FINNEST account.</DialogDescription>{mode && <AccountForm key={mode} initialMode={mode} onSuccess={onSuccess}/>}</DialogContent></Dialog>;
}