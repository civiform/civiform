import {test, expect} from './fixtures/custom_fixture'
import {
  loginAsTestUser,
  validateScreenshot,
  validateAccessibility,
} from './support'

test.describe('Header', {tag: ['@migrated']}, () => {
  test('Not logged in, guest mode enabled', async ({page}) => {
    await validateScreenshot(
      page.getByRole('navigation'),
      'not-logged-in-guest-mode-enabled',
    )
  })

  // TODO(#4360): add a "Not logged in, guest mode disabled" test once we
  // can get to the programs page without logging in, for an entity without
  // guest mode.
  test('Logged in', async ({page}) => {
    await loginAsTestUser(page)
    await validateScreenshot(page.getByRole('navigation'), 'logged-in')
  })

  test('Passes accessibility test', async ({page}) => {
    await validateAccessibility(page)
  })

  test('Displays the government banner', async ({page}) => {
    expect(await page.textContent('section')).toContain(
      'This is an official government website.',
    )
  })

  test('Government banner expands when clicked and closes when clicked again', async ({
    page,
  }) => {
    // The banner is initially closed
    await expect(page.locator('.usa-banner__content')).toBeHidden()
    // Click to expand the banner
    await page.click('.usa-banner__button')

    await expect(page.locator('.usa-banner__content')).toBeVisible()
    await validateScreenshot(page.getByRole('navigation'), 'banner-expanded')
    // Click again to close the banner
    await page.click('.usa-banner__button')

    await expect(page.locator('.usa-banner__content')).toBeHidden()
  })
})
