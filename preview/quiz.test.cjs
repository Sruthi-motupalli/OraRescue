const fs = require('fs');
const vm = require('vm');
const assert = require('assert');

const html = fs.readFileSync(require('path').join(__dirname, 'index.html'), 'utf8');
const scripts = [...html.matchAll(/<script>([\s\S]*?)<\/script>/g)].map(match => match[1]).join('\n');
const storage = new Map();
const element = () => ({
  innerHTML: '', textContent: '', value: '', hidden: false, returnValue: '',
  classList: { add() {}, remove() {}, toggle() {} },
  setAttribute() {}, removeAttribute() {}, addEventListener() {},
  showModal() {}, close() {}, scrollTop: 0,
  elements: { done: { max: 0, value: 0 } }
});
const elements = new Map();
const context = {
  console, assert, Date, Math, JSON, String, Number, Set, Map, FormData: class {},
  setTimeout() { return 0; }, clearTimeout() {},
  matchMedia() { return { matches: false, addEventListener() {} }; },
  localStorage: {
    getItem(key) { return storage.has(key) ? storage.get(key) : null; },
    setItem(key, value) { storage.set(key, String(value)); },
    clear() { storage.clear(); }
  },
  location: { hash: '#home' },
  document: {
    documentElement: { dataset: {} },
    querySelector(selector) { if (!elements.has(selector)) elements.set(selector, element()); return elements.get(selector); },
    querySelectorAll() { return []; },
    addEventListener() {}
  }
};
context.window = { addEventListener() {}, setTimeout: context.setTimeout, clearTimeout: context.clearTimeout };
vm.createContext(context);
vm.runInContext(scripts + `
  importCourseName('MACHINE-LEARNING.zip');
  assert.equal(S.courses.length, 1);
  assert.equal(S.chapters.length, 10);
  assert.equal(S.tasks.filter(t => t.chapterId).length, 10);
  assert.equal(S.tasks.filter(t => t.chapterId && t.selected).length, 0);
  assert.equal(S.questions.length, 20);
  const chapter = S.chapters[0];
  selectChapterForStudy(chapter.id);
  assert.equal(S.selectedChapterId, chapter.id);
  assert.equal(S.tasks.filter(t => t.chapterId && t.selected).length, 1);
  assert.ok(renderCourses().includes('Selected'));
  assert.ok(!renderCourses().includes('>Play</a>'));
  const selectedTask = S.tasks.find(t => t.chapterId === chapter.id);
  S.sessions.push({id:999,taskId:selectedTask.id,day:plusDays(today(),1),start:600,end:660,minutes:60,done:0,recorded:false});
  const plannedHome = renderHome();
  assert.ok(plannedHome.includes('My Study Plan'));
  assert.ok(plannedHome.includes('Upcoming'));
  assert.ok(!plannedHome.includes('This chapter is selected but has not been scheduled yet.'));
  S.sessions.push({id:1000,taskId:selectedTask.id,day:plusDays(today(),-1),start:600,end:660,minutes:60,done:0,recorded:false});
  finishSessionNow(1000);
  assert.equal(S.sessions.find(s => s.id === 1000).recorded, true);
  assert.equal(quizState.chapterId, chapter.id);
  startQuiz(chapter.id, false);
  assert.ok(renderQuiz().includes('Check answer'));
  selectQuizAnswer(0);
  checkQuiz();
  assert.equal(quizState.checked, true);
  while (!quizState.finished) nextQuiz();
  assert.ok(renderQuiz().includes('Save result'));
  saveQuizResult();
  assert.equal(S.attempts.length, 1);
`, context);
console.log('Browser course import, chapter quiz, feedback and saved mastery checks passed.');
