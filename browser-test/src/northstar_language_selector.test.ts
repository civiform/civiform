import {test, expect} from './support/civiform_fixtures'
import type {Page} from '@playwright/test'

test.describe('Language Selector Visibility', () => {
  test('shows language selector when multiple languages are enabled', async ({
    page,
  }: {
    page: Page
  }) => {
    await page.waitForSelector('[data-testid="languageSelector"]', {
      timeout: 10000,
    })
    const selector = page.locator('[data-testid="languageSelector"]')
    await expect(selector).toHaveCount(1)
  })
})
