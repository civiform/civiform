import * as fs from 'fs'

function globalSetup() {
  console.log('CUSTOM GLOBAL SETUP')

  // Clean up directories
  if (fs.existsSync('tmp/videos')) {
    fs.rmSync('tmp/videos', {recursive: true})
  }

  if (fs.existsSync('tmp/json-output')) {
    fs.rmSync('tmp/json-output', {recursive: true})
  }
}

export default globalSetup
