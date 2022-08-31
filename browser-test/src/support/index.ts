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
import {
  BASE_URL,
  TEST_USER_AUTH_STRATEGY,
  TEST_USER_LOGIN,
  TEST_USER_PASSWORD,
  TEST_USER_DISPLAY_NAME,
  DISABLE_SCREENSHOTS,
} from './config'

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

export const startSession = async (): Promise<{
  browser: Browser
  context: BrowserContext
  page: Page
}> => {
  const browser = await chromium.launch()
  const context = await makeBrowserContext(browser)
  const page = await context.newPage()

  await page.goto(BASE_URL)
  await closeWarningMessage(page)

  return {browser, context, page}
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
  await page.waitForURL('**/loginForm', {waitUntil: 'networkidle'})
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
    case 'fake-oidc':
      await loginAsTestUserFakeOidc(page)
      break
    case 'aws-staging':
      await loginAsTestUserAwsStaging(page)
      break
    case 'seattle-staging':
      await loginAsTestUserSeattleStaging(page)
      break
    default:
      // TODO(clouser): Throw an error for an unrecognized strategy.
      // throw new Error(
      //   `unrecognized TEST_USER_AUTH_STRATEGY "${TEST_USER_AUTH_STRATEGY}"`,
      // )
      await loginAsGuest(page)
  }
  await waitForPageJsLoad(page)
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
    page.waitForURL('**/u/login/*', {waitUntil: 'networkidle'}),
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
    // TODO(clouser): Throw an error once this is in place.
    // throw new Error('TEST_USER_DISPLAY_NAME environment variable must be set')
    return 'Guest'
  }
  return TEST_USER_DISPLAY_NAME
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
