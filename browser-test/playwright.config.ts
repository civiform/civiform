import {defineConfig} from '@playwright/test'

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
  },
  reporter: [
    ['list', {printSteps: true}],
    ['html', {open: 'never', outputFolder: 'tmp/html-output'}],
  ],
})
