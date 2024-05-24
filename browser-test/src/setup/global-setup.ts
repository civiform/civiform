import {startSession, dropTables, endSession, seedQuestions} from '../support'
import * as fs from 'fs'

async function globalSetup() {
  console.log('CUSTOM GLOBAL SETUP')

  const {browser, page} = await startSession()
  await dropTables(page)
  await seedQuestions(page)

  await endSession(browser)

  // Clean up directories
  if (fs.existsSync('tmp/videos')) {
    fs.rmSync('tmp/videos', {recursive: true})
  }

  if (fs.existsSync('tmp/json-output')) {
    fs.rmSync('tmp/json-output', {recursive: true})
  }
}

export default globalSetup
