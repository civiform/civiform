import { chromium } from 'playwright'

it('this is the other page test', async () => {
  let browser = await chromium.launch()
  let page = await browser.newPage()

  await page.goto(process.env.BASE_URL)

  expect(await page.textContent('html')).toContain('continue as guest')
})
