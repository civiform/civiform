import { Page } from 'playwright'
import { readFileSync } from 'fs'
import { startSession, endSession } from './support'

const { BASE_URL = 'http://civiform:9000' } = process.env

describe('the landing page', () => {
  it('it has login options', async () => {

    const { browser, page } = await startSession();

    await page.goto(BASE_URL + '/dev/fileUpload');

    expect(await page.textContent('html')).toContain('Dev File Upload');

    await page.setInputFiles('input#myFile', {
      name: 'file.txt',
      mimeType: 'text/plain',
      buffer: Buffer.from('this is test')
    });

    await page.click('button');

    expect(await page.textContent('html')).toContain('Dev File Upload');

    const fileContent = await downloadFile(page, 'file.txt');
    expect(fileContent).toContain('this is test');

    await endSession(browser);
  })
})

const downloadFile = async (page: Page, fileName: string) => {
  const [downloadEvent] = await Promise.all([
    page.waitForEvent('download'),
    page.click(`text="${fileName}"`)
  ]);
  const path = await downloadEvent.path();
  if (path === null) {
    throw new Error('download failed');
  }
  return readFileSync(path, 'utf8');
}