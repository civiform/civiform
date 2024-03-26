import {defineConfig} from '@playwright/test'
import {BASE_URL} from './src/support/config'

// For details see: https://playwright.dev/docs/api/class-testconfig

export default defineConfig({
  timeout: 180000,
  testDir: './src',
  // Exit with error immediately if test.only() or test.describe.only()
  // was committed
  forbidOnly: !!process.env.CI,
  snapshotPathTemplate: './image_snapshots/{arg}{ext}',
  globalSetup: './src/setup/global-setup.ts',
  globalTeardown: './src/setup/global-teardown.ts',
  fullyParallel: false,
  workers: 1,
  retries: 1,
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
    trace: 'on-first-retry',
    video: process.env.RECORD_VIDEO ? 'on-first-retry' : 'off',
    // Fall back support config file until it is removed
    baseURL: process.env.BASE_URL || BASE_URL, // 'http://civiform:9000'
  },
  reporter: process.env.CI
    ? // CI
      [
        // Blob output used to stictch sharded tests into a single trace
        ['blob'],
        ['list', {printSteps: true}],
      ]
    : // Not CI
      [
        ['list', {printSteps: true}],
        ['html', {open: 'never', outputFolder: 'tmp/html-output'}],
      ],
})
