#!/usr/bin/env node

// Writes a Playwright --test-list file for one CI shard, balancing shards by
// expected test duration instead of by test count.
//
// Usage: node bin/shard_tests.mjs <current>/<total> <output file> [playwright args]
//
// Any --grep, --grep-invert or --project arguments are applied when listing
// tests, so the split matches what the run will actually execute (Azure runs
// pass --grep-invert @skip-on-azure). Other arguments are ignored.
//
// Every shard runs this against the same checkout, the same arguments and the
// same shard-durations.json, so all shards compute the same assignment.
//
// Adding, removing, renaming and moving tests:
// - Entries are a file plus a title path, never a line number, so editing a
//   file does not change any other test's entry.
// - A test missing from shard-durations.json (new or renamed) gets the median
//   duration of the tests that are listed.
// - Entries for tests that no longer exist are ignored.
// - Playwright treats each --test-list entry as a prefix, and it can't match a
//   title that contains the separator, a newline, or leading or trailing
//   spaces. Tests that one entry would select together are kept on one shard,
//   and a title that can't be matched is selected through its describe block
//   or file instead. So every test is selected by exactly one shard.
// - After writing the list, this asks Playwright which tests the list
//   selects and exits with an error unless that is exactly this shard's
//   tests. The wrapper then fails the shard instead of silently running the
//   wrong tests.
//
// Tests are assigned one by one, so this must not be used with
// test.describe.configure({mode: 'serial'}) groups. A no-restricted-syntax
// rule in eslint.config.mjs rejects them.
// See bin/update_shard_durations.mjs for how the durations are produced.

import {createHash} from 'node:crypto'
import {existsSync, readFileSync, writeFileSync} from 'node:fs'
import path from 'node:path'
import {
  browserTestDir,
  filterArgs,
  keyFor,
  listTests,
  SEPARATOR,
} from './list_tests.mjs'

const [shardArg, outputPath, ...playwrightArgs] = process.argv.slice(2)
const match = /^(\d+)\/(\d+)$/.exec(shardArg ?? '')
if (!match || !outputPath) {
  console.error(
    'Usage: shard_tests.mjs <current>/<total> <output file> [playwright args]',
  )
  process.exit(2)
}
const current = Number(match[1])
const total = Number(match[2])
if (current < 1 || current > total) {
  console.error(`Shard ${shardArg} is out of range`)
  process.exit(2)
}
const extraArgs = filterArgs(playwrightArgs)

const median = (values) => {
  const sorted = [...values].sort((a, b) => a - b)
  return sorted[Math.floor(sorted.length / 2)]
}

// A title Playwright's --test-list parser can't match exactly: it splits each
// line on the separator character and trims every piece.
const SEPARATOR_CHAR = SEPARATOR.trim()
const unmatchable = (title) =>
  title === '' ||
  title !== title.trim() ||
  title.includes(SEPARATOR_CHAR) ||
  /[\r\n]/.test(title)

// The longest prefix of the test's path that a --test-list line can express.
// It is the full path for almost every test.
const selectorFor = (test) => {
  const titles = test.titlePath.slice(1)
  const usable = titles.findIndex(unmatchable)
  return [test.listFile, ...(usable === -1 ? titles : titles.slice(0, usable))]
}
const lineFor = (selector) => selector.join(SEPARATOR)
const isPrefix = (prefix, of) =>
  prefix.length <= of.length && prefix.every((part, i) => of[i] === part)

const tests = listTests(extraArgs)
if (tests.length === 0) {
  throw new Error('Playwright listed no tests')
}
for (const test of tests) {
  test.key = keyFor(test.titlePath)
  test.selector = selectorFor(test)
  // Matching is done against the list-file form of the path.
  test.matchPath = [test.listFile, ...test.titlePath.slice(1)]
}

