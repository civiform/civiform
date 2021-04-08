import { startSession, endSession } from './support'

it('this is the other page test', async () => {

  const { browser, page } = await startSession();

  expect(await page.textContent('html')).toContain('continue as guest');

  await endSession(browser);
})
