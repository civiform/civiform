import {defineConfig} from '@playwright/test'
import {BASE_URL} from './src/support/config'

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
  ],
})
