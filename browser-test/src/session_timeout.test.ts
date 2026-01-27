import {test, expect} from './support/civiform_fixtures'
import {
  enableFeatureFlag,
  loginAsAdmin,
  loginAsTestUser,
  validateScreenshot,
} from './support'

// Config values from application.dev-browser-tests.conf:
// - Inactivity warning at: 50 minutes (60 - 10)
// - Inactivity timeout at: 60 minutes
// - Session duration warning at: 55 minutes (65 - 10)
// - Maximum session at: 65 minutes
// Note: inactivity timeout must be < max session for inactivity warning to show

test.describe('Session timeout for admins', () => {
  test.beforeEach(async ({page}) => {
    // Get the current real time
    const realTime = Date.now()

    // Install clock at the current real time
    // This way server timestamps and browser time should be in sync
    await page.clock.install({time: realTime})
    await loginAsAdmin(page)
    await enableFeatureFlag(page, 'session_timeout_enabled')
  })

  test('shows inactivity warning modal after 50 minutes and logs user out after 80 more minutes', async ({
    page,
  }) => {
    await test.step('Fast forward 50 mins', async () => {
      await page.clock.runFor('50:00')
    })

    await test.step('Validate inactivity warning modal appears', async () => {
      const inactivityModal = page.locator('#session-inactivity-warning-modal')
      await expect(inactivityModal).not.toHaveClass(/is-hidden/)

      await validateScreenshot(page, 'admin-inactivity-warning-modal')
    })

    await test.step('Extend session from inactivity modal', async () => {
      const extendSessionButton = page.getByRole('button', {
        name: 'Extend Session',
      })
      await extendSessionButton.click()

      const inactivityModal = page.locator('#session-inactivity-warning-modal')
      await expect(inactivityModal).toHaveClass(/is-hidden/)
    })

    await test.step('Confirm toast appears confirming that session has been extended', async () => {
      const toast = page.locator('#session-extended-toast')
      await expect(toast).toContainText('Session successfully extended')
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
  })

  test('shows inactivity warning modal after 50 minutes and session length warning modal after 55 minutes', async ({
    page,
  }) => {
    await test.step('Create and login as applicant', async () => {
      await page.goto('/')
      await loginAsTestUser(page)
    })

    await test.step('Fast forward 50 mins', async () => {
      await page.clock.runFor('50:00')
    })

    await test.step('Validate inactivity warning modal appears', async () => {
      const inactivityModal = page.locator('#session-inactivity-warning-modal')
      await expect(inactivityModal).not.toHaveClass(/is-hidden/)

      await validateScreenshot(page, 'applicant-inactivity-warning-modal')
    })

    await test.step('Close inactivity modal', async () => {
      const closeModalButton = page.getByRole('button', {
        name: 'Close',
      })
      await closeModalButton.click()
    })

    await test.step('Fast forward another 5 mins', async () => {
      await page.clock.runFor('05:00')
    })

    await test.step('Validate session length warning modal appears', async () => {
      const sessionLengthModal = page.locator('#session-length-warning-modal')
      await expect(sessionLengthModal).not.toHaveClass(/is-hidden/)

      await validateScreenshot(page, 'applicant-session-length-warning-modal')
    })

    await test.step('Click logout', async () => {
      const closeModalButton = page.getByTestId('modal-logout-button')
      await closeModalButton.click()
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

      expect(page.url()).toMatch(/\/programs/)
    })
  })
})
