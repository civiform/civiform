import {test, expect} from './support/civiform_fixtures'
import {
  loginAsTestUser,
  validateScreenshot,
  validateAccessibility,
  logout,
  enableFeatureFlag,
} from './support'

test.describe('Header', () => {
  /**
   * @todo (#4360) add a "Not logged in, guest mode disabled" test once we can get to the programs page without logging in, for an entity without guest mode.
   */
  test('Check screenshots and validate accessibility', async ({page}) => {
    await test.step('Take a screenshot as a guest', async () => {
      await logout(page)
      await validateScreenshot(
        page.getByRole('navigation'),
        'not-logged-in-guest-mode-enabled',
      )
    })

    await test.step('Take a screenshot as the test user', async () => {
      await loginAsTestUser(page)
      await validateScreenshot(page.getByRole('navigation'), 'logged-in')
    })

    await test.step('Passes accessibility test', async () => {
      await validateAccessibility(page)
    })
  })

  test('Government banner', async ({page}) => {
    const usaBannerLocator = page.getByRole('banner').locator('.usa-banner')
    const usaBannerContentLocator = usaBannerLocator.locator(
      '.usa-banner__content',
    )
    const usaBannerButtonLocator = usaBannerLocator.getByRole('button', {
      name: "Here's how you know",
    })

    await test.step('Page loads with the banner visible and collapsed', async () => {
      await expect(usaBannerLocator).toContainText(
        'This is an official government website.',
      )
      await expect(usaBannerContentLocator).toBeHidden()
    })

    await test.step('Clicking the button expands the banner', async () => {
      await usaBannerButtonLocator.click()
      await expect(usaBannerContentLocator).toBeVisible()
      await validateScreenshot(page.getByRole('navigation'), 'banner-expanded')
    })

    await test.step('Clicking the button again collapses the banner', async () => {
      await usaBannerButtonLocator.click()
      await expect(usaBannerContentLocator).toBeHidden()
    })
  })

  test(
    'Government banner with north star enabled',
    {tag: ['@northstar']},
    async ({page}) => {
      await enableFeatureFlag(page, 'north_star_applicant_ui')
      const usaBannerLocator = page.getByTestId('governmentBanner')
      const usaBannerContentLocator = usaBannerLocator.locator(
        '.usa-banner__content',
      )
      const usaBannerButtonLocator = usaBannerLocator.getByRole('button', {
        name: "Here's how you know",
      })

      await test.step('Page loads with the banner visible and collapsed', async () => {
        await expect(usaBannerLocator).toContainText(
          'An official website of the United States government',
        )
        await expect(usaBannerContentLocator).toBeHidden()
      })

      await test.step('Clicking the button expands the banner', async () => {
        await usaBannerButtonLocator.click()
        await expect(usaBannerContentLocator).toBeVisible()
        await validateScreenshot(usaBannerLocator, 'banner-expanded-north-star')
      })

      await test.step('Clicking the button again collapses the banner', async () => {
        await usaBannerButtonLocator.click()
        await expect(usaBannerContentLocator).toBeHidden()
      })
    },
  )

  test(
    'Header on desktop with north star enabled shows logo',
    {tag: ['@northstar']},
    async ({page}) => {
      await enableFeatureFlag(page, 'north_star_applicant_ui')

      await page.setViewportSize({width: 1280, height: 720})

      const headerLogo = page.locator('.cf-header-logo')
      await expect(headerLogo).toBeVisible()
    },
  )

  test(
    'Header on tablet with north star enabled shows logo',
    {tag: ['@northstar']},
    async ({page}) => {
      await enableFeatureFlag(page, 'north_star_applicant_ui')

      await page.setViewportSize({width: 800, height: 1024})

      const headerLogo = page.locator('.cf-header-logo')
      await expect(headerLogo).toBeVisible()
    },
  )

  test(
    'Header on mobile with north star enabled hides logo',
    {tag: ['@northstar']},
    async ({page}) => {
      await enableFeatureFlag(page, 'north_star_applicant_ui')

      await page.setViewportSize({width: 360, height: 800})

      const headerLogo = page.locator('.cf-header-logo')
      await expect(headerLogo).toBeHidden()
    },
  )
})
