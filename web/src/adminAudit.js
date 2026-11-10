export const AUDIT_PAGE_SIZE=20;
// IDs are PostgreSQL bigint exposed by PostgREST as JSON numbers. Reject values
// outside the exact JS integer range rather than silently skip or duplicate rows.
const safeId=id=>Number.isSafeInteger(id) && id>0;
export async function loadAdminAudit(client,cursor=null){
 if(cursor!==null && !safeId(cursor))throw new Error('Invalid audit cursor');
 let query=client.from('finnest_admin_audit').select('id,actor_id,item_id,action,occurred_at');
 if(cursor!==null)query=query.lt('id',cursor);
 const {data,error}=await query.order('id',{ascending:false}).limit(AUDIT_PAGE_SIZE+1);
 if(error)throw error;
 if(!Array.isArray(data) || data.some(row=>!row || !safeId(row.id)))throw new Error('Invalid audit data');
 const items=data.slice(0,AUDIT_PAGE_SIZE);
 return {items,cursor:data.length>AUDIT_PAGE_SIZE?items.at(-1).id:null};
}
