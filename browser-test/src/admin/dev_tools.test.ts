import {test, expect} from '../fixtures/custom_fixture'
import {validateAccessibility, validateScreenshot} from '../support'

test.describe('developer tools', {tag: ['@migrated']}, () => {
  test('link shown in the header', async ({page}) => {
    const header = page.locator('nav')
    await validateScreenshot(header, 'dev-tools-in-header')

    expect(await page.textContent('nav')).toContain('DevTools')

    await validateAccessibility(page)
  })

  test('modal appears on click', async ({page}) => {
    await page.click('#debug-content-modal-button')
    await validateScreenshot(page, 'dev-tools-modal')
  })
})
