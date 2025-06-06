import {test, expect} from './support/civiform_fixtures'
import {
  enableFeatureFlag,
  loginAsAdmin,
  loginAsTestUser,
  logout,
  selectApplicantLanguageNorthstar,
  validateAccessibility,
  validateScreenshot,
} from './support'

test.describe('Header', {tag: ['@northstar']}, () => {
  test.beforeEach(async ({page, adminPrograms, seeding}) => {
    await enableFeatureFlag(page, 'north_star_applicant_ui')

    // Since a guest account is not created until you start applying for something,
    // we have to make a program.
    await seeding.seedProgramsAndCategories()
    await page.goto('/')
    await loginAsAdmin(page)
    await adminPrograms.publishAllDrafts()
    await logout(page)
  })

  test('Check screenshots and validate accessibility on desktop', async ({
    page,
  }) => {
    await test.step('Take a screenshot as the test user on desktop', async () => {
      await loginAsTestUser(page)
      await validateScreenshot(page.getByRole('banner'), 'logged-in')
    })

    await test.step('Passes accessibility test on desktop', async () => {
      await validateAccessibility(page)
    })
  })

  test('Check screenshots and validate accessibility on mobile', async ({
    page,
  }) => {
    await page.setViewportSize({width: 360, height: 800})

    await test.step('Take a screenshot as the test user on mobile', async () => {
      await page.click('button:has-text("MENU")')
      await loginAsTestUser(page)
      await page.click('button:has-text("MENU")')
      await validateScreenshot(
        page.getByLabel('Primary navigation'),
        'logged-in-mobile',
      )
    })

    await test.step('Passes accessibility test on mobile', async () => {
      await validateAccessibility(page)
    })
  })

  test('Government banner', async ({page}) => {
    const usaBannerLocator = page.getByTestId('governmentBanner')

    const usaBannerContentLocator = usaBannerLocator.locator(
      '.usa-banner__content',
    )
    const usaBannerButtonLocator = usaBannerLocator.locator(
      '.usa-banner__button-text',
    )

    await test.step('Page loads with the banner visible and collapsed', async () => {
      await expect(usaBannerLocator).toContainText(
        'This is an official government website',
      )
      await expect(usaBannerContentLocator).toBeHidden()
    })

    await test.step('Clicking the button expands the banner', async () => {
      await usaBannerButtonLocator.click()
      await expect(usaBannerContentLocator).toBeVisible()
    })

    await test.step('Clicking the button again collapses the banner', async () => {
      await usaBannerButtonLocator.click()
      await expect(usaBannerContentLocator).toBeHidden()
    })

    await test.step('Renders correctly in right to left mode', async () => {
      await selectApplicantLanguageNorthstar(page, 'ar')
      await usaBannerButtonLocator.click()
      await validateScreenshot(
        page.getByRole('complementary'),
        'government-banner-right-to-left',
      )
    })
  })

  test('Government banner with north star enabled', async ({page}) => {
    const usaBannerLocator = page.getByTestId('governmentBanner')
    const usaBannerContentLocator = usaBannerLocator.locator(
      '.usa-banner__content',
    )
    const usaBannerButtonLocator = usaBannerLocator.getByRole('button', {
      name: "Here's how you know",
    })

    await test.step('Page loads with the banner visible and collapsed', async () => {
      await expect(usaBannerLocator).toContainText(
        'This is an official government website',
      )
      await expect(usaBannerContentLocator).toBeHidden()
    })

    await test.step('Clicking the button expands the banner', async () => {
      await usaBannerButtonLocator.click()
      await expect(usaBannerContentLocator).toBeVisible()
    })

    await test.step('Clicking the button again collapses the banner', async () => {
      await usaBannerButtonLocator.click()
      await expect(usaBannerContentLocator).toBeHidden()
    })
  })

  test('Header on desktop with north star enabled shows logo', async ({
    page,
  }) => {
    await page.setViewportSize({width: 1280, height: 720})

    const headerLogo = page.locator('.cf-header-logo')
    await expect(headerLogo).toBeVisible()
  })

  test('Header on tablet with north star enabled hides logo', async ({
    page,
  }) => {
    await page.setViewportSize({width: 800, height: 1024})

    const headerLogo = page.locator('.cf-header-logo')
    await expect(headerLogo).toBeHidden()
  })

  test('Header on mobile with north star enabled hides logo', async ({
    page,
  }) => {
    await page.setViewportSize({width: 360, height: 800})

    const headerLogo = page.locator('.cf-header-logo')
    await expect(headerLogo).toBeHidden()
  })

  test('Government name shown', async ({page}) => {
    const headerText = page.locator('.usa-logo__text')
    await expect(headerText).toHaveText('TestCity CiviForm')
  })

  test('Government name hidden', async ({page}) => {
    await enableFeatureFlag(page, 'hide_civic_entity_name_in_header')

    await test.step('Header on desktop shows logo and hides gov name', async () => {
      await page.setViewportSize({width: 1280, height: 720})

      const headerLogo = page.locator('.cf-header-logo')
      const govName = page.locator('.cf-gov-name')
      await expect(headerLogo).toBeVisible()
      await expect(govName).toBeHidden()
    })

    await test.step('Header on tablet hides logo and shows gov name', async () => {
      await page.setViewportSize({width: 800, height: 1024})

      const headerLogo = page.locator('.cf-header-logo')
      const govName = page.locator('.cf-gov-name')
      await expect(headerLogo).toBeHidden()
      await expect(govName).toBeVisible()
    })

    await test.step('Header on mobile hides logo and shows gov name', async () => {
      await page.setViewportSize({width: 360, height: 800})

      const headerLogo = page.locator('.cf-header-logo')
      const govName = page.locator('.cf-gov-name')
      await expect(headerLogo).toBeHidden()
      await expect(govName).toBeVisible()
    })
  })
})
