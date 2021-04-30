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

    switch (BASE_URL) {
      case 'http://localhost:9999':
        // Do not check file content if run locally because presigned links
        // do not work outside docker network.
        break;
      case 'http://civiform:9000':
        // Localstack responds with mime type 'text/html' while actual AWS
        // responds with type 'binary/octet-stream'. Browser would not download
        // files with type 'text/html' so we just go to the display page and
        // verify content.
        await page.click('a:text("file.txt")');
        expect(await page.content()).toContain('this is test');
        break;
      default:
        // Download the file and verify content in all other cases.
        const fileContent = await downloadFile(page, 'file.txt');
        expect(fileContent).toContain('this is test');
    }

    await endSession(browser);
  })
})

const downloadFile = async (page: Page, fileName: string) => {
  const [downloadEvent] = await Promise.all([
    page.waitForEvent('download'),
    page.click(`a:text("${fileName}")`)
  ]);
  const path = await downloadEvent.path();
  if (path === null) {
    throw new Error('download failed');
  }
  return readFileSync(path, 'utf8');
}
