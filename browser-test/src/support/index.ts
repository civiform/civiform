import axe = require('axe-core')
import {
  Browser,
  BrowserContext,
  chromium,
  Frame,
  Page,
  PageScreenshotOptions,
  LocatorScreenshotOptions,
  Locator,
} from 'playwright'
import * as path from 'path'
import {MatchImageSnapshotOptions} from 'jest-image-snapshot'
import {waitForPageJsLoad} from './wait'
import {
  BASE_URL,
  LOCALSTACK_URL,
  TEST_USER_AUTH_STRATEGY,
  DISABLE_SCREENSHOTS,
  TEST_USER_LOGIN,
  TEST_USER_PASSWORD,
  TEST_USER_DISPLAY_NAME,
} from './config'
import {AdminQuestions} from './admin_questions'
import {AdminPrograms} from './admin_programs'
import {AdminApiKeys} from './admin_api_keys'
import {AdminProgramStatuses} from './admin_program_statuses'
import {ApplicantQuestions} from './applicant_questions'
import {AdminPredicates} from './admin_predicates'
import {AdminTranslations} from './admin_translations'
import {TIDashboard} from './ti_dashboard'
import {AdminTIGroups} from './admin_ti_groups'

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

/* eslint-disable no-unused-vars */
/**
 * Different auth strategies that are being exercised in this test. Each strategy
 * requires different logic for login (which fields to fill and button to click on
 * login page) and logout (some logout flows require confirmation).
 */
