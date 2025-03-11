import {test, expect} from './support/civiform_fixtures'
import {
  enableFeatureFlag,
  loginAsAdmin,
  loginAsTestUser,
  validateScreenshot,
} from './support'
import {Page} from 'playwright'

type TimeoutData = {
  inactivityWarning: number
  inactivityTimeout: number
  totalWarning: number
  totalTimeout: number
}

test.describe('Session timeout warnings', () => {
  const setTimeoutCookie = async (page: Page, data: TimeoutData) => {
    const now = Math.floor(Date.now() / 1000)
    // Create a JSON object with the timeout data and current time
    const timeoutData = {
      ...data,
      currentTime: now,
    }

    // Convert the JSON object to a string
    const jsonString = JSON.stringify(timeoutData)

    // Use browser's btoa function to encode the string in Base64
    // This matches the Java implementation which uses Base64.getEncoder().encodeToString()
    await page.evaluate((jsonStr) => {
      const base64Value = btoa(jsonStr)
      document.cookie = `session_timeout_data=${base64Value}; path=/;`
    }, jsonString)
  }

  test.describe('applicant UI', () => {
    test.beforeEach(async ({page}) => {
      await enableFeatureFlag(page, 'session_replay_protection_enabled')
    })

    test('shows inactivity warning modal', async ({page}) => {
      await loginAsTestUser(page)
      await page.goto('/programs')

      await setTimeoutCookie(page, {
        inactivityWarning: 0,
        inactivityTimeout: 60,
        totalWarning: 3600,
        totalTimeout: 7200,
      })

      await page.waitForSelector('#session-inactivity-warning-modal')
      await validateScreenshot(page, 'session-inactivity-warning-modal')

      await expect(
        page.locator('#session-inactivity-warning-heading'),
      ).toContainText('Warning')
      await expect(
        page.locator('#session-inactivity-description'),
      ).toContainText('inactive')

      await page.click('button:text("Extend")')
      await expect(page.locator('.usa-alert--success')).toContainText(
        'Session extended successfully',
      )
      await validateScreenshot(page, 'session-extended-success')
    })

    test('shows session length warning modal', async ({page}) => {
      await loginAsTestUser(page)
      await page.goto('/programs')

      await setTimeoutCookie(page, {
        inactivityWarning: 3600,
        inactivityTimeout: 7200,
        totalWarning: 0,
        totalTimeout: 60,
      })

      await page.waitForSelector('#session-length-warning-modal')
      await validateScreenshot(page, 'session-length-warning-modal')

      await expect(
        page.locator('#session-length-warning-heading'),
      ).toContainText('Session Length Warning')
      await expect(page.locator('#session-length-description')).toContainText(
        'too long',
      )
    })

    test('redirects to logout on session timeout', async ({page}) => {
      await loginAsTestUser(page)
      await page.goto('/programs')

      await setTimeoutCookie(page, {
        inactivityWarning: -60,
        inactivityTimeout: 0,
        totalWarning: 3600,
        totalTimeout: 7200,
      })

      await expect(page).toHaveURL('/logout')
    })

    test('handles timeout warnings', async ({page}) => {
      await loginAsTestUser(page)
      await page.goto('/programs')

      await setTimeoutCookie(page, {
        inactivityWarning: 0,
        inactivityTimeout: 60,
        totalWarning: 0,
        totalTimeout: 60,
      })

      await page.waitForSelector(
        [
          '#session-inactivity-warning-modal',
          '#session-length-warning-modal',
        ].join(','),
      )

      const inactivityModal = page.locator('#session-inactivity-warning-modal')
      const lengthModal = page.locator('#session-length-warning-modal')

      const [inactivityVisible, lengthVisible] = await Promise.all([
        inactivityModal.isVisible(),
        lengthModal.isVisible(),
      ])

      expect(inactivityVisible || lengthVisible).toBeTruthy()
      expect(inactivityVisible && lengthVisible).toBeFalsy()

      await validateScreenshot(page, 'session-warning-modal')
    })
  })

  test.describe('admin UI', () => {
    test.beforeEach(async ({page}) => {
      await enableFeatureFlag(page, 'session_replay_protection_enabled')
    })

    test('shows inactivity warning modal for admin', async ({page}) => {
      await loginAsAdmin(page)
      await page.goto('/admin/programs')

      await setTimeoutCookie(page, {
        inactivityWarning: 0,
        inactivityTimeout: 60,
        totalWarning: 3600,
        totalTimeout: 7200,
      })

      await page.waitForSelector('#session-inactivity-warning-modal')
      await validateScreenshot(page, 'admin-session-inactivity-warning-modal')

      await expect(
        page.locator('#session-inactivity-warning-heading'),
      ).toContainText('Warning')
      await page.click('button:text("Extend")')
      await expect(page.locator('.usa-alert--success')).toContainText(
        'Session extended successfully',
      )
    })

    test('redirects admin to logout on session timeout', async ({page}) => {
      await loginAsAdmin(page)
      await page.goto('/admin/programs')

      await setTimeoutCookie(page, {
        inactivityWarning: -60,
        inactivityTimeout: 0,
        totalWarning: 3600,
        totalTimeout: 7200,
      })

      await expect(page).toHaveURL('/logout')
    })
  })

  test.describe('with NorthStar UI', {tag: ['@northstar']}, () => {
    test.beforeEach(async ({page}) => {
      await enableFeatureFlag(page, 'north_star_applicant_ui')
      await enableFeatureFlag(page, 'session_replay_protection_enabled')
    })

    test('shows inactivity warning modal with NorthStar UI', async ({page}) => {
      await loginAsTestUser(page)
      await page.goto('/programs')

      await setTimeoutCookie(page, {
        inactivityWarning: 0,
        inactivityTimeout: 60,
        totalWarning: 3600,
        totalTimeout: 7200,
      })

      await page.waitForSelector('#session-inactivity-warning-modal')
      await validateScreenshot(
        page,
        'northstar-session-inactivity-warning-modal',
      )

      await expect(
        page.locator('#session-inactivity-warning-heading'),
      ).toContainText('Warning')
      await page.click('button:text("Extend")')
      await expect(page.locator('.usa-alert--success')).toContainText(
        'Session extended successfully',
      )
    })

    test('shows session length warning modal with NorthStar UI', async ({
      page,
    }) => {
      await loginAsTestUser(page)
      await page.goto('/programs')

      await setTimeoutCookie(page, {
        inactivityWarning: 3600,
        inactivityTimeout: 7200,
        totalWarning: 0,
        totalTimeout: 60,
      })

      await page.waitForSelector('#session-length-warning-modal')
      await validateScreenshot(page, 'northstar-session-length-warning-modal')

      await expect(
        page.locator('#session-length-warning-heading'),
      ).toContainText('Session Length Warning')
    })
  })
})
