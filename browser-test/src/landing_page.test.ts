import { chromium } from 'playwright'

it('should work', async () => {
  let browser = await chromium.launch()
  let page = await browser.newPage()
  await page.goto('https://localhost:9999')

  console.log(await page.title())
})
