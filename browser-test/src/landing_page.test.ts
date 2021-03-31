import { startSession } from './support'

describe('the landing page', () => {
  it('it has login options', async () => {

    const { page } = await startSession()

    expect(await page.textContent('html')).toContain('continue as guest')
  })
})
