import {test, expect, Frame, Page} from '@playwright/test'
import {AxeBuilder} from '@axe-core/playwright'
import {waitForPageJsLoad} from './wait'
import {BASE_URL} from './config'

export const isLocalDevEnvironment = () => {
  return (
    BASE_URL === 'http://civiform:9000' || BASE_URL === 'http://localhost:9999'
  )
}

export const dismissToast = async (page: Page) => {
  await page.locator('#toast-container div:text("x")').click()
  await waitForPageJsLoad(page)
}

export const selectApplicantLanguageNorthstar = async (
  page: Page,
  languageCode: string,
) => {
  await test.step('Set applicant language from header dropdown', async () => {
    await page.click('#select-language-menu')

    await page.click(`#select-language-${languageCode}`)

    await waitForPageJsLoad(page)
  })
}

export const disableFeatureFlag = async (page: Page, flag: string) => {
  await test.step(`Disable feature flag: ${flag}`, async () => {
    const response = await page.goto(`/dev/feature/${flag}/disable`)
    expect(response?.status(), {
      message: `Could not disable feature flag '${flag}'. Make sure the flag exists.`,
    }).toBe(200)
  })
}

export const enableFeatureFlag = async (page: Page, flag: string) => {
  await test.step(`Enable feature flag: ${flag}`, async () => {
    const response = await page.goto(`/dev/feature/${flag}/enable`)
    expect(response?.status(), {
      message: `Could not enable feature flag '${flag}'. Make sure the flag exists.`,
    }).toBe(200)
  })
}

/**
 * Close the warning toast message if it is showing, otherwise the element may be in
 * the way when trying to click on various top nav elements.
 * @param {Page} page Playwright page to operate against
 */
export const closeWarningMessage = async (page: Page) => {
  const warningMessageLocator = page.locator('#warning-message-dismiss')

  if (await warningMessageLocator.isVisible()) {
    await warningMessageLocator.click()
  }
}

/**
 * Run accessibility tests using axe accessibility testing engine
 * @param {Page} page Playwright page to operate against
 */
export const validateAccessibility = async (page: Page) => {
  await test.step(
    'Validate accessiblity',
    async () => {
      const results = await new AxeBuilder({page}).analyze()
      const errorMessage = `Found ${results.violations.length} axe accessibility violations\nOn page: ${page.url()}`
      expect(results.violations, errorMessage).toEqual([])
    },
    {box: true},
  )
}

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

/**
 * Check if the toast message contains the expected value
 * @param {Page} page Playwright page to operate against
 * @param {string} value Text to look for within the toast message
 */
export const validateToastMessage = async (page: Page, value: string) => {
  await test.step(
    'Validate toast message',
    async () => {
      const toastMessages = await page.innerText('#toast-container')
      expect(toastMessages).toContain(value)
    },
    {box: true},
  )
}

export const validateToastHidden = async (page: Page) => {
  await test.step(
    'Validate toast hidden',
    async () => {
      await expect(page.locator('#toast-container')).toBeHidden()
    },
    {box: true},
  )
}

/**
 * This can be used to simulate slow networks to aid in debugging flaky tests. Its use *should NOT* be
 * committed into the codebase as a permanent fix to anything.
 *
 * This works by modifying the network requests of routes and adding a timeout to help extend the load
 * time of pages. Place this at the beginning of a specific test or a beforeEach call. Playwright currently
 * does not have any builtin throttling capabilities and this is the least invasive option.
 *
 * @param page Playwright page
 * @param {number} timeout in milliseconds
 */
export const throttle = async (page: Page, timeout: number = 100) => {
  await page.route('**/*', async (route) => {
    await new Promise((f) => setTimeout(f, timeout))
    await route.continue()
  })
}
