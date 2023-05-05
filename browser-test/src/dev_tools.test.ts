import {
  createTestContext,
  validateAccessibility,
  validateScreenshot,
} from './support'
import {Locator} from 'playwright'

describe('developer tools', () => {
  const ctx = createTestContext()

  it('link shown in the header', async () => {
    const header: Locator = ctx.page.locator('header')
    await validateScreenshot(header, 'dev-tools-in-header')

    expect(await ctx.page.textContent('header')).toContain('DevTools')

    await validateAccessibility(ctx.page)
  })

  it('modal appears on click', async () => {
    await ctx.page.click('#debug-content-modal-button')
    await validateScreenshot(ctx.page, 'dev-tools-modal')
  })
})
