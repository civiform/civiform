import { chromium, Page } from 'playwright'
export { AdminQuestions } from './admin_questions'

export const startSession = async () => {
  const browser = await chromium.launch()
  const page = await browser.newPage()

  await page.goto('http://civiform:9000')

  return { browser, page }
}

export const logout = async (page: Page) => page.goto('/logout')

export const loginAsAdmin = async (page: Page) => {
  await page.goto('http://civiform:9000/loginForm')
  await page.screenshot({ path: 'tmp/screenshot.png', fullPage: true });
  await page.click('#admin')
}
