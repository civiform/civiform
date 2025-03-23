import {test, expect} from './support/civiform_fixtures'
import {
  AdminSettings,
  enableFeatureFlag,
  loginAsAdmin,
  loginAsTestUser,
  logout,
  validateScreenshot,
  validateToastMessage,
} from './support'
import {Page} from 'playwright'
import {waitForHtmxReady} from './support/wait'

type TimeoutData = {
  inactivityWarning: number
  inactivityTimeout: number
  totalWarning: number
  totalTimeout: number
  currentTime: number
}

type TimeoutType = 'inactivityWarning' | 'totalWarning' | 'inactivityTimeout'

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
      `Mocking time: Real time: ${new Date(realNow).toISOString()}, Mocked time: ${new Date(mockedNow).toISOString()}`,
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

  // Add small buffer (2 seconds) to ensure we're past the threshold
  const advanceMs = (targetTime - browserNow + 2) * 1000

  console.log(`Advancing time by ${advanceMs}ms to trigger ${timeoutType}`)
  await mockDateAndAdvanceTime(page, advanceMs)
}

// Replace the existing advance methods with the new shared one
async function advanceToSessionInactivityWarning(page: Page) {
  return advanceToTimeout(page, 'inactivityWarning')
}

async function advanceToSessionLengthWarning(page: Page) {
  return advanceToTimeout(page, 'totalWarning')
}

async function advanceToSessionInactivityTimeout(page: Page) {
  return advanceToTimeout(page, 'inactivityTimeout')
}

interface TestOptions {
  page: Page
  uiPrefix?: string
}

/**
 * Shared test steps for session timeout behavior
 */
async function verifySessionTimeoutBehavior({
  page,
  uiPrefix = '',
}: TestOptions) {
  await page.waitForLoadState('networkidle')

  // Test inactivity warning
  await advanceToSessionInactivityWarning(page)
  await page.waitForSelector(
    '#session-inactivity-warning-modal div.usa-modal',
    {
      state: 'visible',
      timeout: 20000,
    },
  )

  await validateScreenshot(page, `${uiPrefix}session-inactivity-warning-modal`)

  await expect(
    page.locator('#session-inactivity-warning-heading'),
  ).toContainText('Warning')
  await expect(page.locator('#session-inactivity-description')).toContainText(
    'Your session will expire soon due to inactivity. Would you like to extend your session?',
  )

  // Test session extension
  await page.click('button:text("Extend")')
  await waitForHtmxReady(page)
  await validateToastMessage(page, 'Session successfully extended')
  await validateScreenshot(page, `${uiPrefix}session-extended-success`)

  // Test length warning
  await advanceToSessionLengthWarning(page)
  await page.waitForSelector('#session-length-warning-modal div.usa-modal', {
    state: 'visible',
    timeout: 20000,
  })
  await validateScreenshot(page, `${uiPrefix}session-length-warning-modal`)
  await page.click('#session-length-warning-modal button:text("Cancel")')
}

async function setupSessionTimeout({
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

test.describe('Session timeout warnings', () => {
  test.beforeEach(async ({page, adminSettings}) => {
    await setupSessionTimeout({page, adminSettings})
  })

  test('shows warning modals and logout for applicants', async ({page}) => {
    await loginAsTestUser(page)
    await page.goto('/programs')

    await verifySessionTimeoutBehavior({page})
    // Test timeout
    await advanceToSessionInactivityTimeout(page)
    await page.waitForURL((url) => url.href.includes('logout'))
  })

  test('shows warning modals and logout for Admins', async ({page}) => {
    await loginAsAdmin(page)
    await page.goto('/admin/programs')
    await verifySessionTimeoutBehavior({page, uiPrefix: 'admin-'})
  })
})

test.describe('with NorthStar UI', {tag: ['@northstar']}, () => {
  test.beforeEach(async ({page, adminSettings}) => {
    await enableFeatureFlag(page, 'north_star_applicant_ui')
    await setupSessionTimeout({page, adminSettings})
  })

  test('shows warning modals and timeout with NorthStar UI', async ({page}) => {
    await loginAsTestUser(page)
    await page.goto('/programs')
    await verifySessionTimeoutBehavior({page, uiPrefix: 'ns-'})
    // Test timeout
    await advanceToSessionInactivityTimeout(page)
    await page.waitForURL((url) => url.href.includes('logout'))
  })
})
