export const STATUSES=['Not checked','Checked by you','Information provided','Pending / unknown','Professional review'];
export const LENDER_CHECKS=[['identity','Identity and address requirements'],['income_docs','Income documentation requirements'],['bank','Bank statement period requested'],['property','Property documents required'],['fees','Processing and legal fee schedule']];
export const LEGAL_CHECKS=[['ownership','Ownership & title chain'],['title_documents','Original sale & title documents'],['encumbrance','Encumbrance search or certificate'],['plans','Sanctioned plans'],['rera','RERA disclosures'],['approvals','Approvals'],['agreement','Agreement'],['payment_schedule','Payment schedule'],['possession_terms','Possession, cancellation & refund clauses']];
export const emptyFeedback=()=>({topic:'',date:'',context:'',shared:'',concern:'',followup:''});

// Pure estimate from explicit inputs, not an eligibility or affordability decision.
export function monthlyEmi(amount, rate, years) {
 const decimal=/^(?:\d+(?:\.\d*)?|\.\d+)$/;
 if(!decimal.test(amount.trim()) || !decimal.test(rate.trim()) || !/^\d+$/.test(years.trim()))return null;
 const p=Number(amount), r=Number(rate), y=Number(years);
 if(!Number.isFinite(p)||p<=0||p>1e12||!Number.isFinite(r)||r<0||r>100||!Number.isSafeInteger(y)||y<1||y>50)return null;
 const months=y*12, monthly=r/1200;
 if(monthly===0)return p/months;
 // expm1/log1p preserve precision for very small nonzero rates.
 const denominator=-Math.expm1(-months*Math.log1p(monthly));
 const result=p*monthly/denominator;
 return Number.isFinite(result)?result:null;
}
