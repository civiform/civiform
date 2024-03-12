import {test, expect} from '@playwright/test'
import {
  createTestContext,
  loginAsTestUser,
  validateScreenshot,
  validateAccessibility,
} from './support'

test.describe('Header', () => {
  const ctx = createTestContext(/* clearDb= */ false)

  test('Not logged in, guest mode enabled', async () => {
    const {page} = ctx
    await validateScreenshot(
      page.getByRole('navigation'),
      'not-logged-in-guest-mode-enabled',
    )
  })

  // TODO(#4360): add a "Not logged in, guest mode disabled" test once we
  // can get to the programs page without logging in, for an entity without
  // guest mode.

  test('Logged in', async () => {
    const {page} = ctx
    await loginAsTestUser(page)
    await validateScreenshot(page.getByRole('navigation'), 'logged-in')
  })

  test('Passes accessibility test', async () => {
    const {page} = ctx
    await validateAccessibility(page)
  })

  test('Displays the government banner', async () => {
    const {page} = ctx
    expect(await page.textContent('section')).toContain(
      'This is an official government website.',
    )
  })

  test('Government banner expands when clicked and closes when clicked again', async () => {
    const {page} = ctx
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
