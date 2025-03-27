import {Page} from 'playwright'
import {validateToastMessage, validateScreenshot} from '.'
import {test, expect} from './civiform_fixtures'

type TimeoutData = {
  inactivityWarning: number
  inactivityTimeout: number
  totalWarning: number
  totalTimeout: number
  currentTime: number
}

export class SessionTimeout {
  private page: Page

  constructor(page: Page) {
    this.page = page
  }

  /**
   * Verifies the timeout modal behavior including showing warning and extending session
   */
  async verifyTimeoutModalBehavior(
    inactivityScreenshotFileName?: string,
    lengthScreenshotFileName?: string,
  ) {
    await test.step('Show and verify inactivity warning', async () => {
      await this.advanceToSessionInactivityWarning()
      await expect(
        this.page.locator('#session-inactivity-warning-modal div.usa-modal'),
      ).toBeVisible()
      if (inactivityScreenshotFileName) {
        await validateScreenshot(
          this.page.locator('#session-inactivity-warning-modal div.usa-modal'),
          'admin-session-inactivity-warning-modal',
        )
      }
      await expect(
        this.page.locator('#session-inactivity-warning-heading'),
      ).toContainText('Warning')
      await expect(
        this.page.locator('#session-inactivity-description'),
      ).toContainText(
        'Your session will expire soon due to inactivity. Would you like to extend your session?',
      )
    })

    await test.step('Extend session and verify', async () => {
      await this.page.click('button:text("Extend")')
      await expect(this.page.locator('#toast-container')).toBeVisible()
      await validateToastMessage(this.page, 'Session successfully extended')
      await expect(
        this.page.locator('#session-inactivity-warning-modal div.usa-modal'),
      ).toBeHidden()
    })
    await test.step('Show and verify session length warning', async () => {
      await this.advanceToSessionLengthWarning()
      await expect(
        this.page.locator('#session-length-warning-modal div.usa-modal'),
      ).toBeVisible()
      if (lengthScreenshotFileName) {
        await validateScreenshot(
          this.page.locator('#session-length-warning-modal div.usa-modal'),
          'admin-session-length-warning-modal',
        )
      }
    })
  }

  /**
   * Verifies modal dismissal behavior for both warning types
   */
  async verifyModalDismissal() {
    await test.step('Show and dismiss inactivity warning', async () => {
      await this.advanceToSessionInactivityWarning()
      await expect(
        this.page.locator('#session-inactivity-warning-modal div.usa-modal'),
      ).toBeVisible()
      await this.page.click(
        '#session-inactivity-warning-modal button:text("Cancel")',
      )
      await expect(
        this.page.locator('#session-inactivity-warning-modal div.usa-modal'),
      ).toBeHidden()
    })

    await test.step('Show and dismiss session length warning', async () => {
      await this.advanceToSessionLengthWarning()
      await expect(
        this.page.locator('#session-length-warning-modal div.usa-modal'),
      ).toBeVisible()
      await this.page.click(
        '#session-length-warning-modal button:text("Cancel")',
      )
      await expect(
        this.page.locator('#session-length-warning-modal div.usa-modal'),
      ).toBeHidden()
    })
  }

  /**
   * Advance the session to the timeout threshold and verify that the user is logged out.
   */
  async verifyAutoLogout() {
    await test.step('Advance to timeout and verify logout', async () => {
      await this.advanceToSessionTimeout()
      await this.page.waitForURL((url) => url.href.includes('logout'))
    })
  }

  /**
   * Verify that the session length warning modal appears and the user is logged out on logout button click.
   */
  async verifySessionLengthDialogLogout() {
    await test.step('verify logout from session length warning dialog', async () => {
      await this.page.click(
        '#session-length-warning-modal button:text("Logout")',
      )
      await this.page.waitForURL((url) => url.href.includes('logout'))
    })
  }

  /**
   * Verify that the session length warning modal appears for Admins and the user is logged out on logout button click.
   */
  async verifyAdminSessionLengthDialogLogout() {
    await test.step('verify logout from session length warning dialog', async () => {
      await this.page.click(
        '#session-length-warning-modal button:text("Logout")',
      )
      await this.page.waitForURL((url) => url.pathname === '/programs')
    })
  }

  /**
   * Advances time to inactivity warning threshold
   */
  async advanceToSessionInactivityWarning() {
    return this.advanceToSessionTimeout('inactivityWarning')
  }

  /**
   * Advances time to session length warning threshold
   */
  async advanceToSessionLengthWarning() {
    return this.advanceToSessionTimeout('totalWarning')
  }

  /**
   * Advances time to timeout threshold
   */
  async advanceToSessionTimeout(
    timeoutType:
      | 'inactivityWarning'
      | 'totalWarning'
      | 'totalTimeout' = 'totalTimeout',
  ) {
    const data = await this.getAdjustedTimeoutData()
    if (!data) {
      throw new Error('No timeout data found')
    }

    const browserNow = await this.page.evaluate(() =>
      Math.floor(Date.now() / 1000),
    )
    const targetTime = data[timeoutType]
    const advanceMs = (targetTime - browserNow + 2) * 1000

    await this.advanceTimeWithClockAPI(advanceMs)
  }

  /**
   * Retrieves and adjusts the timeout data from the cookie with clock skew calculation
   * @returns TimeoutData or null if cookie not found
   */
  private async getAdjustedTimeoutData(): Promise<TimeoutData | null> {
    return this.page.evaluate(() => {
      const cookie = document.cookie
        .split('; ')
        .find((row) => row.startsWith('session_timeout_data='))
      if (!cookie) return null

      try {
        const value = cookie.split('=')[1]
        const decoded = atob(decodeURIComponent(value))
        const data: TimeoutData = JSON.parse(decoded) as TimeoutData

        // Calculate clock skew between client and server (same as in SessionTimeoutHandler)
        const clientNow = Math.floor(Date.now() / 1000)
        const clockSkew = clientNow - data.currentTime

        // Return adjusted data
        return {
          inactivityWarning: data.inactivityWarning + clockSkew,
          inactivityTimeout: data.inactivityTimeout + clockSkew,
          totalWarning: data.totalWarning + clockSkew,
          totalTimeout: data.totalTimeout + clockSkew,
          currentTime: data.currentTime,
        }
      } catch (e) {
        console.error('Failed to parse timeout cookie:', e)
        return null
      }
    })
  }

  /**
   * Advances time using Playwright's Clock API
   * @param advanceMilliseconds Amount of time to advance in milliseconds
   */
  private async advanceTimeWithClockAPI(advanceMilliseconds: number) {
    // Get the current time before advancing
    const currentTime = await this.page.evaluate(() => Date.now())

    // Install the clock with the current time
    await this.page.clock.install({time: currentTime})

    // Calculate the target time
    const targetTime = currentTime + advanceMilliseconds

    // Pause at the target time
    await this.page.clock.pauseAt(targetTime)

    // Run for a small amount of time to ensure all timers are processed
    await this.page.clock.runFor(100)

    // Dispatch a custom event to notify the application that time has changed
    // This helps trigger any listeners that might be waiting for time changes
    await this.page.evaluate((ms) => {
      const timeChangeEvent = new CustomEvent('timechange', {
        detail: {
          advancedBy: ms,
          newTime: Date.now(),
        },
      })
      window.dispatchEvent(timeChangeEvent)
    }, advanceMilliseconds)

    // Give the browser a moment to process the event
    await this.page.waitForTimeout(100)
  }
}
