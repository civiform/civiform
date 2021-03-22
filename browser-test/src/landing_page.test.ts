import { chromium } from 'playwright'

it('should work', async () => {
  let browser = await chromium.launch()
  let page = await browser.newPage()

  await page.goto('http://civiform:9000')

  expect(await page.textContent('html')).toContain('continue as guest')
})
