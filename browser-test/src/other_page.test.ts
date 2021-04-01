import { startSession } from './support'

it('this is the other page test', async () => {

  const { page } = await startSession()

  expect(await page.textContent('html')).toContain('continue as guest')
})
