import axe = require('axe-core')
import {
  Browser,
  BrowserContext,
  chromium,
  Page,
  PageScreenshotOptions,
} from 'playwright'
import * as path from 'path'
import {MatchImageSnapshotOptions} from 'jest-image-snapshot'
import {waitForPageJsLoad} from './wait'
import {
  BASE_URL,
  DISABLE_SCREENSHOTS,
  TEST_USER_LOGIN,
  TEST_USER_PASSWORD,
} from './config'
import {AdminQuestions} from './admin_questions'
import {AdminPrograms} from './admin_programs'

export {AdminApiKeys} from './admin_api_keys'
export {AdminQuestions} from './admin_questions'
export {AdminPredicates} from './admin_predicates'
export {AdminPrograms} from './admin_programs'
export {AdminProgramStatuses} from './admin_program_statuses'
export {AdminTranslations} from './admin_translations'
export {AdminTIGroups} from './admin_ti_groups'
export {ClientInformation, TIDashboard} from './ti_dashboard'
export {ApplicantQuestions} from './applicant_questions'
export {NotFoundPage} from './error_pages'
export {clickAndWaitForModal, dismissModal, waitForPageJsLoad} from './wait'

export const isLocalDevEnvironment = () => {
  return (
    BASE_URL === 'http://civiform:9000' || BASE_URL === 'http://localhost:9999'
  )
}

function makeBrowserContext(browser: Browser): Promise<BrowserContext> {
  if (process.env.RECORD_VIDEO) {
    // https://playwright.dev/docs/videos
    // Docs state that videos are only saved upon
    // closing the returned context. In practice,
    // this doesn't appear to be true. Restructuring
    // to ensure that we always close the returned
    // context is possible, but likely not necessary
    // until it causes a problem. In practice, this
    // will only be used when debugging failures.
    const dirs = ['tmp/videos']
    if ('expect' in global) {
      const testPath = expect.getState().testPath
      const testFile = testPath.substring(testPath.lastIndexOf('/') + 1)
      dirs.push(testFile)
      // Some test initialize context in beforeAll at which point test name is
      // not set.
      if (expect.getState().currentTestName) {
        dirs.push(expect.getState().currentTestName)
      }
    }
    return browser.newContext({
      acceptDownloads: true,
      recordVideo: {
        dir: path.join(...dirs),
      },
    })
  } else {
    return browser.newContext({
      acceptDownloads: true,
    })
  }
}

export const startSession = async (
  browser: Browser | null = null,
): Promise<{
  browser: Browser
  context: BrowserContext
  page: Page
}> => {
  if (browser == null) {
    browser = await chromium.launch()
  }
  const context = await makeBrowserContext(browser)
  const page = await context.newPage()

  await dropTables(page)
  await page.goto(BASE_URL)
  await closeWarningMessage(page)

  return {browser, context, page}
}

/**
 * Object containing properties and methods for interacting with browser and
 * app. See docs for createBrowserContext() method for more info.
 */
export interface TestContext {
  /**
   * Playwright Page object. Provides functionality to directly interact with
   * the browser .
   * Methods: https://playwright.dev/docs/api/class-page
   */
  page: Page

  adminQuestions: AdminQuestions
  adminPrograms: AdminPrograms
}

/**
 * Launches a browser and returns context that contains objects needed to
 * interact with the browser. Example usage:
 *
 * describe('some test', () => {
 *   const ctx = createBrowserContext()
 *
 *   it('should do foo', async () => {
 *     await ctx.page.click('#some-button')
 *   })
 * })
 *
 * Browser session is reset between tests and database is cleared by default.
 * Each test starts on the login page.
 *
 * Context object should be accessed only from within it(), before/afterEach(),
 * before/afterAll() functions.
 *
 * @param clearDb Whether database is cleared between tests. True by default.
 *     It's recommended that database is cleared between tests to keep tests
 *     hermetic.
 * @return object containing browser page. Context object is reset between tests
 *     so none of its properties should be cached and reused between tests.
 */
export const createTestContext = (clearDb = true): TestContext => {
  let browser: Browser
  let browserContext: BrowserContext

  // TestContext properties are set in resetContext() later. For now we just
  // need an object that we can return to caller. Caller is expected to access
  // it only from before/afterX functions or tests.
  const ctx: TestContext = {} as unknown as TestContext

  // We create new browser context and session before each test. It's
  // important to get fresh browser context so that each test gets its own
  // videos. If we reuse same browser context - we'll get one huge video for
  // all tests.
  async function resetContext() {
    if (browserContext != null) {
      await browserContext.close()
    }
    browserContext = await makeBrowserContext(browser)
    ctx.page = await browserContext.newPage()
    ctx.adminQuestions = new AdminQuestions(ctx.page)
    ctx.adminPrograms = new AdminPrograms(ctx.page)
    await ctx.page.goto(BASE_URL)
  }

  beforeAll(async () => {
    browser = await chromium.launch()
    await resetContext()
  })

  beforeEach(async () => {
    await resetContext()
  })

  afterEach(async () => {
    if (clearDb) {
      await dropTables(ctx.page)
    }
    // resetting context here so that afterAll() functions of current describe()
    // block and beforeAll() functions of the next describe() block have fresh
    // result.page object.
    await resetContext()
  })

  afterAll(async () => {
    await endSession(browser)
  })

  return ctx
}

export const endSession = async (browser: Browser) => {
  await browser.close()
}

/**
 *  Logs out the user if they are logged in and goes to the site landing page.
 * @param clearDb When set to true clears all data from DB as part of starting
 *     session. Should be used in new tests to ensure that test cases are
 *     hermetic and order-independent.
 */
