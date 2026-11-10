import React, {useState} from 'react';

export default function LocalDataSettings({onDelete}) {
 const [open,setOpen]=useState(false),[confirmation,setConfirmation]=useState(''),[message,setMessage]=useState('');
 function remove(e){
  e.preventDefault();if(confirmation!=='DELETE')return;
  const result=onDelete();
  if(result){setOpen(false);setConfirmation('');setMessage('Local browser journey data deleted.');}
  else setMessage('Could not delete local data. Check the storage warning above.');
 }
 return <section className="card form-card profile-card"><h3>Local browser data</h3>
  <p>Requirements, developer checks, shortlist, visit and resident feedback notes, loan guide, legal and financial notes, and next actions, as well as Resale planning and condition notes, are saved in this browser. Clearing site data or using another browser can remove or hide them. They are not synced or backed up by FINNEST.</p>
  <p>Delete removes this browser’s FINNEST journey draft and journey selection. It does not sign you out, delete your account, website authentication data, cloud records, or Android data. This cannot be undone.</p>
  {!open?<button className="outline-button" onClick={()=>{setMessage('');setOpen(true)}}>Delete local browser data</button>:<form className="auth-form" onSubmit={remove}>
   <label>Type DELETE to confirm<input value={confirmation} autoComplete="off" onChange={e=>setConfirmation(e.target.value)}/></label>
   <div className="admin-actions"><button type="submit" className="outline-button" disabled={confirmation!=='DELETE'}>Delete browser data</button><button type="button" className="text-link" onClick={()=>{setOpen(false);setConfirmation('');setMessage('')}}>Cancel</button></div>
  </form>}
  {message&&<p role="status">{message}</p>}
 </section>
}
