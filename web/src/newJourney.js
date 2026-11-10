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
export const INITIAL_COSTS=[['price','Property price'],['tax','Applicable taxes / GST'],['stamp','Stamp duty'],['registration','Registration'],['parking','Parking'],['deposit','Maintenance deposit'],['brokerage','Brokerage'],['legal','Legal fees'],['loan','Loan fees'],['interiors','Interiors'],['moving','Moving / contingency']];
export const RECURRING_COSTS=[['emi','EMI estimate · monthly'],['maintenance','Maintenance · monthly'],['other','Other recurring costs · monthly']];
export const LOAN_TERMS=[['lender','Lender'],['rate_type','Rate & type'],['fees','Fees'],['tenure','Tenure'],['emi','EMI'],['interest','Estimated total interest'],['prepayment','Prepayment terms']];
export const DEVELOPER_CHECKS=[['track','Track record / delivery'],['rera','Project RERA registration / disclosures'],['title','Land ownership / title'],['approvals','Approvals / sanctioned plans'],['legal','Credible legal history'],['financial','Financial stability where evidence exists']];

// Integer paise avoids floating-point rounding for money and large entered totals.
export function costTotal(values) {
 let paise=0n,count=0;
 for(const raw of values) {
  if(!raw.trim())continue;
  if(!/^\d{1,13}(?:\.\d{1,2})?$/.test(raw))return {kind:'invalid'};
  const [whole,fraction='']=raw.split('.');
  const value=BigInt(whole)*100n+BigInt(fraction.padEnd(2,'0'));
  if(value>100000000000000n)return {kind:'invalid'};
  paise+=value;count++;
 }
 return count?{kind:'entered',paise,count}:{kind:'empty'};
}
export function formatPaise(paise) {
 const whole=(paise/100n).toString(), fraction=(paise%100n).toString().padStart(2,'0');
 const tail=whole.slice(-3), head=whole.slice(0,-3);
 return `${head?head.replace(/\B(?=(\d{2})+(?!\d))/g,',')+',':''}${tail}.${fraction}`;
}
