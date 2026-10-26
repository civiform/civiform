#!/usr/bin/env node

// Writes a Playwright --test-list file for one CI shard, balancing shards by
// expected test duration instead of by test count.
//
// Usage: node bin/shard_tests.mjs <current>/<total> <output file>
//
// Every shard runs this against the same test list and the same
// shard-durations.json, so all shards agree on the assignment. Tests missing
// from shard-durations.json (new tests) get the median duration, so they are
// still assigned to exactly one shard. Tests are assigned one by one, so this
// must not be used with test.describe.configure({mode: 'serial'}) groups
// (none exist today); Playwright's own sharding keeps those together. See
// shard-durations.json and
// bin/update_shard_durations.mjs for how the durations are produced.

import {execFileSync} from 'node:child_process'
import {readFileSync, writeFileSync} from 'node:fs'
import path from 'node:path'
import {fileURLToPath} from 'node:url'

const browserTestDir = path.resolve(
  path.dirname(fileURLToPath(import.meta.url)),
  '..',
)
const durationsPath = path.join(browserTestDir, 'shard-durations.json')

const [shardArg, outputPath] = process.argv.slice(2)
const match = /^(\d+)\/(\d+)$/.exec(shardArg ?? '')
if (!match || !outputPath) {
  console.error('Usage: shard_tests.mjs <current>/<total> <output file>')
  process.exit(2)
}
const current = Number(match[1])
const total = Number(match[2])
if (current < 1 || current > total) {
  console.error(`Shard ${shardArg} is out of range`)
  process.exit(2)
}

const median = (values) => {
  const sorted = [...values].sort((a, b) => a - b)
  return sorted[Math.floor(sorted.length / 2)]
}

// Test keys have the form "<file relative to testDir> › <describe> › <title>".
// Playwright's --test-list matches by file and title path prefix, so the same
// key (with the file made relative to the config dir) selects the test.
const listJson = execFileSync(
  'npx',
  ['playwright', 'test', '--list', '--reporter=json'],
  {cwd: browserTestDir, encoding: 'utf8', maxBuffer: 256 * 1024 * 1024},
)
const list = JSON.parse(listJson)
const testDir = list.config.projects[0].testDir
const fileForTestList = (file) =>
  path
    .relative(list.config.rootDir, path.join(testDir, file))
    .split(path.sep)
    .join('/')

const tests = new Map() // key -> {listLine, count}
const walk = (suite, titles) => {
  for (const spec of suite.specs ?? []) {
    const key = [...titles, spec.title].join(' › ')
    const listLine = [fileForTestList(titles[0]), ...titles.slice(1), spec.title]
      .join(' › ')
    const entry = tests.get(key) ?? {listLine, count: 0}
    entry.count += spec.tests.length
    tests.set(key, entry)
  }
  for (const child of suite.suites ?? []) {
    walk(child, [...titles, child.title])
  }
}
for (const fileSuite of list.suites) {
  walk(fileSuite, [fileSuite.title])
}

const durations = JSON.parse(readFileSync(durationsPath, 'utf8'))
const defaultDuration = median(Object.values(durations))

// Longest-processing-time-first: place each test, longest first, on the
// currently lightest shard. Ties break on key so the result is deterministic.
const weighted = [...tests.entries()].map(([key, entry]) => ({
  key,
  listLine: entry.listLine,
  seconds: (durations[key] ?? defaultDuration) * entry.count,
}))
weighted.sort((a, b) => b.seconds - a.seconds || (a.key < b.key ? -1 : 1))

const load = new Array(total).fill(0)
const assigned = Array.from({length: total}, () => [])
for (const test of weighted) {
  const lightest = load.indexOf(Math.min(...load))
  load[lightest] += test.seconds
  assigned[lightest].push(test.listLine)
}

const mine = assigned[current - 1].sort()
writeFileSync(outputPath, mine.join('\n') + '\n')

const unknown = weighted.filter((t) => !(t.key in durations)).length
console.error(
  `Shard ${current}/${total}: ${mine.length} test entries, ` +
    `~${(load[current - 1] / 60).toFixed(1)} min expected ` +
    `(heaviest shard ~${(Math.max(...load) / 60).toFixed(1)} min, ` +
    `${unknown} tests without a recorded duration)`,
)
