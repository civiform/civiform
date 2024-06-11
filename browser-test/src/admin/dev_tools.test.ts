import {test, expect} from '../support/civiform_fixtures'
import {validateAccessibility, validateScreenshot} from '../support'

test.describe('developer tools', () => {
  test('dev link exists', async ({page}) => {
    const header = page.locator('nav')

    await test.step('link shown in the header', async () => {
      await expect(header.getByText('DevTools')).toBeInViewport()
      await validateScreenshot(header, 'dev-tools-in-header')
      await validateAccessibility(page)
    })

    await test.step('modal appears on click', async () => {
      await header.getByText('DevTools').click()
      await validateScreenshot(page, 'dev-tools-modal')
    })
  })
})
