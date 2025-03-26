import {Page} from 'playwright'
import {AdminSettings, loginAsAdmin, logout, validateToastMessage} from '.'
import {waitForHtmxReady} from './wait'
import {test, expect} from './civiform_fixtures'

type TimeoutData = {
  inactivityWarning: number
  inactivityTimeout: number
  totalWarning: number
  totalTimeout: number
  currentTime: number
}

type TimeoutType = 'inactivityWarning' | 'totalWarning' | 'totalTimeout'

/**
 * Gets the adjusted timeout data that accounts for clock skew
 * This matches what the SessionTimeoutHandler uses internally
 */
async function getAdjustedTimeoutData(page: Page): Promise<TimeoutData | null> {
  return page.evaluate(() => {
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
 * Helper function to mock the Date object and advance time
 */
async function mockDateAndAdvanceTime(page: Page, advanceMilliseconds: number) {
  await page.evaluate((ms) => {
    // Store the original Date constructor
    const OriginalDate = Date
    const originalNow = Date.now

    // Create a mock date that will be ahead by the specified milliseconds
    const mockTimeOffset = ms

    const realNow = originalNow.call(Date)
    const mockedNow = realNow + mockTimeOffset

    console.log(
      `Mocking time: Real time: ${realNow / 1000}, Mocked time: ${mockedNow / 1000}, Offset: ${mockTimeOffset / 1000}`,
    )

    // Override the Date constructor
    // @ts-expect-error - Intentionally mocking Date constructor
    window.Date = class extends OriginalDate {
      constructor(...args: ConstructorParameters<typeof OriginalDate>) {
        if (args.length > 0) {
          // If arguments provided, behave normally
          super(...args)
        } else {
          // When called with no arguments, return current time + offset
          super(realNow + mockTimeOffset)
        }
      }

      // Override getTime to return the advanced time
      getTime() {
        return realNow + mockTimeOffset
      }

      // Override now to return the advanced time
      static now() {
        return realNow + mockTimeOffset
      }
    }

    // Dispatch timechange event with time details. Simply advancing time does not trigger the timeout
    // that is already set up in the browser
    const timeChangeEvent = new CustomEvent('timechange', {
      detail: {
        realTime: realNow,
        mockedTime: mockedNow,
        offset: mockTimeOffset,
      },
    })

    console.log(
      'Dispatching timechange event with details:',
      timeChangeEvent.detail,
    )
    window.dispatchEvent(timeChangeEvent)
  }, advanceMilliseconds)
  // Give the browser a moment to process the event
  await page.waitForTimeout(100)
}

/**
 * Advances time to trigger a specific timeout event
 */
async function advanceToTimeout(page: Page, timeoutType: TimeoutType) {
  // Get the adjusted timeout data that accounts for clock skew
  const data = await getAdjustedTimeoutData(page)
  if (!data) {
    throw new Error('No timeout data found')
  }

  // Get current browser time
  const browserNow = await page.evaluate(() => Math.floor(Date.now() / 1000))

  // Get target time based on timeout type
  const targetTime = data[timeoutType]
  console.log(`timeoutType: ${timeoutType}, timeoutValue: ${targetTime}`)
  // Add small buffer (2 seconds) to ensure we're past the threshold
  const advanceMs = (targetTime - browserNow + 2) * 1000

  console.log(`Advancing time by ${advanceMs}ms to trigger ${timeoutType}`)
  await mockDateAndAdvanceTime(page, advanceMs)
}

export async function advanceToSessionInactivityWarning(page: Page) {
  return advanceToTimeout(page, 'inactivityWarning')
}

export async function advanceToSessionLengthWarning(page: Page) {
  return advanceToTimeout(page, 'totalWarning')
}

export async function advanceToSessionTimeout(page: Page) {
  return advanceToTimeout(page, 'totalTimeout')
}

export async function setupSessionTimeout({
  page,
  adminSettings,
}: {
  page: Page
  adminSettings: AdminSettings
}) {
  page.on('console', (msg) => {
    const type = msg.type()
    const text = msg.text()
    console.log(`Browser ${type}: ${text}`)
  })

  page.on('pageerror', (error) => {
    console.error('Browser error:', error)
  })

  await loginAsAdmin(page)
  await adminSettings.gotoAdminSettings()
  await adminSettings.enableSetting('SESSION_TIMEOUT_ENABLED')
  await adminSettings.saveChanges()
  await logout(page)
}

// Shared test functions
export async function verifyTimeoutModalBehavior(page: Page) {
  await test.step('Show and verify inactivity warning', async () => {
    await advanceToSessionInactivityWarning(page)
    await page.waitForSelector(
      '#session-inactivity-warning-modal div.usa-modal',
      {state: 'visible', timeout: 20000},
    )

    await expect(
      page.locator('#session-inactivity-warning-heading'),
    ).toContainText('Warning')
    await expect(page.locator('#session-inactivity-description')).toContainText(
      'Your session will expire soon due to inactivity. Would you like to extend your session?',
    )
  })

  await test.step('Extend session and verify', async () => {
    await page.click('button:text("Extend")')
    await waitForHtmxReady(page)
    await validateToastMessage(page, 'Session successfully extended')
  })
}

export async function verifyModalDismissal(page: Page) {
  await test.step('Show and dismiss inactivity warning', async () => {
    await advanceToSessionInactivityWarning(page)
    await page.waitForSelector(
      '#session-inactivity-warning-modal div.usa-modal',
      {state: 'visible', timeout: 20000},
    )
    await page.click('#session-inactivity-warning-modal button:text("Cancel")')
    await expect(
      page.locator('#session-inactivity-warning-modal div.usa-modal'),
    ).toBeHidden()
  })

  await test.step('Show and dismiss session length warning', async () => {
    await advanceToSessionLengthWarning(page)
    await page.waitForSelector('#session-length-warning-modal div.usa-modal', {
      state: 'visible',
      timeout: 20000,
    })
    await page.click('#session-length-warning-modal button:text("Cancel")')
    await expect(
      page.locator('#session-length-warning-modal div.usa-modal'),
    ).toBeHidden()
  })
}

export async function verifyAutoLogout(page: Page) {
  await test.step('Advance to timeout and verify logout', async () => {
    await advanceToSessionTimeout(page)
    await page.waitForURL((url) => url.href.includes('logout'))
  })
}
