import {test, expect} from './support/civiform_fixtures'
import {
  enableFeatureFlag,
  loginAsAdmin,
  loginAsTestUser,
  validateScreenshot,
} from './support'
import { logoutFromModal } from './support/auth'

// Config values from application.dev-browser-tests.conf:
// - Inactivity warning at: 10 minutes (60 - 50)
// - Inactivity timeout at: 60 minutes
// - Session duration warning at: 20 minutes (120 - 100)
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

  test('shows inactivity warning modal after 10 minutes', async ({page}) => {
    await test.step('Fast forward 10 mins', async () => {
      await page.clock.runFor(600000)
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

    await test.step('Fast forward another 30 mins', async () => {
      await page.clock.runFor(1800000)
    })

    await test.step('Validate session length warning', async () => {
      const inactivityModal = page.locator('#session-length-warning-modal')
      await expect(inactivityModal).not.toHaveClass(/hidden/, {
        timeout: 10000,
      })

      await validateScreenshot(page, 'admin-session-length-warning-modal')
    })

    await test.step('Click logout button on modal', async () => {
      const logoutButton = page.getByRole('button', {name: 'Logout'})
      await logoutButton.click()

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

  test('shows inactivity warning modal after 20 minutes', async ({page}) => {
    await test.step('Create and login as applicant', async () => {
      await page.goto('/')
      await loginAsTestUser(page)
    })

    await test.step('Fast forward 20 mins', async () => {
      await page.clock.runFor(1200000)
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

    await test.step('Fast forward another 30 mins', async () => {
      await page.clock.runFor(1800000)
    })

    await test.step('Validate session length warning', async () => {
      const inactivityModal = page.locator('#session-length-warning-modal')
      await expect(inactivityModal).not.toHaveClass(/hidden/, {
        timeout: 10000,
      })

      await validateScreenshot(page, 'applicant-session-length-warning-modal')
    })

    await test.step('Click logout button on modal', async () => {
      await page.getByTestId('modal-logout-button').click()
      // If the user logged in through OIDC previously - during logout they are
      // redirected to dev-oidc:PORT/session/end page. There they need to confirm
      // logout.
      if (page.url().match('dev-oidc.*/session/end')) {
        const pageContent = await page.textContent('html')
        if (pageContent!.includes('Do you want to sign-out from')) {
          // OIDC central provider confirmation page
          await page.click('button:has-text("Yes")')
        }
      }

      // Logout is handled by the play framework so it doesn't land on a
      // page with civiform js where we should waitForPageJsLoad. Because
      // the process goes through a sequence of redirects we need to wait
      // for the final destination URL (the programs index page), to make tests reliable.
      await page.waitForURL('**/programs')
    })
  })
})
