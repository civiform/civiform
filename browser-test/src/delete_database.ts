import {
  startSession,
  dropTables,
  endSession,
  seedCanonicalQuestions,
} from './support'
import * as fs from 'fs'

module.exports = async () => {
  const {browser, page} = await startSession()
  await dropTables(page)
  await seedCanonicalQuestions(page)

  await endSession(browser)

  // Clean up videos directory. Previous test runs might have created it.
  // If we don't clean up directory - video files gets accumulated and it
  // becomes hard to see the latest videos.
  if (fs.existsSync('tmp/videos')) {
    fs.rmSync('tmp/videos', {recursive: true})
  }
}
