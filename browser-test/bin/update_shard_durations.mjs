#!/usr/bin/env node

// Regenerates shard-durations.json from the Playwright JSON results that each
// browser test shard uploads ("test results json (aws) - shard-N-run-1").
//
// Usage:
//   gh run download <run id> --repo civiform/civiform \
//     --pattern 'test results json (aws)*' --dir /tmp/shard-results/<run id>
//   (repeat for a handful of recent green runs; artifacts are kept for 3 days)
//   node bin/update_shard_durations.mjs /tmp/shard-results
//
// Each test's duration is the median of its passing attempts across all the
// results.json files found under the given directory.

import {readdirSync, readFileSync, statSync, writeFileSync} from 'node:fs'
import path from 'node:path'
import {fileURLToPath} from 'node:url'

const root = process.argv[2]
if (!root) {
  console.error('Usage: update_shard_durations.mjs <dir with results.json files>')
  process.exit(2)
}

const findResults = (dir) =>
  readdirSync(dir).flatMap((name) => {
    const full = path.join(dir, name)
    if (statSync(full).isDirectory()) return findResults(full)
    return name === 'results.json' ? [full] : []
  })

const samples = new Map() // key -> seconds[]
const walk = (suite, titles) => {
  for (const spec of suite.specs ?? []) {
    const key = [...titles, spec.title].join(' › ')
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
const durations = {}
for (const key of [...samples.keys()].sort()) {
  durations[key] = Math.round(median(samples.get(key)) * 10) / 10
}

const out = path.join(
  path.dirname(fileURLToPath(import.meta.url)),
  '..',
  'shard-durations.json',
)
writeFileSync(out, JSON.stringify(durations, null, 1) + '\n')
console.error(
  `Wrote ${Object.keys(durations).length} test durations from ${files.length} results files to ${out}`,
)
