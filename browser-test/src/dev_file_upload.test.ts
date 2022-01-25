import { Page } from 'playwright'
import { readFileSync } from 'fs'
import { startSession, endSession, waitForPageJsLoad } from './support'

const { BASE_URL = 'http://civiform:9000' } = process.env

describe('the dev file upload page', () => {
  it('it can upload and download a file', async () => {
    const { browser, page } = await startSession()

    await page.goto(BASE_URL + '/dev/fileUpload')

    expect(await page.textContent('h1')).toContain('Dev file upload')

    await page.setInputFiles('input[type=file]', {
      name: 'file.txt',
      mimeType: 'text/plain',
      buffer: Buffer.from('this is test'),
    })

    await page.click('button:visible')

    expect(await page.textContent('h1')).toContain('Dev file upload')
    expect(await page.content()).toContain('dev/file.txt')

    await endSession(browser)
  })
})

const downloadFile = async (page: Page, fileName: string) => {
  const [downloadEvent] = await Promise.all([
    page.waitForEvent('download'),
    page.click(`a:text("${fileName}")`),
  ])
  const path = await downloadEvent.path()
  if (path === null) {
    throw new Error('download failed')
  }
  return readFileSync(path, 'utf8')
}
