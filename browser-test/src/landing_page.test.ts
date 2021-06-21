import { startSession, endSession } from './support'

describe('the landing page', () => {
  it('it has login options', async () => {

    const { browser, page } = await startSession();

    expect(await page.textContent('html')).toContain('CONTINUE AS GUEST');

    await endSession(browser);
  })
})
