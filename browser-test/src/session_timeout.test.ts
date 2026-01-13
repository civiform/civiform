import {test, expect} from './support/civiform_fixtures'
import {enableFeatureFlag, loginAsAdmin, validateScreenshot} from './support'

// Config values from application.dev-browser-tests.conf:
// - Inactivity warning at: 10 minutes (60 - 50)
// - Inactivity timeout at: 60 minutes
// - Session duration warning at: 20 minutes (120 - 100)
// - Maximum session at: 120 minutes

test.describe('Session timeout with clock', () => {
  test('shows inactivity warning modal after 10 minutes', async ({page}) => {
    // Get the current real time BEFORE installing clock
    const realTime = Date.now()

    // Install clock at the CURRENT real time, not a fixed time
    // This way server timestamps and browser time should be in sync
    await page.clock.install({time: realTime})

    await enableFeatureFlag(page, 'session_timeout_enabled')
    await loginAsAdmin(page)

    // Fast forward 10 minutes
    await page.clock.runFor(600000)

    // Wait a bit for handlers to process
    await page.waitForTimeout(10000)

    // Check if modal appears
    const inactivityModal = page.locator('#session-inactivity-warning-modal')
    await expect(inactivityModal).not.toHaveClass(/is-hidden/, {timeout: 10000})

    await validateScreenshot(page, 'admin-inactivity-warning-modal')
  })
})
