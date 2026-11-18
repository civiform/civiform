// Shared by shard_tests.mjs and update_shard_durations.mjs: lists the browser
// tests the way Playwright sees them.

import {execFileSync} from 'node:child_process'
import path from 'node:path'
import {fileURLToPath} from 'node:url'

export const browserTestDir = path.resolve(
  path.dirname(fileURLToPath(import.meta.url)),
  '..',
)

// The separator Playwright uses in test titles, list output and --test-list
// files. Durations are keyed by the title path joined with it.
export const SEPARATOR = ' \u203A '

export const keyFor = (titlePath) => titlePath.join(SEPARATOR)

export const median = (values) => {
  const sorted = [...values].sort((a, b) => a - b)
  return sorted[Math.floor(sorted.length / 2)]
}

// Returns [{titlePath, listFile, count}], one entry per test, in Playwright's
// order. titlePath is [file relative to testDir, ...describe titles, test
// title], the same path the JSON reporter uses, so it is the key into
// shard-durations.json. listFile is the file relative to rootDir, which is
// what a --test-list entry needs. Neither has line numbers, so adding,
// removing or moving other tests never changes them. count is the number of
// projects the test runs in. extraArgs are Playwright filter arguments (like
// --grep-invert) that the run will also use.
export const listTests = (extraArgs = []) => {
  const list = JSON.parse(
    execFileSync(
      'npx',
      ['playwright', 'test', '--list', '--reporter=json', ...extraArgs],
      {cwd: browserTestDir, encoding: 'utf8', maxBuffer: 256 * 1024 * 1024},
    ),
  )
  if (list.errors?.length) {
    throw new Error(
      'Playwright reported errors while listing tests:\n' +
        list.errors.map((e) => e.message ?? JSON.stringify(e)).join('\n'),
    )
  }
  const testDir = list.config.projects[0].testDir
  const rootDir = list.config.rootDir
  const fileFor = (file) =>
    path.relative(rootDir, path.join(testDir, file)).split(path.sep).join('/')

  const tests = new Map() // key -> {titlePath, listFile, count}
  const walk = (suite, titles, listFile) => {
    for (const spec of suite.specs ?? []) {
      const titlePath = [...titles, spec.title]
      const key = keyFor(titlePath)
      const entry = tests.get(key) ?? {titlePath, listFile, count: 0}
      entry.count += spec.tests.length
      tests.set(key, entry)
    }
    for (const child of suite.suites ?? []) {
      walk(child, [...titles, child.title], listFile)
    }
  }
  for (const fileSuite of list.suites ?? []) {
    walk(fileSuite, [fileSuite.title], fileFor(fileSuite.title))
  }
  return [...tests.values()]
}

// Only these Playwright arguments change which tests run, so only these are
// passed on to listTests. Everything else (reporters, --headed, ...) is
// dropped so the list output stays parseable.
const FILTER_FLAGS_WITH_VALUE = new Set(['--grep', '-g', '--grep-invert', '--project'])
export const filterArgs = (args) => {
  const kept = []
  for (let i = 0; i < args.length; i++) {
    const arg = args[i]
    if (FILTER_FLAGS_WITH_VALUE.has(arg) && i + 1 < args.length) {
      kept.push(arg, args[++i])
    } else if (/^--(grep|grep-invert|project)=/.test(arg)) {
      kept.push(arg)
    }
  }
  return kept
}
