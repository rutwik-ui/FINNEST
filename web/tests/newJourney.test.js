import test from 'node:test';
import assert from 'node:assert/strict';
import {monthlyEmi, LEGAL_CHECKS, LENDER_CHECKS, costTotal, formatPaise} from '../src/newJourney.js';
test('computes illustrative EMI at zero, ordinary and tiny rates',()=>{
 assert.equal(monthlyEmi('120000','0','10'),1000);
 assert.ok(Math.abs(monthlyEmi('1000000','8','20')-8364.4006899)<0.0001);
 assert.ok(Math.abs(monthlyEmi('120000','0.000000001','10')-1000)<0.01);
});
test('rejects malformed or out-of-range estimates',()=>{
 for (const args of [['','8','20'],['0','8','20'],['1000000000001','8','20'],['100','-1','20'],['100','101','20'],['100','8','0'],['100','8','51'],['100','8','1.5'],['Infinity','8','20'],['100,000','8','20'],['100','8e2','20']]) assert.equal(monthlyEmi(...args),null,JSON.stringify(args));
 assert.ok(monthlyEmi('1000000000000','100','50')>0);
});
test('legal and lender prompts have stable unique ids',()=>{
 assert.equal(LEGAL_CHECKS.length,9);assert.equal(new Set(LEGAL_CHECKS.map(x=>x[0])).size,9);
 assert.equal(LENDER_CHECKS.length,5);assert.equal(new Set(LENDER_CHECKS.map(x=>x[0])).size,5);
});

test('sums exact paise without assuming missing amounts are zero',()=>{
 assert.deepEqual(costTotal(['','','']),{kind:'empty'});
 assert.deepEqual(costTotal(['0','','0.01','0.02']),{kind:'entered',paise:3n,count:3});
 const total=costTotal(['1000000000000','1000000000000','0.01']);
 assert.equal(formatPaise(total.paise),'20,00,00,00,00,000.01');assert.equal(total.count,3);
 for(const x of ['1.001','-1','1,000','1000000000000.01','12.',' 1','1e3']) assert.equal(costTotal(['5',x]).kind,'invalid',x);
});
