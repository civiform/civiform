import {test, expect} from './support/civiform_fixtures'
import {enableFeatureFlag, loginAsAdmin, loginAsTestUser} from './support'
import {SessionTimeout} from './support/session_timeout'

test.describe('Session timeout warnings for Admin', () => {
  test.beforeEach(async ({page}) => {
    await enableFeatureFlag(page, 'session_timeout_enabled')
    await loginAsAdmin(page)
  })

  test('shows inactivity warning modal, extends session, show session length warning modal and logout', async ({
    page,
  }) => {
    const sessionTimeout = new SessionTimeout(page)
    await sessionTimeout.verifyTimeoutModalBehavior(
      'admin-session-inactivity-warning-modal',
      'admin-session-length-warning-modal',
    )
    await sessionTimeout.verifyAdminSessionLengthDialogLogout()
  })

  test('dismisses session length warning modal when Cancel button is clicked', async ({
    page,
  }) => {
    const sessionTimeout = new SessionTimeout(page)
    await sessionTimeout.verifyModalDismissal()

    await test.step('Advance to timeout and verify logout', async () => {
      await sessionTimeout.advanceToSessionTimeout()
      await page.waitForURL((url) => url.pathname === '/programs')
      await expect(page.locator('a[href="/applicantLogin"]')).toHaveText(
        'Log in',
      )
    })
  })
})

// Regular Applicant UI
test.describe('Session timeout warnings for Applicants', () => {
  test.beforeEach(async ({page}) => {
    await enableFeatureFlag(page, 'session_timeout_enabled')
    await loginAsTestUser(page)
  })

  test('shows inactivity warning and extends session modal', async ({page}) => {
    const sessionTimeout = new SessionTimeout(page)
    await sessionTimeout.verifyTimeoutModalBehavior()
    await sessionTimeout.verifySessionLengthDialogLogout()
  })

  test('dismisses warning modals when Cancel is clicked and logs out automatically at timeout', async ({
    page,
  }) => {
    const sessionTimeout = new SessionTimeout(page)
    await sessionTimeout.verifyModalDismissal()
    await sessionTimeout.verifyAutoLogout()
  })
})

// NorthStar UI
test.describe(
  'Session timeout warnings with NorthStar UI',
  {tag: ['@northstar']},
  () => {
    test.beforeEach(async ({page}) => {
      await enableFeatureFlag(page, 'north_star_applicant_ui')
      await enableFeatureFlag(page, 'session_timeout_enabled')
      await loginAsTestUser(page)
    })

    test('shows inactivity warning and extends session modal', async ({
      page,
    }) => {
      const sessionTimeout = new SessionTimeout(page)
      await sessionTimeout.verifyTimeoutModalBehavior()
      await sessionTimeout.verifySessionLengthDialogLogout()
    })

    test('dismisses warning modals when Cancel is clicked and logs out automatically at timeout', async ({
      page,
    }) => {
      const sessionTimeout = new SessionTimeout(page)
      await sessionTimeout.verifyModalDismissal()
      await sessionTimeout.verifyAutoLogout()
    })
  },
)
