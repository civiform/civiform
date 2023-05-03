import {
  createTestContext,
  enableFeatureFlag,
  disableFeatureFlag,
  validateAccessibility,
  validateScreenshot,
  TestContext,
} from './support'

function sharedTests(ctx: TestContext, screenshotName: string) {
  it('matches the expected screenshot', async () => {
    await validateScreenshot(ctx.page, screenshotName)
  })

  it('has no accessibility violations', async () => {
    await validateAccessibility(ctx.page)
  })
}

describe('the landing page', () => {
  const ctx = createTestContext()

  describe('without civiform version feature flag', () => {
    beforeEach(async () => {
      await disableFeatureFlag(
        ctx.page,
        'show_civiform_image_tag_on_landing_page',
      )
    })

    sharedTests(ctx, 'landing-page-no-version')

    it('does not have civiform version', async () => {
      expect(await ctx.page.textContent('html')).not.toContain(
        'CiviForm version:',
      )
    })
  })

  describe('with civiform version feature flag', () => {
    beforeEach(async () => {
      await enableFeatureFlag(
        ctx.page,
        'show_civiform_image_tag_on_landing_page',
      )
    })

    sharedTests(ctx, 'landing-page-with-version')

    it('has civiform version', async () => {
      expect(await ctx.page.textContent('html')).toContain('CiviForm version:')
    })
  })
})
