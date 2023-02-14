import {
  AuthStrategy,
  createTestContext,
  enableFeatureFlag,
  disableFeatureFlag,
  validateAccessibility,
  validateScreenshot,
  TestContext,
} from './support'
import {TEST_USER_AUTH_STRATEGY} from './support/config'

function sharedTests(ctx: TestContext, screenshotName: string) {
  it('has login options', async () => {
    expect(await ctx.page.textContent('html')).toContain('Continue as guest')
    if (TEST_USER_AUTH_STRATEGY !== AuthStrategy.AWS_STAGING) {
      expect(await ctx.page.textContent('html')).toContain('Create account')
    }
  })

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
        'CiviForm version: dev',
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
      expect(await ctx.page.textContent('html')).toContain(
        'CiviForm version: dev',
      )
    })
  })
})