// Group tests that must share a shard: any test a selector matches goes with
// the test that selector belongs to (union-find). Selectors only match within
// a file, so compare tests file by file.
const parent = tests.map((_, i) => i)
const find = (i) => (parent[i] === i ? i : (parent[i] = find(parent[i])))
const byFile = new Map()
tests.forEach((test, i) => {
  byFile.set(test.listFile, [...(byFile.get(test.listFile) ?? []), i])
})
for (const indexes of byFile.values()) {
  for (const i of indexes) {
    for (const j of indexes) {
      if (i !== j && isPrefix(tests[i].selector, tests[j].matchPath)) {
        parent[find(j)] = find(i)
      }
    }
  }
}
const groups = new Map() // root index -> test indexes
tests.forEach((_, i) => {
  const root = find(i)
  groups.set(root, [...(groups.get(root) ?? []), i])
})

const durationsPath = path.join(browserTestDir, 'shard-durations.json')
const durations = existsSync(durationsPath)
  ? JSON.parse(readFileSync(durationsPath, 'utf8'))
  : {}
const known = tests.filter((t) => t.key in durations)
const defaultDuration =
  known.length > 0 ? median(known.map((t) => durations[t.key])) : 1

// Longest-processing-time-first: place each group, longest first, on the
// currently lightest shard. Ties break on the group's first key so the result
// is the same on every shard.
const units = [...groups.values()].map((indexes) => {
  const members = indexes.map((i) => tests[i])
  return {
    members,
    firstKey: members.map((t) => t.key).sort()[0],
    lines: [...new Set(members.map((t) => lineFor(t.selector)))].sort(),
    seconds: members.reduce(
      (sum, t) => sum + (durations[t.key] ?? defaultDuration) * t.count,
      0,
    ),
  }
})
units.sort(
  (a, b) =>
    b.seconds - a.seconds || (a.firstKey < b.firstKey ? -1 : a.firstKey > b.firstKey ? 1 : 0),
)

const load = new Array(total).fill(0)
const assigned = Array.from({length: total}, () => [])
for (const unit of units) {
  const lightest = load.indexOf(Math.min(...load))
  load[lightest] += unit.seconds
  assigned[lightest].push(unit)
}

const mine = assigned[current - 1]
const lines = mine.flatMap((u) => u.lines).sort()
writeFileSync(outputPath, lines.join('\n') + '\n')

// Fingerprint of the whole assignment. Every shard of a run should print the
// same one; if they differ, the shards disagreed about the split.
const fingerprint = createHash('sha256')
  .update(
    assigned
      .map((shard) => shard.flatMap((u) => u.lines).sort().join('\n'))
      .join('\n--\n'),
  )
  .digest('hex')
  .slice(0, 12)

// Check the list with Playwright itself: it must select exactly this shard's
// tests, each the expected number of times.
const expected = new Map(
  mine.flatMap((u) => u.members).map((t) => [t.key, t.count]),
)
const selected = new Map(
  listTests([...extraArgs, `--test-list=${path.resolve(outputPath)}`]).map(
    (t) => [keyFor(t.titlePath), t.count],
  ),
)
const problems = []
for (const [key, count] of expected) {
  if (selected.get(key) !== count) {
    problems.push(`expected ${count}, selected ${selected.get(key) ?? 0}: ${key}`)
  }
}
for (const [key, count] of selected) {
  if (!expected.has(key)) {
    problems.push(`not assigned to this shard, selected ${count}: ${key}`)
  }
}
if (problems.length > 0) {
  console.error(
    `Shard ${current}/${total}: the test list does not select exactly this ` +
      `shard's tests (${problems.length} problems):\n` +
      problems.slice(0, 20).join('\n'),
  )
  process.exit(1)
}

const unknown = tests.length - known.length
const grouped = units.filter((u) => u.members.length > 1).length
console.error(
  `Shard ${current}/${total}: ${expected.size} tests in ${lines.length} list entries, ` +
    `~${(load[current - 1] / 60).toFixed(1)} min expected ` +
    `(heaviest shard ~${(Math.max(...load) / 60).toFixed(1)} min). ` +
    `${tests.length} tests listed, ${unknown} without a recorded duration` +
    (grouped > 0 ? `, ${grouped} groups kept on one shard` : '') +
    `. Assignment ${fingerprint}.`,
)
if (unknown > tests.length * 0.05) {
  console.error(
    `::warning::${unknown} of ${tests.length} browser tests have no recorded duration, so shards ` +
      'may be unbalanced. Refresh browser-test/shard-durations.json with ' +
      'bin/update_shard_durations.mjs --from-github',
  )
}
