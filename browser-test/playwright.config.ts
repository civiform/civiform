import {defineConfig} from '@playwright/test'
import {BASE_URL} from './src/support/config'

// For details see: https://playwright.dev/docs/api/class-testconfig

export default defineConfig({
  // 45s was too small when run against the staging site. Trying to lower
  // this a little bit again to find a middle ground. We could eventually
  // make the timeout an environment variable so we can have a longer
  // timeout for staging.
  timeout: 80000, // 80s
  testDir: './src',
  // Exit with error immediately if test.only() or test.describe.only()
  // was committed
  forbidOnly: !!process.env.CI,
  snapshotPathTemplate: './image_snapshots/{arg}{ext}',
  globalSetup: './src/setup/global-setup.ts',
  fullyParallel: false,
  workers: 1,
  // For us, having retries enabled does not appear to have been particularly
  // beneficial. Instead it makes failues end up taking 2 * timeout. As long
  // as retain-on-failure used below works correctly we're probably fine not
  // retrying.
  retries: 0,
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
    // retain-on-failure should remove trace and video files when a test succeeds allowing
    // for minimizing cloud storage needs.
    trace: process.env.CI === 'true' ? 'retain-on-failure' : 'on',
    video: process.env.RECORD_VIDEO === 'true' ? 'retain-on-failure' : 'off',
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
