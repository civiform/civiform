import {
  createTestContext,
  validateAccessibility,
  validateScreenshot,
} from './support'
import {Locator} from 'playwright'

describe('developer tools', () => {
  const ctx = createTestContext()

  test('link shown in the header', async () => {
    const header: Locator = ctx.page.locator('nav')
    await validateScreenshot(header, 'dev-tools-in-header')

    expect(await ctx.page.textContent('nav')).toContain('DevTools')

    await validateAccessibility(ctx.page)
  })

  test('modal appears on click', async () => {
    await ctx.page.click('#debug-content-modal-button')
    await validateScreenshot(ctx.page, 'dev-tools-modal')
  })
})
