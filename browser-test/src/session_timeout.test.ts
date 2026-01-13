import {test, expect} from './support/civiform_fixtures'
import {
  enableFeatureFlag,
  loginAsAdmin,
  loginAsTestUser,
  validateScreenshot,
} from './support'

// Config values from application.dev-browser-tests.conf:
// - Inactivity warning at: 10 minutes
// - Inactivity timeout at: 60 minutes
// - Session duration warning at: 20 minutes
// - Maximum session at: 120 minutes

test.describe('Session timeout for admins', () => {
  test.beforeEach(async ({page}) => {
    // Get the current real time
    const realTime = Date.now()

    // Install clock at the current real time
    // This way server timestamps and browser time should be in sync
    await page.clock.install({time: realTime})

    await enableFeatureFlag(page, 'session_timeout_enabled')
    await loginAsAdmin(page)
  })

  test('shows inactivity warning modal after 50 minutes', async ({page}) => {
    await test.step('Fast forward 50 mins', async () => {
      await page.clock.runFor(3000000)
    })

    await test.step('Validate inactivity warning modal appears', async () => {
      const inactivityModal = page.locator('#session-inactivity-warning-modal')
      await expect(inactivityModal).not.toHaveClass(/display-none/, {
        timeout: 10000,
      })

      await validateScreenshot(page, 'admin-inactivity-warning-modal')
    })

    await test.step('Extend session from inactivity modal', async () => {
      const extendSessionButton = page.getByRole('button', {
        name: 'Extend Session',
      })
      await extendSessionButton.click()

      const inactivityModal = page.locator('#session-inactivity-warning-modal')
      await expect(inactivityModal).toHaveClass(/hidden/, {
        timeout: 10000,
      })
    })

    await test.step('Confirm toast appears confirming that session has been extended', async () => {
      const toast = page.locator('#session-extended-toast')
      await expect(toast).toContainText('Session successfully extended')
    })

    await test.step('Fast forward another 60 mins', async () => {
      await page.clock.runFor(3600000)
    })

    await test.step('Validate user has been logged out', async () => {
      await expect(page).toHaveURL(/\/programs/)
    })
  })
})

test.describe('Session timeout for applicants', () => {
  test.beforeEach(async ({page}) => {
    // Get the current real time
    const realTime = Date.now()

    // Install clock at the current real time
    // This way server timestamps and browser time should be in sync
    await page.clock.install({time: realTime})

    await enableFeatureFlag(page, 'session_timeout_enabled')
  })

  test('shows inactivity warning modal after 50 minutes', async ({
    page,
  }) => {
    await test.step('Create and login as applicant', async () => {
      await page.goto('/')
      await loginAsTestUser(page)
    })

    await test.step('Fast forward 50 mins', async () => {
      await page.clock.runFor(3000000)
    })

    await test.step('Validate inactivity warning modal appears', async () => {
      const inactivityModal = page.locator('#session-inactivity-warning-modal')
      await expect(inactivityModal).not.toHaveClass(/hidden/, {
        timeout: 10000,
      })

      await validateScreenshot(page, 'applicant-inactivity-warning-modal')
    })

    await test.step('Extend session from inactivity modal', async () => {
      const extendSessionButton = page.getByRole('button', {
        name: 'Extend Session',
      })
      await extendSessionButton.click()

      const inactivityModal = page.locator('#session-inactivity-warning-modal')
      await expect(inactivityModal).toHaveClass(/hidden/, {
        timeout: 10000,
      })
    })

    await test.step('Confirm toast appears confirming that session has been extended', async () => {
      const toast = page.locator('#session-extended-toast')
      await expect(toast).toContainText('Session successfully extended')
    })

    await test.step('Fast forward another 60 mins', async () => {
      await page.clock.runFor(7200000)
    })
  })
})
