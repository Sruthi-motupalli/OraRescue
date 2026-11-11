// Host-side regression checks for the browser's pure planning rules.
const fs=require('node:fs'),vm=require('node:vm'),assert=require('node:assert/strict');
const html=fs.readFileSync(__dirname+'/index.html','utf8');
const source=html.slice(html.indexOf('function generatePlan()'),html.indexOf('function acceptPlan()'));
const progressRule=html.slice(html.indexOf('function canRecord('),html.indexOf('function nowMin()'));
function plan(tasks,windows,sessions=[],split=true,clock=0){
 const context={S:{tasks,windows,sessions},TODAY:'2026-11-11',splitMode:split,preview:null,
  nowMin:()=>clock,localDay:()=> '2026-11-11',render:()=>{},
  winsForDay:d=>windows.filter(w=>w.day===d).sort((a,b)=>a.start-b.start),
  taskLeft:t=>Math.max(0,t.minutes-sessions.filter(s=>s.taskId===t.id).reduce((n,s)=>n+s.done,0))};
 vm.createContext(context);vm.runInContext(source,context);context.generatePlan();return context.preview;
}
const task=(id,minutes=60,exam='2026-11-13')=>({id,minutes,exam,priority:2});
const win=(start,end,day='2026-11-11')=>({start,end,day});
let p=plan([task(1,90)],[win(900,960),win(1200,1260)]);
assert.deepEqual(Array.from(p.sessions,s=>s.minutes),[60,30]);
p=plan([task(1,90)],[win(900,960),win(1200,1260)],[],false);
assert.equal(p.sessions.length,0);assert.equal(p.unplaced[0].minutes,90);
p=plan([task(1,100)],[win(900,1020)],[{taskId:1,day:'2026-11-11',start:960,done:20,recorded:true}]);
assert.deepEqual(Array.from(p.sessions,s=>[s.start,s.end]),[[900,960],[980,1000]]);
p=plan([task(1,90)],[win(900,1020)],[],true,990);
assert.equal(p.sessions[0].start,990);assert.equal(p.unplaced[0].minutes,60);
p=plan([task(1,60,'2026-11-11')],[win(900,960)]);
assert.equal(p.sessions.length,0);assert.equal(p.unplaced[0].minutes,60);
p=plan([task(1,90),task(2,30)],[win(900,930),win(1000,1120)],[],false);
assert.deepEqual(Array.from(p.sessions,s=>s.start),[900,1000]);
const c={localDay:()=> '2026-11-11',nowMin:()=>900};vm.createContext(c);vm.runInContext(progressRule,c);
assert.equal(c.canRecord({day:'2026-11-11',start:901}),false);
assert.equal(c.canRecord({day:'2026-11-11',start:900}),true);
assert.equal(c.canRecord({day:'2026-11-10',start:1000,recorded:true}),false);
console.log('7 browser rule groups passed: splitting, contiguous placement, completed intervals, elapsed time, exam cutoff, chronology and progress gating.');
