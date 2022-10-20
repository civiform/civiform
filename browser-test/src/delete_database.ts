import {
  startSession,
  dropTables,
  endSession,
  seedCanonicalQuestions,
} from './support'

module.exports = async () => {
  const {browser, page} = await startSession()
  page.setDefaultTimeout(5000)
  await dropTables(page)
  await seedCanonicalQuestions(page)

  await endSession(browser)
}
