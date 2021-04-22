import { Browser, chromium, Page } from 'playwright'
export { AdminQuestions } from './admin_questions'
export { AdminPrograms } from './admin_programs'
export { AdminTranslations } from './admin_translations'
export { AdminTIGroups } from './admin_ti_groups'
export { ApplicantQuestions } from './applicant_questions'

export const { BASE_URL = 'http://civiform:9000', TEST_USER_LOGIN = '', TEST_USER_PASSWORD = '' } = process.env

var assert = require('assert');

export const startSession = async () => {
  const browser = await chromium.launch();
  const page = await browser.newPage({ acceptDownloads: true });

  await page.goto(BASE_URL);

  return { browser, page };
}

export const endSession = async (browser: Browser) => {
  await browser.close();
}

export const gotoRootUrl = async (page: Page) => {
  await page.goto(BASE_URL);
}

export const gotoEndpoint = async (page: Page, endpoint: string) => {
  return await page.goto(BASE_URL + endpoint);
}

export const logout = async (page: Page) => {
  await page.click('text=Logout');
}

export const loginAsAdmin = async (page: Page) => {
  await page.click('#admin');
}

export const loginAsProgramAdmin = async (page: Page) => {
  await page.click('#program-admin');
}

export const loginAsGuest = async (page: Page) => {
  await page.click('#guest');
}

export const loginAsTestUser = async (page: Page) => {
  if (isTestUser()) {
    await page.click("#idcs");
    await page.fill("#idcs-signin-basic-signin-form-username", TEST_USER_LOGIN);
    await page.fill("#idcs-signin-basic-signin-form-password > input", TEST_USER_PASSWORD);
    await page.click("#idcs-signin-basic-signin-form-submit")
  } else {
    await page.click('#guest');
  }
}

function isTestUser() {
  return TEST_USER_LOGIN !== '' && TEST_USER_PASSWORD !== ''
}


export const userDisplayName = () => {
  if (isTestUser()) {
    return 'TEST, UATAPP'
  } else {
    return '<Anonymous Applicant>'
  }
}

/**
 * The option to select a language is only shown once for a given applicant. If this is
 * the first time they see this page, select the given language. Otherwise continue.
 */
export const selectApplicantLanguage = async (page: Page, language: string) => {
  const infoPageRegex = /applicants\/\d+\/edit/;
  const maybeSelectLanguagePage = await page.url();
  if (maybeSelectLanguagePage.match(infoPageRegex)) {
    const languageOption = `.cf-radio-option:has-text("${language}")`;
    await page.click(languageOption + ' input');
    await page.click('button:visible');
  }

  const programIndexRegex = /applicants\/\d+\/programs/;
  const maybeProgramIndexPage = await page.url();
  expect(maybeProgramIndexPage).toMatch(programIndexRegex);
}

//getMethods = (obj) => Object.getOwnPropertyNames(obj).filter(item => typeof obj[item] === 'function');

export const getUserId = async (page: Page) => {
  let url = page.url();
  await gotoEndpoint(page, '/users/me');
  let user_id = await page.innerText('#applicant-id');

  await page.goto(url);

  return user_id;
}

export const loginWithSimulatedIdcs = async (page: Page) => {
  await page.click('#idcs');

  let pg_source = await page.content();

  if (pg_source.includes("Enter any login")) {
    await page.click('css=[name=login]');
    await page.keyboard.type('username');
    await page.click('css=[name=password]');
    await page.keyboard.type('password');

    await page.click('.login-submit');
  }

  await page.click('.login-submit');
}

export const dropTables = async (page: Page) => {
  await page.goto(BASE_URL + '/dev/seed');
  await page.click("#clear");
}

export const assertEndpointEquals = async (page: Page, endpoint: string) => {
  let url = await page.url();
  let d_url = BASE_URL.concat(endpoint);

  assert.equal(url, d_url);
}

export const assertPageIncludes = async (page: Page, substring: string) => {
  let pg_source = await page.content();
  assert(pg_source.includes(substring));
}

//const getMethods = (obj) => {
//  let properties = new Set()
//  let currentObj = obj
//  do {
//    Object.getOwnPropertyNames(currentObj).map(item => properties.add(item))
//  } while ((currentObj = Object.getPrototypeOf(currentObj)))
//  return [...properties.keys()].filter(item => typeof obj[item] === 'function')
//}
