import {Frame, Page} from '@playwright/test'

/*
 * Replaces any variable content with static values. This is particularly useful
 * for image diffs.
 *
 * Supports date and time elements with class .cf-bt-date, and applicant IDs
 * with class .cf-application-id
 */
export const normalizeElements = async (page: Frame | Page) => {
  await page.evaluate(() => {
    const replacements: {[selector: string]: (text: string) => string} = {
      '.cf-bt-date': (text) =>
        text
          .replace(/\d{4}\/\d{2}\/\d{2}/, '2030/01/01')
          .replace(/\d{4}-\d{2}-\d{2}/, '2030-01-01')
          .replace(/\b(\d{1,2}\/\d{1,2}\/\d{2})\b/, '1/1/30')
          .replace(/\d{1,2}:\d{2} (AM|PM) [A-Z]{2,3}/, '11:22 PM PDT')
          .replace(/^[A-Z][a-z]+ \d{1,2}, \d{4}$/, 'January 1, 2030'),
      '.cf-application-id': (text) => text.replace(/\d+/, '1234'),
      '.cf-bt-email': () => 'fake-email@example.com',
      '.cf-bt-api-key-id': (text) => text.replace(/ID: .*/, 'ID: ####'),
      '.cf-bt-api-key-created-by': (text) =>
        text.replace(/Created by .*/, 'Created by fake-admin-12345'),
    }
    for (const [selector, replacement] of Object.entries(replacements)) {
      for (const element of Array.from(document.querySelectorAll(selector))) {
        if (
          selector == '.cf-bt-email' &&
          element.textContent == '(no email address)'
        ) {
          continue
        } else {
          element.textContent = replacement(element.textContent)
        }
      }
    }
  })
}
