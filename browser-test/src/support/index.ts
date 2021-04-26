import { Browser, chromium, Page } from 'playwright'
export { AdminQuestions } from './admin_questions'
export { AdminPrograms } from './admin_programs'
export { AdminTranslations } from './admin_translations'
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

export const gotoEndpoint = async (page: Page, endpoint: string) => {
  return await page.goto(BASE_URL + endpoint);
}

export const logout = async (page: Page) => {
  await page.click('text=Logout');
}

export const loginAsAdmin = async (page: Page) => {
  await page.click('#admin');
}

export const loginAsGuest = async (page: Page) => {
  await page.click('#guest');
}

export const selectApplicantLanguage = async (page: Page, language: string) => {
  await page.selectOption('select', { label: language });
  await page.click('button');
}

export const dropTables = async (page: Page) => {
  await page.goto(BASE_URL + '/dev/seed');
  await page.click("#clear");
}
