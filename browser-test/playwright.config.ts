import {defineConfig} from '@playwright/test'

export default defineConfig({
  timeout: 15 * 1000,// 180000,
  testDir: './src',
  snapshotPathTemplate: './image_snapshots/{arg}{ext}',
  globalSetup: './src/setup/global-setup.ts',
  globalTeardown: './src/setup/global-teardown.ts',
  fullyParallel: false,
  workers: 1,
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
    trace: 'on',
    video: process.env.RECORD_VIDEO ? 'on-first-retry' : 'off',
    baseURL: 'http://civiform:9000'
  },
  reporter: [
    ['list', {printSteps: true}],
    ['html', {open: 'never', outputFolder: 'tmp/html-output'}],
  ],
})
