import { Browser, chromium, Page } from 'playwright'
import { waitForPageJsLoad } from './wait'
export { AdminApiKeys } from './admin_api_keys'
export { AdminQuestions } from './admin_questions'
export { AdminPredicates } from './admin_predicates'
export { AdminPrograms } from './admin_programs'
export { AdminTranslations } from './admin_translations'
export { AdminTIGroups } from './admin_ti_groups'
export { ApplicantQuestions } from './applicant_questions'
export { clickAndWaitForModal, waitForPageJsLoad } from './wait'
import { BASE_URL, TEST_USER_LOGIN, TEST_USER_PASSWORD } from './config'
export { BASE_URL, TEST_USER_LOGIN, TEST_USER_PASSWORD }

export const isLocalDevEnvironment = () => {
  return (
    BASE_URL === 'http://civiform:9000' || BASE_URL === 'http://localhost:9999'
  )
}

function makeBrowserContext(browser: Browser) {
  if (process.env.RECORD_VIDEO) {
    // https://playwright.dev/docs/videos
    // Docs state that videos are only saved upon
    // closing the returned context. In practice,
    // this doesn't appear to be true. Restructuring
    // to ensure that we always close the returned
    // context is possible, but likely not necessary
    // until it causes a problem. In practice, this
    // will only be used when debugging failures.
    const suffix = (global as any)['expect'] === undefined
      ? '' : expect.getState().currentTestName
    return browser.newContext({
      acceptDownloads: true,
      recordVideo: {
        dir: `tmp/videos/${suffix}/`,
      },
    })
  } else {
    return browser.newContext({
      acceptDownloads: true,
    })
  }
}

export const startSession = async () => {
  const browser = await chromium.launch()
  const context = await makeBrowserContext(browser)
  const page = await context.newPage()

  await page.goto(BASE_URL)

  return { browser, context, page }
}

export const endSession = async (browser: Browser) => {
  await browser.close()
}

// Logs out the user if they are logged in and goes to the site landing page.
export const resetSession = async (page: Page) => {
  const logoutText = await page.$('text=Logout')
  if (logoutText !== null) {
    await logout(page)
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

export const loginAsGuest = async (page: Page) => {
  try{ 
    await page.click('#guest') 
  } catch(NASTY_ERROR) { 
    await page.pause() 
    await page.click('#guest') 
  }
  await waitForPageJsLoad(page)
}

export const loginAsTestUser = async (page: Page) => {
  if (isTestUser()) {
    await page.click('#idcs')
    // Wait for the IDCS login page to make sure we've followed all redirects.
    await page.waitForURL('**/#/login*')
    await page.fill('input[name=userName]', TEST_USER_LOGIN)
    await page.fill('input[name=password]', TEST_USER_PASSWORD)
    await page.click('button:has-text("Login"):not([disabled])')
    await page.waitForNavigation({ waitUntil: 'networkidle' })
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
  assertProgramIndexPage = false
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