export enum AuthStrategy {
  FAKE_OIDC = 'fake-oidc',
  AWS_STAGING = 'aws-staging',
  SEATTLE_STAGING = 'seattle-staging',
}
/* eslint-enable no-unused-vars */

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
    if ('expect' in global && expect.getState() != null) {
      const testPath = expect.getState().testPath
      const testFile = testPath.substring(testPath.lastIndexOf('/') + 1)
      dirs.push(testFile)
      // Some test initialize context in beforeAll at which point test name is
      // not set.
      if (expect.getState().currentTestName) {
        // remove special characters
        dirs.push(expect.getState().currentTestName.replace(/[:"<>|*?]/g, ''))
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

  await page.goto(BASE_URL)
  await closeWarningMessage(page)

  return {browser, context, page}
}

/**
 * Object containing properties and methods for interacting with browser and
 * app. See docs for createTestContext() method for more info.
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
  adminApiKeys: AdminApiKeys
  adminProgramStatuses: AdminProgramStatuses
  applicantQuestions: ApplicantQuestions
  adminPredicates: AdminPredicates
  adminTranslations: AdminTranslations
  tiDashboard: TIDashboard
  adminTiGroups: AdminTIGroups
}

/**
 * Launches a browser and returns context that contains objects needed to
 * interact with the browser. It should be called at the very beginning of the
 * top-most describe() and reused across all other describe/it functions.
 * Example usage:
 *
 * ```
 * describe('some test', () => {
 *   const ctx = createTestContext()
 *
 *   it('should do foo', async () => {
 *     await ctx.page.click('#some-button')
 *   })
 * })
 * ```
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
  // video file. If we reuse same browser context across multiple test cases -
  // we'll get one huge video for all tests.
  async function resetContext() {
    if (browserContext != null) {
      await browserContext.close()
    }
    browserContext = await makeBrowserContext(browser)
    ctx.page = await browserContext.newPage()
    // Default timeout is 30s. It's too long given that civiform is not JS
    // heavy and all elements render quite quickly. Setting it to 5 sec so that
    // tests fail fast.
    ctx.page.setDefaultTimeout(5000)
    ctx.adminQuestions = new AdminQuestions(ctx.page)
    ctx.adminPrograms = new AdminPrograms(ctx.page)
    ctx.adminApiKeys = new AdminApiKeys(ctx.page)
    ctx.adminProgramStatuses = new AdminProgramStatuses(ctx.page)
    ctx.applicantQuestions = new ApplicantQuestions(ctx.page)
    ctx.adminPredicates = new AdminPredicates(ctx.page)
    ctx.adminTranslations = new AdminTranslations(ctx.page)
    ctx.tiDashboard = new TIDashboard(ctx.page)
    ctx.adminTiGroups = new AdminTIGroups(ctx.page)
    await ctx.page.goto(BASE_URL)
    await closeWarningMessage(ctx.page)
  }

  beforeAll(async () => {
    browser = await chromium.launch()
    await resetContext()
    // clear DB at beginning of each test suite. While data can leak/share
    // between test cases within a test file, data should not be shared
    // between test files.
    await dropTables(ctx.page)
    await ctx.page.goto(BASE_URL)
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

export const gotoEndpoint = async (page: Page, endpoint: string) => {
  return await page.goto(BASE_URL + endpoint)
}

export const logout = async (page: Page) => {
  await page.click('text=Logout')
  // If the user logged in through OIDC previously - during logout they are
  // redirected to fake-idcs:PORT/session/end page. There they need to confirm
  // logout.
  if (page.url().match('fake-idcs.*/session/end')) {
    const pageContent = await page.textContent('html')
    if (pageContent!.includes('Do you want to sign-out from')) {
      // OIDC central provider confirmation page
      await page.click('button:has-text("Yes")')
    }
  }

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
  switch (TEST_USER_AUTH_STRATEGY) {
    case AuthStrategy.FAKE_OIDC:
      await loginAsTestUserFakeOidc(page)
      break
    case AuthStrategy.AWS_STAGING:
      await loginAsTestUserAwsStaging(page)
      break
    case AuthStrategy.SEATTLE_STAGING:
      await loginAsTestUserSeattleStaging(page)
      break
    default:
      throw new Error(
        `Unrecognized or unset TEST_USER_AUTH_STRATEGY environment variable of '${TEST_USER_AUTH_STRATEGY}'`,
      )
  }
  await waitForPageJsLoad(page)
  await page.waitForSelector(
    `:has-text("Logged in as ${testUserDisplayName()}")`,
  )
}

async function loginAsTestUserSeattleStaging(page: Page) {
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
}

async function loginAsTestUserAwsStaging(page: Page) {
  await Promise.all([
    page.waitForURL('**/u/login*', {waitUntil: 'networkidle'}),
    page.click('button:has-text("Log in")'),
  ])

  await page.fill('input[name=username]', TEST_USER_LOGIN)
  await page.fill('input[name=password]', TEST_USER_PASSWORD)
  await Promise.all([
    page.waitForURL('**/applicants/**', {waitUntil: 'networkidle'}),
    page.click('button:has-text("Continue")'),
  ])
}

async function loginAsTestUserFakeOidc(page: Page) {
  await Promise.all([
    page.waitForURL('**/interaction/*', {waitUntil: 'networkidle'}),
    page.click('button:has-text("Log in")'),
  ])

  // If the user has previously signed in to the provider, a prompt is shown
  // to reauthorize rather than sign-in. In this case, click "Continue" instead
  // and skip filling out any login information. If we want to support logging
  // in as multiple users, this will need to be adjusted.
  const pageText = await page.innerText('html')
  if (
    pageText.includes(
      'the client is asking you to confirm previously given authorization',
    )
  ) {
    return Promise.all([
      page.waitForURL('**/applicants/**', {waitUntil: 'networkidle'}),
      page.click('button:has-text("Continue")'),
    ])
  }

  await page.fill('input[name=login]', TEST_USER_LOGIN)
  await page.fill('input[name=password]', TEST_USER_PASSWORD)
  await Promise.all([
    page.waitForURL('**/interaction/*', {waitUntil: 'networkidle'}),
    page.click('button:has-text("Sign-in"):not([disabled])'),
  ])
  // A screen is shown prompting the user to authorize a set of scopes.
  // This screen is skipped if the user has already logged in once.
  await Promise.all([
    page.waitForURL('**/applicants/**', {waitUntil: 'networkidle'}),
    page.click('button:has-text("Continue")'),
  ])
}

export const testUserDisplayName = () => {
  if (!TEST_USER_DISPLAY_NAME) {
    throw new Error(
      'Empty or unset TEST_USER_DISPLAY_NAME environment variable',
    )
  }
  return TEST_USER_DISPLAY_NAME
}

export const supportsEmailInspection = () => {
  return TEST_USER_AUTH_STRATEGY === 'fake-oidc'
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
  const maybeSelectLanguagePage = page.url()
  if (maybeSelectLanguagePage.match(infoPageRegex)) {
    const languageOption = `.cf-radio-option:has-text("${language}")`
    await page.click(languageOption + ' input')
    await page.click('button:visible')
  }
  await waitForPageJsLoad(page)

  if (assertProgramIndexPage) {
    const programIndexRegex = /applicants\/\d+\/programs/
    const maybeProgramIndexPage = page.url()
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
  element: Page | Locator,
  screenshotFileName: string,
  screenshotOptions?: PageScreenshotOptions | LocatorScreenshotOptions,
  matchImageSnapshotOptions?: MatchImageSnapshotOptions,
) => {
  // Do not make image snapshots when running locally
  if (DISABLE_SCREENSHOTS) {
    return
  }
  const page = 'page' in element ? element.page() : element
  // Normalize all variable content so that the screenshot is stable.
  await normalizeElements(page)
  // Also process any sub frames.
  for (const frame of page.frames()) {
    await normalizeElements(frame)
  }

  expect(screenshotFileName).toMatch(/^[a-z0-9-]+$/)
  expect(
    await element.screenshot({
      ...screenshotOptions,
    }),
  ).toMatchImageSnapshot({
    allowSizeMismatch: true,
    failureThreshold: 0,
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

/*
 * Replaces any variable content with static values. This is particularly useful
 * for image diffs.
 *
 * Supports date and time elements with class .cf-bt-date, and applicant IDs
 * with class .cf-application-id
 */
const normalizeElements = async (page: Frame | Page) => {
  await page.evaluate(() => {
    for (const date of Array.from(document.querySelectorAll('.cf-bt-date'))) {
      // Use regexp replacement instead of full replacement to make sure that format of the text
      // matches what we expect. In case underlying format changes to "September 20, 2022" then
      // regexp will break and it will show up in screenshots.
      date.textContent = date
        .textContent!.replace(/\d{4}\/\d{2}\/\d{2}/, '2030/01/01')
        .replace(/^(\d{1,2}\/\d{1,2}\/\d{2})$/, '1/1/30')
        .replace(/\d{1,2}:\d{2} (PM|AM)/, '11:22 PM')
    }
    // Process application id values.
    for (const applicationId of Array.from(
      document.querySelectorAll('.cf-application-id'),
    )) {
      applicationId.textContent = applicationId.textContent!.replace(
        /\d+/,
        '1234',
      )
    }
  })
}
export type LocalstackSesEmail = {
  Body: {
    html_part: string | null
    text_part: string | null
  }
  Destination: {
    ToAddresses: string[]
  }
  Source: string
  Subject: string
}

/**
 * Queries the emails that have been sent for a given recipient. This method requires that tests
 * run in an environment that uses localstack since it captures the emails sent using SES and makes
 * them available at a well-known endpoint). An error is thrown when the method is called from an
 * environment that does not use localstack. The supportsEmailInspection method can be used to
 * determine if the environment supports sending emails.
 */
export const extractEmailsForRecipient = async function (
  page: Page,
  recipientEmail: string,
): Promise<LocalstackSesEmail[]> {
  if (!supportsEmailInspection()) {
    throw new Error('Unsupported call to extractEmailsForRecipient')
  }
  const originalPageUrl = page.url()
  await page.goto(`${LOCALSTACK_URL}/_localstack/ses`)
  const responseJson = JSON.parse(await page.innerText('body')) as any

  const allEmails = responseJson.messages as LocalstackSesEmail[]
  const filteredEmails = allEmails.filter((email) => {
    return email.Destination.ToAddresses.includes(recipientEmail)
  })

  await page.goto(originalPageUrl)
  return filteredEmails
}
