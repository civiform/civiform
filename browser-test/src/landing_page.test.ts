import {createTestContext} from './support'

describe('the landing page', () => {
  const ctx = createTestContext()

  it('it has login options', async () => {
    expect(await ctx.page.textContent('html')).toContain('Continue as guest')
  })
})
