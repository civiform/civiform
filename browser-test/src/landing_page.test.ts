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
    await disableFeatureFlag(ctx.page, 'new_login_form_enabled')
    expect(await ctx.page.textContent('html')).toContain('Continue as guest')
    if (TEST_USER_AUTH_STRATEGY !== AuthStrategy.AWS_STAGING) {
      expect(await ctx.page.textContent('html')).toContain('Create an account')
    }

    await enableFeatureFlag(ctx.page, 'new_login_form_enabled')
    expect(await ctx.page.textContent('html')).toContain(
      'or continue as a guest',
    )
    if (TEST_USER_AUTH_STRATEGY !== AuthStrategy.AWS_STAGING) {
      expect(await ctx.page.textContent('html')).toContain('Create an account')
    }
  })

  it('matches the expected screenshot', async () => {
    await disableFeatureFlag(ctx.page, 'new_login_form_enabled')
    await validateScreenshot(ctx.page, screenshotName + '-old-login')

    // New login page is taller, so set the viewport so that we can see it all.
    await ctx.page.setViewportSize({width: 1300, height: 900})
    await enableFeatureFlag(ctx.page, 'new_login_form_enabled')
    await validateScreenshot(ctx.page, screenshotName + '-new-login')
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
