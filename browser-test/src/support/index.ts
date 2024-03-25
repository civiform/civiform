import {test, expect} from '@playwright/test'
import {AxeBuilder} from '@axe-core/playwright'
import {
  Browser,
  BrowserContext,
  chromium,
  Frame,
  Page,
  Locator,
  devices,
  BrowserContextOptions,
} from 'playwright'
import * as path from 'path'
import {waitForPageJsLoad} from './wait'
import {
  BASE_URL,
  LOCALSTACK_URL,
  TEST_USER_AUTH_STRATEGY,
  DISABLE_SCREENSHOTS,
  TEST_USER_LOGIN,
  TEST_USER_PASSWORD,
  TEST_USER_DISPLAY_NAME,
  DISABLE_BROWSER_ERROR_WATCHER,
} from './config'
import {AdminQuestions} from './admin_questions'
import {AdminPrograms} from './admin_programs'
import {AdminApiKeys} from './admin_api_keys'
import {AdminProgramStatuses} from './admin_program_statuses'
import {ApplicantFileQuestion} from './applicant_file_question'
import {ApplicantQuestions} from './applicant_questions'
import {AdminPredicates} from './admin_predicates'
import {AdminTranslations} from './admin_translations'
import {AdminProgramImage} from './admin_program_image'
import {TIDashboard} from './ti_dashboard'
import {AdminTIGroups} from './admin_ti_groups'
import {BrowserErrorWatcher} from './browser_error_watcher'

export {AdminApiKeys} from './admin_api_keys'
export {AdminQuestions} from './admin_questions'
export {AdminPredicates} from './admin_predicates'
export {AdminPrograms} from './admin_programs'
export {AdminProgramStatuses} from './admin_program_statuses'
export {AdminSettings} from './admin_settings'
export {AdminTranslations} from './admin_translations'
export {AdminProgramImage} from './admin_program_image'
export {AdminTIGroups} from './admin_ti_groups'
export {ApplicantFileQuestion} from './applicant_file_question'
export {ApplicantQuestions} from './applicant_questions'
export {ClientInformation, TIDashboard} from './ti_dashboard'
export {NotFoundPage} from './error_pages'
export {clickAndWaitForModal, dismissModal, waitForPageJsLoad} from './wait'

export const isLocalDevEnvironment = () => {
  return (
    BASE_URL === 'http://civiform:9000' || BASE_URL === 'http://localhost:9999'
  )
}

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

/** True when the test environment is hermetic i.e. not a durable staging deployment. */
export const isHermeticTestEnvironment = () =>
  TEST_USER_AUTH_STRATEGY === AuthStrategy.FAKE_OIDC

