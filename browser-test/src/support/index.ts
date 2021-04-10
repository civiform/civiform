import { Browser, chromium, Page } from 'playwright'
export { AdminQuestions } from './admin_questions'
export { AdminPrograms } from './admin_programs'
export { ApplicantQuestions } from './applicant_questions'

const { BASE_URL = 'http://civiform:9000' } = process.env

export const startSession = async () => {
  const browser = await chromium.launch();
  const page = await browser.newPage({ acceptDownloads: true });

  await page.goto(BASE_URL);

  return { browser, page };
}

export const endSession = async (browser: Browser) => {
  await browser.close();
}

export const logout = async (page: Page) => {
  // TODO: add logout button to applicant page and use that
  await page.goto(BASE_URL + '/logout');
}

export const loginAsAdmin = async (page: Page) => {
  await page.click('#admin');
}

export const loginAsGuest = async (page: Page) => {
  await page.click('#guest');
}

export const dropTables = async (page: Page) => {
  await page.goto(BASE_URL + '/dev/seed');
  await page.click("#clear");
}
