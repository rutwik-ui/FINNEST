import React, {useEffect,useRef,useState} from 'react';
import {DRAFT_KEY,decodeRecovery} from './localDraft';

export default function LocalDataSettings({onDelete,onRestore}) {
 const [open,setOpen]=useState(false),[confirmation,setConfirmation]=useState(''),[message,setMessage]=useState('');
 const [restoreOpen,setRestoreOpen]=useState(false),[restoreConfirmation,setRestoreConfirmation]=useState(''),[recovery,setRecovery]=useState(null),[fileMessage,setFileMessage]=useState('');
 const readVersion=useRef(0);
 useEffect(()=>()=>{readVersion.current++},[]);
 function chooseRecovery(e){
  const file=e.target.files?.[0], version=++readVersion.current;
  setRecovery(null);setRestoreConfirmation('');setFileMessage('');
  if(!file)return;
  if(file.size>5*1024*1024){setFileMessage('File is too large (maximum 5 MiB).');return}
  const reader=new FileReader();
  reader.onload=()=>{if(version!==readVersion.current)return;try{decodeRecovery(reader.result);setRecovery(reader.result);setFileMessage('File is structurally compatible. Review the replacement warning below.')}catch{setFileMessage('File is not a compatible FINNEST browser draft. No data was changed.')}};
  reader.onerror=()=>{if(version===readVersion.current)setFileMessage('Could not read the selected file. No data was changed.')};
  reader.readAsText(file);
 }
 function restore(e){
  e.preventDefault();if(!recovery || restoreConfirmation!=='RESTORE')return;
  if(onRestore(recovery)){readVersion.current++;setRecovery(null);setRestoreOpen(false);setRestoreConfirmation('');setFileMessage('');setMessage('Browser draft restored from the selected file.');}
  else setMessage('Could not restore data. The existing draft was not intentionally replaced; check the storage warning and reload if another tab changed it.');
 }
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
  <p>Download a copy of the currently saved browser draft, including both journeys, profile and reminder notes. It may contain sensitive personal information and is not encrypted. Keep it private and delete unwanted copies. Unsaved changes in this tab are not included. This is a raw website recovery copy, not an Android or cloud backup.</p>
  <button type="button" className="outline-button" onClick={exportSaved}>Download saved browser draft</button>
  {!restoreOpen?<button type="button" className="outline-button admin-space" onClick={()=>{setMessage('');setRestoreOpen(true)}}>Restore browser draft</button>:<form className="auth-form" onSubmit={restore}>
   <p>Restoring replaces the entire current browser draft, including both journeys, profile, reminders and any unsaved changes in this tab. It cannot be undone. Download the current saved draft first if you may need it. This does not change Android, account or cloud data.</p>
   <label>Choose a FINNEST browser draft file<input type="file" accept=".json,application/json" onChange={chooseRecovery}/></label>
   {fileMessage&&<p role="status">{fileMessage}</p>}
   <label>Type RESTORE to confirm<input value={restoreConfirmation} autoComplete="off" onChange={e=>setRestoreConfirmation(e.target.value)}/></label>
   <div className="admin-actions"><button type="submit" className="outline-button" disabled={!recovery || restoreConfirmation!=='RESTORE'}>Replace browser draft</button><button type="button" className="text-link" onClick={()=>{readVersion.current++;setRestoreOpen(false);setRecovery(null);setRestoreConfirmation('');setFileMessage('');setMessage('')}}>Cancel restore</button></div>
  </form>}
  <p>Delete removes this browser’s FINNEST journey draft, journey selection, local profile and reminder notes. It does not sign you out, delete your account, website authentication data, cloud records, or Android data. This cannot be undone.</p>
  {!open?<button className="outline-button" onClick={()=>{setMessage('');setOpen(true)}}>Delete local browser data</button>:<form className="auth-form" onSubmit={remove}>
   <label>Type DELETE to confirm<input value={confirmation} autoComplete="off" onChange={e=>setConfirmation(e.target.value)}/></label>
   <div className="admin-actions"><button type="submit" className="outline-button" disabled={confirmation!=='DELETE'}>Delete browser data</button><button type="button" className="text-link" onClick={()=>{setOpen(false);setConfirmation('');setMessage('')}}>Cancel</button></div>
  </form>}
  {message&&<p role="status">{message}</p>}
 </section>
}
