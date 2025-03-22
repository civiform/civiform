import {defineConfig} from '@playwright/test'
import {BASE_URL} from './src/support/config'

import fs from 'fs'
import path from 'path'

/**
 * Recursively finds all 'test.ts' files in './src', sorts them to ensure
 * deterministic ordering with even distribution of file sizes across shards.
 */
/* eslint-disable */
function getTestFilesSortedBySize(dir: string = './src'): string[] {
  const results: string[] = []

  function search(directory: string) {
    for (const file of fs.readdirSync(directory)) {
      const fullPath = path.join(directory, file)
      fs.statSync(fullPath).isDirectory()
        ? search(fullPath)
        : file.endsWith('test.ts') && results.push(fullPath)
    }
  }

  search(dir)

  // Return just the file paths
  return partitionFilesBySizeInOrder(results, 10)
}

/**
 * Partitions file paths by file size into roughly balanced buckets,
 * then returns a flat array of file paths: ordered by bucket, then file.
 */
/* eslint-disable */
function partitionFilesBySizeInOrder(
  filePaths: string[],
  numBuckets: number,
): string[] {
  // Map file paths to { path, size }
  const filesWithSizes = filePaths.map((file) => ({
    path: file,
    size: fs.statSync(file).size, // inline file size
  }))

  // Sort by descending size
  filesWithSizes.sort((a, b) => b.size - a.size)

  // Initialize buckets
  const buckets: {sum: number; files: string[]}[] = Array.from(
    {length: numBuckets},
    () => ({
      sum: 0,
      files: [],
    }),
  )

  // Distribute files
  for (const file of filesWithSizes) {
    // Find bucket with the smallest current sum
    let bestBucket = buckets[0]
    for (const bucket of buckets) {
      if (bucket.sum < bestBucket.sum) {
        bestBucket = bucket
      }
    }

    bestBucket.files.push(file.path)
    bestBucket.sum += file.size
  }

  // Return a flat array in bucket order, then file order
  return buckets.flatMap((bucket) => bucket.files)
}

// For details see: https://playwright.dev/docs/api/class-testconfig

export default defineConfig({
  // 45s was too small when run against the staging site. Trying to lower
  // this a little bit again to find a middle ground. We could eventually
  // make the timeout an environment variable so we can have a longer
  // timeout for staging.
  timeout: 90000, // 90s
  testDir: './src',
  // Exit with error immediately if test.only() or test.describe.only()
  // was committed
  forbidOnly: !!process.env.CI,
  snapshotPathTemplate: './image_snapshots/{arg}{ext}',
  globalSetup: './src/setup/global-setup.ts',
  fullyParallel: false,
  workers: 1,
  retries: process.env.CI === 'true' ? 1 : 0,
  outputDir: './tmp/test-output',
  expect: {
    toHaveScreenshot: {
      maxDiffPixelRatio: 0,
      animations: 'disabled',
    },
    toMatchSnapshot: {
      maxDiffPixelRatio: 0,
    },
  },
  use: {
    // retain-on-failure may not be removing files in a timely manner
    // causing disk usage on github actions to fill the disk
    trace: process.env.CI === 'true' ? 'on-first-retry' : 'on',
    video: process.env.RECORD_VIDEO === 'true' ? 'on-first-retry' : 'off',
    // Fall back support config file until it is removed
    baseURL: process.env.BASE_URL || BASE_URL, // 'http://civiform:9000'
  },
  reporter: [
    ['list', {printSteps: true}],
    ['html', {open: 'never', outputFolder: 'tmp/html-output'}],
    ['json', {outputFile: 'tmp/json-output/results.json'}],
    ['./src/reporters/file_placement_reporter.ts'],
    ['./src/reporters/run_heuristics_reporter.ts'],
  ],
  // projects: [
  //   {
  //     testMatch: getTestFilesSortedBySize(),
  //   },
  // ],
})
