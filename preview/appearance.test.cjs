// Exercise the same preference code the preview runs before its first render.
const { readFileSync } = require('node:fs');
const { createContext, runInContext } = require('node:vm');
const assert = require('node:assert/strict');

const html = readFileSync(__dirname + '/index.html', 'utf8');
const script = html.match(/<script>([\s\S]*?)<\/script>/)[1];
const key = 'ora_appearance';
const storage = new Map([['ora_v3', 'existing study data']]);

function loadPreview(dark) {
  const root = { dataset: {} };
  const media = {
    matches: dark,
    addEventListener(_, listener) { this.change = listener; },
  };
  const context = createContext({
    document: { documentElement: root },
    matchMedia: () => media,
    localStorage: {
      getItem: name => storage.get(name),
      setItem: (name, value) => storage.set(name, value),
    },
  });
  runInContext(script, context);
  return { root, media, select: value => context.setAppearance(value) };
}

let preview = loadPreview(true);
assert.equal(preview.root.dataset.theme, 'dark');
preview.select('light');
assert.equal(preview.root.dataset.theme, 'light');

preview = loadPreview(true);
assert.equal(preview.root.dataset.theme, 'light');
preview.select('dark');
assert.equal(loadPreview(false).root.dataset.theme, 'dark');

preview.select('system');
assert.equal(storage.get(key), 'system');
preview.media.matches = false;
preview.media.change();
assert.equal(preview.root.dataset.theme, 'light');
preview.media.matches = true;
preview.media.change();
assert.equal(preview.root.dataset.theme, 'dark');

storage.set(key, 'invalid preference');
assert.equal(loadPreview(false).root.dataset.theme, 'light');
assert.equal(storage.get('ora_v3'), 'existing study data');
console.log('Appearance checks passed: immediate changes, persistence, system updates, fallback and study-data isolation.');
