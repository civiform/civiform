import {
  AuthStrategy,
  createTestContext,
  validateAccessibility,
  validateScreenshot,
} from './support'
import {TEST_USER_AUTH_STRATEGY} from './support/config'

describe('the landing page', () => {
  const ctx = createTestContext()

  it('it has login options', async () => {
    expect(await ctx.page.textContent('html')).toContain('Continue as guest')
    if (TEST_USER_AUTH_STRATEGY !== AuthStrategy.AWS_STAGING) {
      expect(await ctx.page.textContent('html')).toContain('Create account')
    }
    await validateScreenshot(ctx.page, 'landing-page')
  })

  it('it has civiform version', async () => {
    expect(await ctx.page.textContent('html')).toContain(
      'CiviForm version: dev',
    )
    await validateScreenshot(ctx.page, 'landing-page')
  })

  it('has no accessibility violations', async () => {
    await validateAccessibility(ctx.page)
  })
})