function makeBrowserContext(
  browser: Browser,
  useMobile = false,
): Promise<BrowserContext> {
  const contextOptions: BrowserContextOptions = {
    ...(useMobile ? devices['Pixel 5'] : {}),
    acceptDownloads: true,
  }

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
      const testPath = test.info().file

      if (testPath == null) {
        throw new Error('testPath cannot be null')
      }

      const testFile = testPath.substring(testPath.lastIndexOf('/') + 1)
      dirs.push(testFile)

      // Some test initialize context in beforeAll at which point test name is
      // not set.

      const testName = test.info().title

      if (testName) {
        // remove special characters
        dirs.push(testName.replaceAll(/[:"<>|*?]/g, ''))
      }
    }

    contextOptions.recordVideo = {
      dir: path.join(...dirs),
    }
  }

  return browser.newContext(contextOptions)
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
  browser: Browser
  browserContext: BrowserContext
  browserErrorWatcher: BrowserErrorWatcher
  useMobile: boolean

  adminQuestions: AdminQuestions
  adminPrograms: AdminPrograms
  adminApiKeys: AdminApiKeys
  adminProgramStatuses: AdminProgramStatuses
  applicantFileQuestion: ApplicantFileQuestion
  applicantQuestions: ApplicantQuestions
  adminPredicates: AdminPredicates
  adminTranslations: AdminTranslations
  adminProgramImage: AdminProgramImage
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
 *   test('should do foo', async () => {
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
  // TestContext properties are set in resetContext() later. For now we just
  // need an object that we can return to caller. Caller is expected to access
  // it only from before/afterX functions or tests.
  const ctx: TestContext = {} as unknown as TestContext

  test.beforeAll(async () => {
    ctx.browser = await chromium.launch()
    await resetContext(ctx)
    // clear DB at beginning of each test suite. While data can leak/share
    // between test cases within a test file, data should not be shared
    // between test files.
    await dropTables(ctx.page)
    await ctx.page.goto(BASE_URL)
  })

  test.beforeEach(async () => {
    await resetContext(ctx)
  })

  test.afterEach(async () => {
    if (clearDb) {
      await dropTables(ctx.page)
    }
    // resetting context here so that afterAll() functions of current describe()
    // block and beforeAll() functions of the next describe() block have fresh
    // result.page object.
    await resetContext(ctx)
  })

  test.afterAll(async () => {
    await endSession(ctx.browser)
  })

  return ctx
}

// We create new browser context and session before each test. It's
// important to get fresh browser context so that each test gets its own
// video file. If we reuse same browser context across multiple test cases -
// we'll get one huge video for all tests.
export async function resetContext(ctx: TestContext) {
  if (ctx.browserContext != null) {
    try {
      if (!DISABLE_BROWSER_ERROR_WATCHER) {
        ctx.browserErrorWatcher.failIfContainsErrors()
      }
    } finally {
      // browserErrorWatcher might throw an error that should bubble up all
      // the way to the developer. Regardless whether the error is thrown or
      // not we need to close the browser context. Without that some processes
      // won't be finished, like saving videos.
      await ctx.browserContext.close()
    }
  }
  ctx.browserContext = await makeBrowserContext(ctx.browser, ctx.useMobile)
  ctx.page = await ctx.browserContext.newPage()
  ctx.browserErrorWatcher = new BrowserErrorWatcher(ctx.page)
  // Default timeout is 30s. It's too long given that civiform is not JS
  // heavy and all elements render quite quickly. Setting it to 5 sec so that
  // tests fail fast.
  ctx.page.setDefaultTimeout(8000)
  ctx.adminQuestions = new AdminQuestions(ctx.page)
  ctx.adminPrograms = new AdminPrograms(ctx.page)
  ctx.adminApiKeys = new AdminApiKeys(ctx.page)
  ctx.adminProgramStatuses = new AdminProgramStatuses(ctx.page)
  ctx.applicantQuestions = new ApplicantQuestions(ctx.page)
  ctx.adminPredicates = new AdminPredicates(ctx.page)
  ctx.adminTranslations = new AdminTranslations(ctx.page)
  ctx.adminProgramImage = new AdminProgramImage(ctx.page)
  ctx.applicantFileQuestion = new ApplicantFileQuestion(ctx.page)
  ctx.tiDashboard = new TIDashboard(ctx.page)
  ctx.adminTiGroups = new AdminTIGroups(ctx.page)
  await ctx.page.goto(BASE_URL)
  await closeWarningMessage(ctx.page)
}

export const endSession = async (browser: Browser) => {
  await browser.close()
}

export const gotoEndpoint = async (page: Page, endpoint = '') => {
  return await page.goto(BASE_URL + endpoint)
}

export const dismissToast = async (page: Page) => {
  await page.locator('#toast-container div:text("x")').click()
  await waitForPageJsLoad(page)
}

export const logout = async (page: Page, closeToast = true) => {
  await page.click('#logout-button')
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

  // Logout is handled by the play framework so it doesn't land on a
  // page with civiform js where we should waitForPageJsLoad. Because
  // the process goes through a sequence of redirects we need to wait
  // for the final destination URL (the programs index page), to make tests reliable.
  await page.waitForURL('**/programs')
  await validateToastMessage(page, 'Your session has ended.')
  if (closeToast) await dismissToast(page)
}

export const loginAsAdmin = async (page: Page) => {
  await test.step('Login as Civiform Admin', async () => {
    await page.click('#debug-content-modal-button')
    await page.click('#admin')
    await waitForPageJsLoad(page)
  })
}

export const loginAsProgramAdmin = async (page: Page) => {
  await test.step('Login as Program Admin', async () => {
    await page.click('#debug-content-modal-button')
    await page.click('#program-admin')
    await waitForPageJsLoad(page)
  })
}

export const loginAsCiviformAndProgramAdmin = async (page: Page) => {
  await test.step('Login as Civiform and Program Admin', async () => {
    await page.click('#debug-content-modal-button')
    await page.click('#dual-admin')
    await waitForPageJsLoad(page)
  })
}

export const loginAsTrustedIntermediary = async (page: Page) => {
  await test.step('Login as Trusted Intermediary', async () => {
    await page.click('#debug-content-modal-button')
    await page.click('#trusted-intermediary')
    await waitForPageJsLoad(page)
  })
}

/**
 * Logs in via an auth provider.
 * @param loginButton Selector of a button on current page that starts auth
 *     login. Normally it's "Log in" button on main page, but in some cases
 *     login can be initiated from different pages, for example after program
 *     submission.
 */
export const loginAsTestUser = async (
  page: Page,
  loginButton = 'a:has-text("Log in")',
  isTi = false,
  displayName: string = '',
) => {
  await test.step('Login as Test User', async () => {
    switch (TEST_USER_AUTH_STRATEGY) {
      case AuthStrategy.FAKE_OIDC:
        await loginAsTestUserFakeOidc(page, loginButton, isTi)
        break
      case AuthStrategy.AWS_STAGING:
        await loginAsTestUserAwsStaging(page, loginButton, isTi)
        break
      case AuthStrategy.SEATTLE_STAGING:
        await loginAsTestUserSeattleStaging(page, loginButton)
        break
      default:
        throw new Error(
          `Unrecognized or unset TEST_USER_AUTH_STRATEGY environment variable of '${TEST_USER_AUTH_STRATEGY}'`,
        )
    }
    await waitForPageJsLoad(page)
    if (displayName === '') {
      displayName = testUserDisplayName()
    }
    await page.waitForSelector(`:has-text("Logged in as ${displayName}")`)
  })
}

async function loginAsTestUserSeattleStaging(page: Page, loginButton: string) {
  await page.click(loginButton)
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

async function loginAsTestUserAwsStaging(
  page: Page,
  loginButton: string,
  isTi: boolean,
) {
  await Promise.all([
    page.waitForURL('**/u/login*', {waitUntil: 'networkidle'}),
    page.click(loginButton),
  ])

  await page.fill('input[name=username]', TEST_USER_LOGIN)
  await page.fill('input[name=password]', TEST_USER_PASSWORD)
  await Promise.all([
    page.waitForURL(isTi ? '**/admin/**' : /.*\/programs.*/, {
      waitUntil: 'networkidle',
    }),
    // Auth0 has an additional hidden "Continue" button that does nothing for some reason
    page.click('button:visible:has-text("Continue")'),
  ])
}

async function loginAsTestUserFakeOidc(
  page: Page,
  loginButton: string,
  isTi: boolean,
) {
  await Promise.all([
    page.waitForURL('**/interaction/*', {waitUntil: 'networkidle'}),
    page.click(loginButton),
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
    throw new Error(
      'Unexpected reauthorization page. Central logout should fully logout user.',
    )
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
    page.waitForURL(isTi ? '**/admin/**' : /\/programs.*/, {
      waitUntil: 'networkidle',
    }),
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
 * The option to select a language is shown in the header bar as a dropdown. This helper method selects the given language from the dropdown.
 */
export const selectApplicantLanguage = async (page: Page, language: string) => {
  await page.click('#select-language')
  await page.selectOption('#select-language', {label: language})

  await waitForPageJsLoad(page)
}

export const dropTables = async (page: Page) => {
  await page.goto(BASE_URL + '/dev/seed')
  await page.click('#clear')
}

export const seedQuestions = async (page: Page) => {
  await page.goto(BASE_URL + '/dev/seed')
  await page.click('#sample-questions')
}

export const seedPrograms = async (page: Page) => {
  await page.goto(BASE_URL + '/dev/seed')
  await page.click('#sample-programs')
}

export const disableFeatureFlag = async (page: Page, flag: string) => {
  await test.step(`Disable feature flag: ${flag}`, async () => {
    await page.goto(`/dev/feature/${flag}/disable`)
  })
}

export const enableFeatureFlag = async (page: Page, flag: string) => {
  await test.step(`Enable feature flag: ${flag}`, async () => {
    await page.goto(`/dev/feature/${flag}/enable`)
  })
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
  await test.step('Validate accessiblity', async () => {
    const results = await new AxeBuilder({page}).analyze()
    const errorMessage = `Found ${results.violations.length} axe accessibility violations:\n ${JSON.stringify(
      results.violations,
      null,
      2,
    )}`

    expect(results.violations, errorMessage).toEqual([])
  })
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
  fullPage?: boolean,
  mobileScreenshot?: boolean,
) => {
  if (fullPage === undefined) {
    fullPage = true
  }

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

  if (fullPage) {
    // Some tests take screenshots while scroll position in the middle. That
    // affects header which is position fixed and on final full-page screenshots
    // overlaps part of the page.
    await page.evaluate(() => {
      window.scrollTo(0, 0)
    })
  }

  expect(screenshotFileName).toMatch(/^[a-z0-9-]+$/)

  await takeScreenshot(element, `${screenshotFileName}`, fullPage)

  const existingWidth = page.viewportSize()?.width || 1280

  if (mobileScreenshot) {
    const height = page.viewportSize()?.height || 720
    // Update the viewport size to different screen widths so we can test on a
    // variety of sizes
    await page.setViewportSize({width: 320, height})

    await takeScreenshot(element, `${screenshotFileName}-mobile`, fullPage)

    // Medium width
    await page.setViewportSize({width: 800, height})

    await takeScreenshot(element, `${screenshotFileName}-medium`, fullPage)

    // Reset back to original width
    await page.setViewportSize({width: existingWidth, height})
  }
}

const takeScreenshot = async (
  element: Page | Locator,
  fullScreenshotFileName: string,
  fullPage?: boolean,
) => {
  const testFileName = path
    .basename(test.info().file)
    .replace('.test.ts', '_test')

  expect(
    await element.screenshot({
      fullPage: fullPage,
      animations: 'disabled',
    }),
  ).toMatchSnapshot([testFileName, fullScreenshotFileName + '.png'])
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
    const replacements: {[selector: string]: (text: string) => string} = {
      '.cf-bt-date': (text) =>
        text
          .replace(/\d{4}\/\d{2}\/\d{2}/, '2030/01/01')
          .replace(/\d{4}-\d{2}-\d{2}/, '2030-01-01')
          .replace(/^(\d{1,2}\/\d{1,2}\/\d{2})$/, '1/1/30')
          .replace(/\d{1,2}:\d{2} (AM|PM) [A-Z]{2,3}/, '11:22 PM PDT'),
      '.cf-application-id': (text) => text.replace(/\d+/, '1234'),
      '.cf-bt-email': () => 'fake-email@example.com',
      '.cf-bt-api-key-id': (text) => text.replace(/ID: .*/, 'ID: ####'),
      '.cf-bt-api-key-created-by': (text) =>
        text.replace(/Created by .*/, 'Created by fake-admin-12345'),
    }
    for (const [selector, replacement] of Object.entries(replacements)) {
      for (const element of Array.from(document.querySelectorAll(selector))) {
        if (
          selector == '.cf-bt-email' &&
          element.textContent == '(no email address)'
        ) {
          continue
        } else {
          element.textContent = replacement(element.textContent!)
        }
      }
    }
  })
}

export const validateToastMessage = async (page: Page, value: string) => {
  const toastMessages = await page.innerText('#toast-container')
  expect(toastMessages).toContain(value)
}

type LocalstackSesResponse = {
  messages: LocalstackSesEmail[]
}
type LocalstackSesEmail = {
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
  await page.goto(`${LOCALSTACK_URL}/_aws/ses`)
  const responseJson = JSON.parse(
    await page.innerText('body'),
  ) as LocalstackSesResponse

  const allEmails = responseJson.messages
  const filteredEmails = allEmails.filter((email) => {
    return email.Destination.ToAddresses.includes(recipientEmail)
  })

  await page.goto(originalPageUrl)
  return filteredEmails
}

export const expectEnabled = async (page: Page, locator: string) => {
  expect(await page.getAttribute(locator, 'disabled')).toBeNull()
}

export const expectDisabled = async (page: Page, locator: string) => {
  expect(await page.getAttribute(locator, 'disabled')).not.toBeNull()
}
