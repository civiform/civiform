// Run with `node --test .github/scripts` (a step in format.yaml does).
const {test} = require('node:test')
const assert = require('node:assert/strict')
const {parseDiff, buildComments} = require('./post-format-suggestions.js')

function suggestion(files) {
  return buildComments(parseDiff(files))
}

test('anchors each hunk on exactly the replaced lines', () => {
  const diff = [
    'diff --git a/src/a.ts b/src/a.ts',
    'index 1111111..2222222 100644',
    '--- a/src/a.ts',
    '+++ b/src/a.ts',
    '@@ -5,2 +5,2 @@ class A {',
    '-  x = "1";',
    '-  y = "2";',
    "+  x = '1'",
    "+  y = '2'",
    '@@ -9 +9 @@ class A {',
    '-        z()',
    '+    z()',
    '',
  ].join('\n')
  const {comments, skipped} = suggestion(diff)
  assert.equal(skipped, 0)
  assert.deepEqual(comments, [
    {
      path: 'src/a.ts',
      line: 6,
      side: 'RIGHT',
      start_line: 5,
      start_side: 'RIGHT',
      body: "```suggestion\n  x = '1'\n  y = '2'\n```",
    },
    {
      path: 'src/a.ts',
      line: 9,
      side: 'RIGHT',
      body: '```suggestion\n    z()\n```',
    },
  ])
})

test('added content that renders as "+++ b/..." is not a file boundary', () => {
  const diff = [
    'diff --git a/doc.md b/doc.md',
    '--- a/doc.md',
    '+++ b/doc.md',
    '@@ -3 +3,2 @@',
    '-old',
    '+new',
    '+++ b/evil',
    '',
  ].join('\n')
  const {comments} = suggestion(diff)
  assert.equal(comments.length, 1)
  assert.equal(comments[0].path, 'doc.md')
  assert.equal(comments[0].body, '```suggestion\nnew\n++ b/evil\n```')
})

test('fence outgrows backtick runs in the content', () => {
  const diff = [
    'diff --git a/doc.md b/doc.md',
    '--- a/doc.md',
    '+++ b/doc.md',
    '@@ -1 +1,3 @@',
    '-x',
    '+a',
    '+```',
    '+b',
    '',
  ].join('\n')
  const {comments} = suggestion(diff)
  assert.equal(comments[0].body, '````suggestion\na\n```\nb\n````')
})

test('deletion-only hunk emits an empty suggestion block', () => {
  const diff = [
    'diff --git a/a.ts b/a.ts',
    '--- a/a.ts',
    '+++ b/a.ts',
    '@@ -4,2 +3,0 @@',
    '-a',
    '-b',
    '',
  ].join('\n')
  const {comments} = suggestion(diff)
  assert.deepEqual(comments, [
    {
      path: 'a.ts',
      line: 5,
      side: 'RIGHT',
      start_line: 4,
      start_side: 'RIGHT',
      body: '```suggestion\n```',
    },
  ])
})

test('skips pure insertions, which have no line to anchor on', () => {
  const diff = [
    'diff --git a/a.ts b/a.ts',
    '--- a/a.ts',
    '+++ b/a.ts',
    '@@ -4,0 +5,2 @@',
    '+a',
    '+b',
    '',
  ].join('\n')
  const {comments, skipped} = suggestion(diff)
  assert.equal(comments.length, 0)
  assert.equal(skipped, 1)
})

test('skips trailing-newline-only changes, which GitHub cannot apply', () => {
  const diff = [
    'diff --git a/a.ts b/a.ts',
    '--- a/a.ts',
    '+++ b/a.ts',
    '@@ -3 +3 @@',
    '-last',
    '\\ No newline at end of file',
    '+last',
    '',
  ].join('\n')
  const {comments, skipped} = suggestion(diff)
  assert.equal(comments.length, 0)
  assert.equal(skipped, 1)
})

test('skips bodies over the API limit instead of dooming the review', () => {
  const diff = [
    'diff --git a/a.ts b/a.ts',
    '--- a/a.ts',
    '+++ b/a.ts',
    '@@ -1 +1 @@',
    '-x',
    '+' + 'y'.repeat(70000),
    '',
  ].join('\n')
  const {comments, skipped} = suggestion(diff)
  assert.equal(comments.length, 0)
  assert.equal(skipped, 1)
})

test('strips the trailing tab from paths containing spaces', () => {
  const diff = [
    'diff --git "a/my file.ts" "b/my file.ts"',
    '--- "a/my file.ts"',
    '+++ b/my file.ts\t',
    '@@ -1 +1 @@',
    '-x',
    '+y',
    '',
  ].join('\n')
  const {comments} = suggestion(diff)
  assert.equal(comments[0].path, 'my file.ts')
})

test('skips quoted special-character paths but keeps parsing later files', () => {
  const diff = [
    'diff --git "a/we\\tird.ts" "b/we\\tird.ts"',
    '--- "a/we\\tird.ts"',
    '+++ "b/we\\tird.ts"',
    '@@ -1 +1 @@',
    '-x',
    '+y',
    'diff --git a/plain.ts b/plain.ts',
    '--- a/plain.ts',
    '+++ b/plain.ts',
    '@@ -2 +2 @@',
    '-p',
    '+q',
    '',
  ].join('\n')
  const {comments} = suggestion(diff)
  assert.equal(comments.length, 1)
  assert.equal(comments[0].path, 'plain.ts')
  assert.equal(comments[0].line, 2)
})

test('binary and mode-only entries produce no comments', () => {
  const diff = [
    'diff --git a/img.png b/img.png',
    'index 1111111..2222222 100644',
    'Binary files a/img.png and b/img.png differ',
    'diff --git a/run.sh b/run.sh',
    'old mode 100644',
    'new mode 100755',
    '',
  ].join('\n')
  const {comments, skipped} = suggestion(diff)
  assert.equal(comments.length, 0)
  assert.equal(skipped, 0)
})
