import {test, expect} from './support/civiform_fixtures'
import {
  enableFeatureFlag,
  loginAsAdmin,
  loginAsTestUser,
  validateScreenshot,
  validateToastMessage,
} from './support'
import {waitForHtmxReady} from './support/wait'
import {
  setupSessionTimeout,
  verifyTimeoutModalBehavior,
  verifyModalDismissal,
  verifyAutoLogout,
  advanceToSessionInactivityWarning,
  advanceToSessionLengthWarning,
  advanceToSessionTimeout,
} from './support/session_timeout'

test.describe('Session timeout warnings for Admin', () => {
  test.beforeEach(async ({page, adminSettings}) => {
    await setupSessionTimeout({page, adminSettings})
    await loginAsAdmin(page)
    await page.goto('/admin/programs')
    await page.waitForLoadState('networkidle')
  })

  test('shows inactivity warning modal, extends session, show session length warning modal and logout', async ({
    page,
  }) => {
    await test.step('Show and verify inactivity warning', async () => {
      await advanceToSessionInactivityWarning(page)
      await page.waitForSelector(
        '#session-inactivity-warning-modal div.usa-modal',
        {state: 'visible', timeout: 20000},
      )

      await validateScreenshot(
        page.locator('#session-inactivity-warning-modal div.usa-modal'),
        'admin-session-inactivity-warning-modal',
      )

      await expect(
        page.locator('#session-inactivity-warning-heading'),
      ).toContainText('Warning')
      await expect(
        page.locator('#session-inactivity-description'),
      ).toContainText(
        'Your session will expire soon due to inactivity. Would you like to extend your session?',
      )
    })

    await test.step('Extend session and verify', async () => {
      await page.click('button:text("Extend")')
      await waitForHtmxReady(page)
      await validateToastMessage(page, 'Session successfully extended')

      await expect(
        page.locator('#session-inactivity-warning-modal div.usa-modal'),
      ).toBeHidden()
    })

    await test.step('Show and verify session length warning', async () => {
      await advanceToSessionLengthWarning(page)
      await page.waitForSelector(
        '#session-length-warning-modal div.usa-modal',
        {
          state: 'visible',
          timeout: 20000,
        },
      )

      await validateScreenshot(
        page.locator('#session-length-warning-modal div.usa-modal'),
        'admin-session-length-warning-modal',
      )
    })

    await test.step('Logout and verify', async () => {
      await page.click('#session-length-warning-modal button:text("Logout")')
      await page.waitForURL((url) => url.pathname === '/programs')
      await expect(page.locator('a[href="/applicantLogin"]')).toHaveText(
        'Log in',
      )
    })
  })

  test('dismisses session length warning modal when Cancel button is clicked', async ({
    page,
  }) => {
    await test.step('Show and dismiss inactivity warning', async () => {
      await advanceToSessionInactivityWarning(page)
      await page.waitForSelector(
        '#session-inactivity-warning-modal div.usa-modal',
        {state: 'visible', timeout: 20000},
      )
      await page.click(
        '#session-inactivity-warning-modal button:text("Cancel")',
      )
    })

    await test.step('Show and dismiss session length warning', async () => {
      await advanceToSessionLengthWarning(page)
      await page.waitForSelector(
        '#session-length-warning-modal div.usa-modal',
        {
          state: 'visible',
          timeout: 20000,
        },
      )
      await page.click('#session-length-warning-modal button:text("Cancel")')
      await expect(
        page.locator('#session-length-warning-modal div.usa-modal'),
      ).toBeHidden()
    })
    await test.step('Advance to timeout and verify logout', async () => {
      await advanceToSessionTimeout(page)
      await page.waitForURL((url) => url.pathname === '/programs')
      await expect(page.locator('a[href="/applicantLogin"]')).toHaveText(
        'Log in',
      )
    })
  })
})

// Applicant UI tests
test.describe('Session timeout warnings for Applicants', () => {
  test.beforeEach(async ({page, adminSettings}) => {
    await setupSessionTimeout({page, adminSettings})
    await loginAsTestUser(page)
    await page.goto('/programs')
    await page.waitForLoadState('networkidle')
  })

  test('shows inactivity warning modal and extends session', async ({page}) => {
    await verifyTimeoutModalBehavior(page)
  })

  test('dismisses warning modals when Cancel is clicked and logs out automatically at timeout', async ({
    page,
  }) => {
    await verifyModalDismissal(page)
    await verifyAutoLogout(page)
  })
})

// NorthStar UI tests
test.describe('with NorthStar UI', {tag: ['@northstar']}, () => {
  test.beforeEach(async ({page, adminSettings}) => {
    await enableFeatureFlag(page, 'north_star_applicant_ui')
    await setupSessionTimeout({page, adminSettings})
    await loginAsTestUser(page)
    await page.goto('/programs')
    await page.waitForLoadState('networkidle')
  })

  test('shows inactivity warning modal and extends session', async ({page}) => {
    await verifyTimeoutModalBehavior(page)
  })

  test('dismisses warning modals when Cancel is clicked and logs out automatically at timeout', async ({
    page,
  }) => {
    await verifyModalDismissal(page)
    await verifyAutoLogout(page)
  })
})
