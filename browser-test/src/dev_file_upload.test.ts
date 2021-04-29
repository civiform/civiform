import { Page } from 'playwright'
import { readFileSync } from 'fs'
import { startSession, endSession } from './support'

const { BASE_URL = 'http://civiform:9000' } = process.env

describe('the dev file upload page', () => {
  it('it can upload and download a file', async () => {

    const { browser, page } = await startSession();

    await page.goto(BASE_URL + '/dev/fileUpload');

    expect(await page.textContent('h1')).toContain('Dev File Upload');

    await page.setInputFiles('input[type=file]', {
      name: 'file.txt',
      mimeType: 'text/plain',
      buffer: Buffer.from('this is test')
    });

    await page.click('button');

    expect(await page.textContent('h1')).toContain('Dev File Upload');

    if (BASE_URL !== 'http://localhost:9999') {
      // Only confirm file content if not localhost because presigned link
      // doesn't work outside docker network.

      // Localstack responds with 'text/html' while actual AWS responds 'binary/octet-stream'.
      // We just go to the display page and check content.
      await page.click('text=file.txt');
      expect(await page.content()).toContain('this is test')
    }

    await endSession(browser);
  })
})
