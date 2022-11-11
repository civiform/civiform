import {
  createTestContext,
  validateAccessibility,
  validateScreenshot,
} from './support'

describe('the landing page', () => {
  const ctx = createTestContext()

  it('it has login options', async () => {
    expect(await ctx.page.textContent('html')).toContain('Continue as guest')
    expect(await ctx.page.textContent('html')).toContain('Create account')
    await validateScreenshot(ctx.page, 'landing-page')
  })

  it('has no accessibility violations', async () => {
    await validateAccessibility(ctx.page)
  })
})
