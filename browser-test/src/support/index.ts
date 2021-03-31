import { chromium, Page } from 'playwright'
export { AdminQuestions } from './admin_questions'

const { BASE_URL } = process.env

export const startSession = async () => {
  const browser = await chromium.launch()
  const page = await browser.newPage()

  await page.goto(BASE_URL)

  return { browser, page }
}

export const logout = async (page: Page) => page.goto('/logout')

export const loginAsAdmin = async (page: Page) => {
  await page.goto(BASE_URL + '/loginForm')
  await page.screenshot({ path: 'tmp/screenshot.png', fullPage: true });
  await page.click('#admin')
}
