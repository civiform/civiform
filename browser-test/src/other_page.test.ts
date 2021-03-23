import { chromium } from 'playwright'

it('this is the other page test', async () => {
  let browser = await chromium.launch()
  let page = await browser.newPage()

  await page.goto('http://civiform:9000')

  expect(await page.textContent('html')).toContain('continue as guest')
})
