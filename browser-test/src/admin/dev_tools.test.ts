import {test, expect} from '../support/civiform_fixtures'
import {
  enableFeatureFlag,
  validateAccessibility,
  validateScreenshot,
} from '../support'

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
      expect(await page.innerText('h1')).toContain('Dev tools')
      await validateScreenshot(page, 'dev-tools-modal')
    })
  })
  test('not functional when disable demo mode logins is enabled', async ({
    page,
  }) => {
    await enableFeatureFlag(page, 'staging_disable_demo_mode_logins')

    const header = page.locator('nav')

    await test.step('link not shown in the header', async () => {
      await expect(header.getByText('DevTools')).not.toBeInViewport()
      await validateAccessibility(page)
    })

    await test.step('navigating to dev tools URL unsuccessful', async () => {
      await page.goto(`/dev/seed`)
      expect(page.url()).toEqual('/')
      expect(await page.innerText('h1')).not.toContain('Dev tools')
    })

    await test.step('navigating to clear URL unsuccessful', async () => {
      await page.goto(`/dev/seed/clear`)
      expect(page.url()).toEqual('/')
      expect(await page.innerText('h1')).not.toContain('Dev tools')
    })
  })
})
