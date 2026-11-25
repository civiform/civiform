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
//     Uses results.json files you already downloaded under <dir>, one
//     subdirectory per run (<dir>/<run>/.../results.json).
//   --summary=<file>, before either form, also writes a JSON summary that
//     says whether the committed file needs a refresh (see needsRefresh
//     below).
//
// Run it on a checkout that is up to date with main (merge main into your
// branch first). Tests that main has added since your checkout would be
// dropped, and CI, which tests the merge with main, would still warn.
//
// The "Browser Tests - Refresh Shard Durations" workflow runs this weekly on
// main and opens a PR with the new file when needsRefresh is true. To refresh
// by hand, run it here and commit shard-durations.json.
//
// Each test's duration is the median of its passing attempts across all the
// results.json files found. Only tests that exist in this checkout are kept,
// so tests deleted since those runs (or only present on another PR's branch)
// drop out of the file.

import {execFileSync} from 'node:child_process'
import {
  existsSync,
  mkdtempSync,
  readdirSync,
  readFileSync,
  statSync,
  writeFileSync,
} from 'node:fs'
import os from 'node:os'
import path from 'node:path'
import {browserTestDir, keyFor, listTests, median} from './list_tests.mjs'

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
    } catch (error) {
      // Show gh's own message: expired artifacts, a logged-out gh and a
      // rate limit all end up here.
      const reason = String(error.stderr ?? error.message).trim()
      console.error(`Skipping run ${id}: ${reason}`)
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

// A refresh is worth a PR when the shards CI ran are this far out of balance
// (heaviest shard over the mean shard, timed with the new durations), or when
// this share of current tests has no duration in the committed file.
const MAX_IMBALANCE = 1.1
const MAX_MISSING_SHARE = 0.03

const args = process.argv.slice(2)
const summaryArg = args.find((arg) => arg.startsWith('--summary='))
const [firstArg, secondArg] = args.filter((arg) => arg !== summaryArg)
if (!firstArg) {
  console.error(
    'Usage: update_shard_durations.mjs [--summary=<file>] --from-github [runs] | <dir with results.json files>',
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
const walk = (suite, titles, shardKeys) => {
  for (const spec of suite.specs ?? []) {
    const key = keyFor([...titles, spec.title])
    for (const test of spec.tests) {
      shardKeys.push(key)
      // Only passing attempts: a failed attempt's duration says nothing about
      // how long the test takes.
      const passed = test.results.filter((r) => r.status === 'passed')
      if (passed.length === 0) continue
      const seconds = passed[passed.length - 1].duration / 1000
      samples.set(key, [...(samples.get(key) ?? []), seconds])
    }
  }
  for (const child of suite.suites ?? []) {
    walk(child, [...titles, child.title], shardKeys)
  }
}

const files = findResults(root)
if (files.length === 0) {
  console.error(`No results.json files found under ${root}. Nothing written.`)
  process.exit(1)
}
const shardsByRun = new Map() // run directory -> [test keys run by a shard]
for (const file of files) {
  const run = path.relative(root, file).split(path.sep)[0]
  const shardKeys = []
  for (const fileSuite of JSON.parse(readFileSync(file, 'utf8')).suites) {
    walk(fileSuite, [fileSuite.title], shardKeys)
  }
  shardsByRun.set(run, [...(shardsByRun.get(run) ?? []), shardKeys])
}

const current = new Set(listTests().map((t) => keyFor(t.titlePath)))
const durations = {}
for (const key of [...samples.keys()].sort()) {
  if (current.has(key)) {
    durations[key] = Math.round(median(samples.get(key)) * 10) / 10
  }
}
// How well the committed file split the tests: time each shard of each run
// with the new durations, which takes runner speed out of it, and compare the
// heaviest shard with the mean. Take the median across runs, since each run
// may come from a PR that added or removed tests. Runs with a single results
// file can't be compared.
const newSeconds = (key) =>
  samples.has(key) ? median(samples.get(key)) : 0
const imbalances = [...shardsByRun.values()]
  .filter((shards) => shards.length > 1)
  .map((shards) => {
    const loads = shards.map((keys) =>
      keys.reduce((sum, key) => sum + newSeconds(key), 0),
    )
    return (
      Math.max(...loads) / (loads.reduce((sum, s) => sum + s, 0) / loads.length)
    )
  })
const imbalance = imbalances.length > 0 ? median(imbalances) : null
const dropped = [...samples.keys()].filter((key) => !current.has(key)).length
const missing = [...current].filter((key) => !(key in durations)).length

const out = path.join(browserTestDir, 'shard-durations.json')
const committed = existsSync(out) ? JSON.parse(readFileSync(out, 'utf8')) : {}
const missingBefore = [...current].filter((key) => !(key in committed)).length
writeFileSync(out, JSON.stringify(durations, null, 2) + '\n')
console.error(
  `Wrote ${Object.keys(durations).length} test durations from ${files.length} results files to ${out}. ` +
    `Dropped ${dropped} tests that aren't in this checkout; ${missing} tests here have no passing result yet.`,
)

if (summaryArg) {
  const missingShare = missingBefore / current.size
  const reasons = []
  if (imbalance !== null && imbalance > MAX_IMBALANCE) {
    reasons.push(
      `The heaviest shard takes ${Math.round((imbalance - 1) * 100)}% longer ` +
        `than the average shard (median of ${imbalances.length} runs).`,
    )
  }
  if (missingShare > MAX_MISSING_SHARE) {
    reasons.push(
      `${missingBefore} of ${current.size} tests had no recorded duration.`,
    )
  }
  const summary = {
    needsRefresh: reasons.length > 0,
    reasons,
    imbalance: imbalance === null ? null : Math.round(imbalance * 1000) / 1000,
    runsCompared: imbalances.length,
    tests: current.size,
    missingBefore,
    missingAfter: missing,
    resultsFiles: files.length,
  }
  writeFileSync(summaryArg.slice('--summary='.length), JSON.stringify(summary, null, 2) + '\n')
  console.error(JSON.stringify(summary))
}
