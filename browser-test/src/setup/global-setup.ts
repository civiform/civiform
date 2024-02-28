import {startSession, dropTables, endSession, seedQuestions} from '../support'
import * as fs from 'fs'

async function globalSetup() {
  console.log("CUSTOM GLOBAL SETUP")

  const {browser, page} = await startSession()
  await dropTables(page)
  await seedQuestions(page)

  await endSession(browser)

  // Clean up videos directory. Previous test runs might have created it.
  // If we don't clean up directory - video files gets accumulated and it
  // becomes hard to see the latest videos.
  if (fs.existsSync('tmp/videos')) {
    fs.rmSync('tmp/videos', {recursive: true})
  }
}

export default globalSetup
