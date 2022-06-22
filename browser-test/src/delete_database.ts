import {
  startSession,
  dropTables,
  endSession,
  seedCanonicalQuestions,
} from './support'

module.exports = async () => {
  const { browser, page } = await startSession()
  await dropTables(page)
  await seedCanonicalQuestions(page)

  await endSession(browser)
}
