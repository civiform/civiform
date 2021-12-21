import { startSession, dropTables, endSession } from './support'

module.exports = async () => {
  const { browser, page } = await startSession()
  await dropTables(page)
  await endSession(browser)
}
