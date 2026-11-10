#!/usr/bin/env node

// Regenerates shard-durations.json from the Playwright JSON results that each
// browser test shard uploads ("test results json (aws) - shard-N-run-1").
//
// Usage (needs the gh CLI, logged in):
//   node bin/update_shard_durations.mjs --from-github [number of runs, default 6]
//     Downloads the results of the most recent green "Server - On PR to Main"
//     runs (artifacts are only kept for 3 days, so runs whose results have
//     expired are skipped) and regenerates the file.
//   node bin/update_shard_durations.mjs <dir>
//     Uses results.json files you already downloaded under <dir>.
//
// Run it on a checkout that is up to date with main (merge main into your
// branch first). Tests that main has added since your checkout would be
// dropped, and CI, which tests the merge with main, would still warn.
//
// Then commit shard-durations.json. Refresh it every month or two, or when
// the "tests without a recorded duration" warning shows up in the CI logs.
//
// Each test's duration is the median of its passing attempts across all the
// results.json files found. Only tests that exist in this checkout are kept,
// so tests deleted since those runs (or only present on another PR's branch)
// drop out of the file.

import {execFileSync} from 'node:child_process'
import {
  mkdtempSync,
  readdirSync,
  readFileSync,
  statSync,
  writeFileSync,
} from 'node:fs'
import os from 'node:os'
import path from 'node:path'
import {fileURLToPath} from 'node:url'
import {keyFor, listTests} from './list_tests.mjs'

const REPO = 'civiform/civiform'

const downloadRecentResults = (count) => {
  const dir = mkdtempSync(path.join(os.tmpdir(), 'shard-results-'))
  // Ask for more runs than needed: older runs' results may have expired.
  const runIds = JSON.parse(
    execFileSync(
      'gh',
      [
        'run', 'list', '--repo', REPO,
        '--workflow', 'Server - On PR to Main',
        '--status', 'success', '--limit', String(count * 4),
        '--json', 'databaseId',
      ],
      {encoding: 'utf8'},
    ),
  ).map((run) => run.databaseId)
  let downloaded = 0
  for (const id of runIds) {
    if (downloaded === count) break
    try {
      execFileSync(
        'gh',
        [
          'run', 'download', String(id), '--repo', REPO,
          '--pattern', 'test results json (aws)*',
          '--dir', path.join(dir, String(id)),
        ],
        {stdio: ['ignore', 'inherit', 'pipe']},
      )
      downloaded++
    } catch {
      console.error(`Skipping run ${id}: no results to download (expired?)`)
    }
  }
  if (downloaded === 0) {
    console.error('No run had results to download. Nothing written.')
    process.exit(1)
  }
  if (downloaded < count) {
    console.error(`Only ${downloaded} of ${count} runs had results.`)
  }
  return dir
}

const [firstArg, secondArg] = process.argv.slice(2)
if (!firstArg) {
  console.error(
    'Usage: update_shard_durations.mjs --from-github [runs] | <dir with results.json files>',
  )
  process.exit(2)
}
const root =
  firstArg === '--from-github'
    ? downloadRecentResults(Number(secondArg ?? 6))
    : firstArg

const findResults = (dir) =>
  readdirSync(dir).flatMap((name) => {
    const full = path.join(dir, name)
    if (statSync(full).isDirectory()) return findResults(full)
    return name === 'results.json' ? [full] : []
  })

const samples = new Map() // key -> seconds[]
const walk = (suite, titles) => {
  for (const spec of suite.specs ?? []) {
    const key = keyFor([...titles, spec.title])
    for (const test of spec.tests) {
      // Only passing attempts: a failed attempt's duration says nothing about
      // how long the test takes.
      const passed = test.results.filter((r) => r.status === 'passed')
      if (passed.length === 0) continue
      const seconds = passed[passed.length - 1].duration / 1000
      samples.set(key, [...(samples.get(key) ?? []), seconds])
    }
  }
  for (const child of suite.suites ?? []) {
    walk(child, [...titles, child.title])
  }
}

const files = findResults(root)
for (const file of files) {
  for (const fileSuite of JSON.parse(readFileSync(file, 'utf8')).suites) {
    walk(fileSuite, [fileSuite.title])
  }
}

const median = (values) => {
  const sorted = [...values].sort((a, b) => a - b)
  return sorted[Math.floor(sorted.length / 2)]
}
const current = new Set(listTests().map((t) => keyFor(t.titlePath)))
const durations = {}
for (const key of [...samples.keys()].sort()) {
  if (current.has(key)) {
    durations[key] = Math.round(median(samples.get(key)) * 10) / 10
  }
}
const dropped = [...samples.keys()].filter((key) => !current.has(key)).length
const missing = [...current].filter((key) => !(key in durations)).length

const out = path.join(
  path.dirname(fileURLToPath(import.meta.url)),
  '..',
  'shard-durations.json',
)
writeFileSync(out, JSON.stringify(durations, null, 2) + '\n')
console.error(
  `Wrote ${Object.keys(durations).length} test durations from ${files.length} results files to ${out}. ` +
    `Dropped ${dropped} tests that aren't in this checkout; ${missing} tests here have no passing result yet.`,
)
