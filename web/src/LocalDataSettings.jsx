import React, {useState} from 'react';
import {DRAFT_KEY} from './localDraft';

export default function LocalDataSettings({onDelete}) {
 const [open,setOpen]=useState(false),[confirmation,setConfirmation]=useState(''),[message,setMessage]=useState('');
 function exportSaved(){
  let url;
  try {
   const raw=window.localStorage.getItem(DRAFT_KEY);
   if(raw===null){setMessage('No saved browser draft to export.');return}
   url=URL.createObjectURL(new Blob([raw],{type:'application/json'}));
   const link=document.createElement('a');link.href=url;link.download='finnest-browser-draft.json';
   document.body.appendChild(link);link.click();link.remove();
   setMessage('Download requested. Check your browser downloads and store the file securely.');
  } catch {setMessage('Could not request a download. Check browser storage and download permissions.');}
  finally {if(url)URL.revokeObjectURL(url)}
 }
 function remove(e){
  e.preventDefault();if(confirmation!=='DELETE')return;
  const result=onDelete();
  if(result){setOpen(false);setConfirmation('');setMessage('Local browser journey data deleted.');}
  else setMessage('Could not delete local data. Check the storage warning above.');
 }
 return <section className="card form-card profile-card"><h3>Local browser data</h3>
  <p>Requirements, developer checks, shortlist, visit and resident feedback notes, loan guide, legal and financial notes, and next actions, as well as Resale planning and checklist notes, local profile and reminder notes, are saved in this browser. Clearing site data or using another browser can remove or hide them. They are not synced or backed up by FINNEST.</p>
  <p>Download a copy of the currently saved browser draft, including both journeys, profile and reminder notes. It may contain sensitive personal information and is not encrypted. Keep it private and delete unwanted copies. Unsaved changes in this tab are not included. This is a raw recovery copy; restoring it in FINNEST is not yet supported.</p>
  <button type="button" className="outline-button" onClick={exportSaved}>Download saved browser draft</button>
  <p>Delete removes this browser’s FINNEST journey draft, journey selection, local profile and reminder notes. It does not sign you out, delete your account, website authentication data, cloud records, or Android data. This cannot be undone.</p>
  {!open?<button className="outline-button" onClick={()=>{setMessage('');setOpen(true)}}>Delete local browser data</button>:<form className="auth-form" onSubmit={remove}>
   <label>Type DELETE to confirm<input value={confirmation} autoComplete="off" onChange={e=>setConfirmation(e.target.value)}/></label>
   <div className="admin-actions"><button type="submit" className="outline-button" disabled={confirmation!=='DELETE'}>Delete browser data</button><button type="button" className="text-link" onClick={()=>{setOpen(false);setConfirmation('');setMessage('')}}>Cancel</button></div>
  </form>}
  {message&&<p role="status">{message}</p>}
 </section>
}
