import {DEVELOPER_CHECKS,LEGAL_CHECKS,LENDER_CHECKS,INITIAL_COSTS} from './newJourney.js';
import {VISIT_CHECKS} from './visit.js';
import {RESALE_CONDITION,RESALE_SECTIONS,resalePath} from './resale.js';
export function relatedChecks(journey) {
 const groups=journey==='New Property' ? [
  ['Developer checks','checks','/journey',DEVELOPER_CHECKS],['Visit observations','visitChecks','/visit',VISIT_CHECKS],['Lender documents','lenderChecks','/loan',LENDER_CHECKS],['Legal review','legalChecks','/legal',LEGAL_CHECKS],['Cost prompts','costChecks','/financial',INITIAL_COSTS.slice(1)]
 ] : journey==='Resale Property' ? [
  ['Condition inspection','resaleCondition','/resale-condition',RESALE_CONDITION], ...RESALE_SECTIONS.map(s=>[s.title,s.key,resalePath(s.id),s.checks])
 ] : [];
 return groups.flatMap(([group,key,path,items])=>items.map(([id,title])=>({value:`${key}:${id}`,group,key,id,title,path})));
}
export function pendingPatch(data,item) {
 const target=item && relatedChecks(data.journey).find(x=>x.value===item.value);
 if(!target)return null;
 return {[target.key]:{...data[target.key],[target.id]:{...data[target.key][target.id],status:'Pending / unknown'}}};
}
