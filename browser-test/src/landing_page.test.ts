import { endSession, startSession, validateScreenshot } from './support'

describe('the landing page', () => {
  it('it has login options', async () => {
    const {browser, page} = await startSession()

    expect(await page.textContent('html')).toContain('Continue as guest')
    await validateScreenshot(page);

    await endSession(browser)
  })
})
