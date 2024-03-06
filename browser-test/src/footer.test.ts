import {test, expect} from '@playwright/test'
import {
  createTestContext,
  enableFeatureFlag,
  disableFeatureFlag,
  validateAccessibility,
  validateScreenshot,
  TestContext,
} from './support'
import {Locator} from 'playwright'

function sharedTests(ctx: TestContext, screenshotName: string) {
  test('matches the expected screenshot', async () => {
    const footer: Locator = ctx.page.locator('footer')
    await validateScreenshot(footer, screenshotName)
  })

  test('has no accessibility violations', async () => {
    await validateAccessibility(ctx.page)
  })
}

test.describe('the footer', () => {
  const ctx = createTestContext()

  test.describe('without civiform version feature flag', () => {
    test.beforeEach(async () => {
      await disableFeatureFlag(
        ctx.page,
        'show_civiform_image_tag_on_landing_page',
      )
    })

    sharedTests(ctx, 'footer-no-version')

    test('does not have civiform version', async () => {
      expect(await ctx.page.textContent('html')).not.toContain(
        'CiviForm version:',
      )
    })
  })

  test.describe('with civiform version feature flag', () => {
    test.beforeEach(async () => {
      await enableFeatureFlag(
        ctx.page,
        'show_civiform_image_tag_on_landing_page',
      )
    })

    sharedTests(ctx, 'footer-with-version')

    test('has civiform version', async () => {
      expect(await ctx.page.textContent('html')).toContain('CiviForm version:')
    })
  })
})
