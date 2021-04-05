import { Browser, chromium, Page } from 'playwright'
export { AdminQuestions } from './admin_questions'
export { AdminPrograms } from './admin_programs'

const { BASE_URL = 'http://civiform:9000' } = process.env

export const startSession = async () => {
  const browser = await chromium.launch()
  const page = await browser.newPage()

  await page.goto(BASE_URL)

  return { browser, page }
}

export const endSession = async (browser: Browser) => {
  browser.close()
}

export const logout = async (page: Page) => {
  // TODO: add logout button to applicant page and use that
  page.goto('/logout')
}

export const loginAsAdmin = async (page: Page) => {
  await page.screenshot({ path: 'tmp/screenshot.png', fullPage: true });
  await page.click('#admin')
}