export const resetSession = async (page: Page, clearDb = false) => {
  const logoutText = await page.$('text=Logout')
  if (logoutText !== null) {
    await logout(page)
  }
  if (clearDb) {
    await dropTables(page)
  }
  await page.goto(BASE_URL)
}

export const gotoEndpoint = async (page: Page, endpoint: string) => {
  return await page.goto(BASE_URL + endpoint)
}

export const logout = async (page: Page) => {
  await page.click('text=Logout')
  // Logout is handled by the play framework so it doesn't land on a
  // page with civiform js where we should waitForPageJsLoad. Because
  // the process goes through a sequence of redirects we need to wait
  // for the final destination URL to make tests reliable.
  await page.waitForURL('**/loginForm')
}

export const loginAsAdmin = async (page: Page) => {
  await page.click('#admin')
  await waitForPageJsLoad(page)
}

export const loginAsProgramAdmin = async (page: Page) => {
  await page.click('#program-admin')
  await waitForPageJsLoad(page)
}

export const loginAsTrustedIntermediary = async (page: Page) => {
  await page.click('#trusted-intermediary')
  await waitForPageJsLoad(page)
}

export const loginAsGuest = async (page: Page) => {
  await page.click('#guest')
  await waitForPageJsLoad(page)
}

export const setLangEsUS = async (page: Page) => {
  await page.click('text=Español')
  await page.click('text=Submit')
}

export const loginAsTestUser = async (page: Page) => {
  if (isTestUser()) {
    await page.click('#idcs')
    // Wait for the IDCS login page to make sure we've followed all redirects.
    // If running this against a site with a real IDCS (i.e. staging) and this
    // test fails with a timeout try re-running the tests. Sometimes there are
    // just transient network hiccups that will pass on a second run.
    // In short: If using a real IDCS retry test if this has a timeout failure.
    await page.waitForURL('**/#/login*')
    await page.fill('input[name=userName]', TEST_USER_LOGIN)
    await page.fill('input[name=password]', TEST_USER_PASSWORD)
    await page.click('button:has-text("Login"):not([disabled])')
    await page.waitForNavigation({waitUntil: 'networkidle'})
  } else {
    await page.click('#guest')
  }
  await waitForPageJsLoad(page)
}

function isTestUser() {
  return TEST_USER_LOGIN !== '' && TEST_USER_PASSWORD !== ''
}

export const userDisplayName = () => {
  if (isTestUser()) {
    return 'TEST, UATAPP'
  } else {
    return 'Guest'
  }
}

/**
 * The option to select a language is only shown once for a given applicant. If this is
 * the first time they see this page, select the given language. Otherwise continue.
 */
export const selectApplicantLanguage = async (
  page: Page,
  language: string,
  assertProgramIndexPage = false,
) => {
  const infoPageRegex = /applicants\/\d+\/edit/
  const maybeSelectLanguagePage = await page.url()
  if (maybeSelectLanguagePage.match(infoPageRegex)) {
    const languageOption = `.cf-radio-option:has-text("${language}")`
    await page.click(languageOption + ' input')
    await page.click('button:visible')
  }
  await waitForPageJsLoad(page)

  if (assertProgramIndexPage) {
    const programIndexRegex = /applicants\/\d+\/programs/
    const maybeProgramIndexPage = await page.url()
    expect(maybeProgramIndexPage).toMatch(programIndexRegex)
  }
}

export const dropTables = async (page: Page) => {
  await page.goto(BASE_URL + '/dev/seed')
  await page.click('#clear')
}

export const seedCanonicalQuestions = async (page: Page) => {
  await page.goto(BASE_URL + '/dev/seed')
  await page.click('#canonical-questions')
}

export const enableFeatureFlag = async (page: Page, flag: string) => {
  await page.goto(BASE_URL + `/dev/feature/${flag}/enable`)
}

export const closeWarningMessage = async (page: Page) => {
  // The warning message may be in the way of this link
  const element = await page.$('#warning-message-dismiss')

  if (element !== null) {
    await element
      .click()
      .catch(() =>
        console.log(
          "Didn't find a warning toast message to dismiss, which is fine.",
        ),
      )
  }
}

export const validateAccessibility = async (page: Page) => {
  // Inject axe and run accessibility test.
  await page.addScriptTag({path: 'node_modules/axe-core/axe.min.js'})
  const results = await page.evaluate(() => {
    return axe.run()
  })

  expect(results).toHaveNoA11yViolations()
}

/**
 * Saves a screenshot to a file such as
 * browser-test/image_snapshots/test_file_name/{screenshotFileName}-snap.png.
 * If the screenshot already exists, compare the new screenshot with the
 * existing screenshot, and save a pixel diff instead if the two don't match.
 * @param screenshotFileName Must use dash-separated-case for consistency.
 */
export const validateScreenshot = async (
  page: Page,
  screenshotFileName: string,
  pageScreenshotOptions?: PageScreenshotOptions,
  matchImageSnapshotOptions?: MatchImageSnapshotOptions,
) => {
  // Do not make image snapshots when running locally
  if (DISABLE_SCREENSHOTS) {
    return
  }
  expect(screenshotFileName).toMatch(/[a-z0-9-]+/)
  expect(
    await page.screenshot({
      ...pageScreenshotOptions,
    }),
  ).toMatchImageSnapshot({
    allowSizeMismatch: true,
    // threshold is 1% it's pretty wide but there is some noise that we can't
    // explain
    failureThreshold: 0.01,
    failureThresholdType: 'percent',
    customSnapshotsDir: 'image_snapshots',
    customDiffDir: 'diff_output',
    customSnapshotIdentifier: ({testPath}) => {
      const dir = path.basename(testPath).replace('.test.ts', '_test')
      return `${dir}/${screenshotFileName}`
    },
    ...matchImageSnapshotOptions,
  })
}
