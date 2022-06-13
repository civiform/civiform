import { startSession, dropTables, endSession, seedCanonicalQuestions } from './support'

module.exports = async () => {
  const { browser, page } = await startSession()
  await dropTables(page)

  // If the base URL is not localhost, then this is a prober run and the
  // canonical questions need to be seeded manually since the tests are
  // not running immediately after a server start.
  if (!process.env.BASE_URL.includes('localhost')) {
    await seedCanonicalQuestions(page)
  }

  await endSession(browser)
}
