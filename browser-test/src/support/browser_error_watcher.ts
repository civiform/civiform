import {Page} from 'playwright'

interface ErrorOnPage {
  message: string
  url: string
}

/**
 * Class that watches for various errors that can happen within browser
 * controlled by Playwright. During test run this class accumulates errors
 * and at the end of the test (in afterEach) it will fail the test if at least
 * one error was collectd.
 */
export class BrowserErrorWatcher {
  private readonly errors: ErrorOnPage[] = []

  constructor(page: Page) {
    // Catch JS errors on page.
    page.on('pageerror', (error) => {
      this.errors.push({message: error.stack || error.message, url: page.url()})
    })

    // Catch requests failed due to network problems.
    page.on('requestfailed', (request) => {
      this.errors.push({
        message: request.failure()?.errorText || 'no failure',
        url: request.url(),
      })
    })

    // Catch requests failed due to 4xx or 5xx responses.
    page.on('requestfinished', async (request) => {
      const statusCode = (await request.response())?.status() || 200
      if (statusCode >= 400 && statusCode < 600) {
        this.errors.push({
          message: `Got response with status code ${statusCode}`,
          url: request.url(),
        })
      }
    })
  }

  failIfContainsErrors() {
    if (this.errors.length === 0) {
      return
    }
    const errorMessages = this.errors
      .map((error) => `Page: ${error.url}\nError: ${error.message}`)
      .join('\n\n')
    throw new Error(
      `Detected ${this.errors.length} errors in browser during test run:\n\n${errorMessages}`,
    )
  }
}
