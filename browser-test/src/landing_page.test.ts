import {createBrowserContext, validateScreenshot} from './support'

describe('the landing page', () => {
  const ctx = createBrowserContext()
  it('it has login options', async () => {
    expect(await ctx.page.textContent('html')).toContain('Continue as guest')
    await validateScreenshot(ctx.page, 'landing-page', {fullPage: true})
  })
})
