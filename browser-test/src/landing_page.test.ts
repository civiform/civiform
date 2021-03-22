import { chromium } from 'playwright'

it('should work', async () => {
  let browser = await chromium.launch()
  let page = await browser.newPage()

  await page.goto('http://civiform:9000')

  console.log(await page.textContent('html'))
})
