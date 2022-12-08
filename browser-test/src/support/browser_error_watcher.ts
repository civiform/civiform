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
  private readonly downloadUrls = new Set<string>()
  private readonly urlsToIgnore: RegExp[] = []

  constructor(page: Page) {
    // Catch JS errors on page.
    page.on('pageerror', (error) => {
      this.errors.push({message: error.stack || error.message, url: page.url()})
    })

    // For some reason all download file requests end up as net::ERR_ABORTED.
    // To exclude it from our error detection we record them here and check
    // later.
    page.on('download', (download) => {
      this.downloadUrls.add(download.url())
    })

    // Catch requests failed due to network problems.
    page.on('requestfailed', (request) => {
      this.errors.push({
        message: request.failure()?.errorText || 'no failure',
        url: request.url(),
      })
    })

    // Catch requests failed due to 4xx or 5xx responses.
    page.on('requestfinished', (request) => {
      void request.response().then((response) => {
        if (response == null) return
        const statusCode = response.status()
        if (statusCode >= 400 && statusCode < 600) {
          this.errors.push({
            message: `Got response with status code ${statusCode}`,
            url: request.url(),
          })
        }
      })
    })
  }

  failIfContainsErrors() {
    const errorsToReport = this.errors
      .filter((error) => !this.downloadUrls.has(error.url))
      .filter((error) => {
        return !this.urlsToIgnore.some((regexp) => regexp.test(error.url))
      })
    if (errorsToReport.length === 0) {
      return
    }
    const errorMessages = errorsToReport
      .map((error) => `Page: ${error.url}\nError: ${error.message}`)
      .join('\n\n')
    throw new Error(
      `Detected ${errorsToReport.length} errors in browser during test run:\n\n${errorMessages}`,
    )
  }

  /**
   * Instructs BrowserErrorWatcher to ignore errors coming from all urls that
   * match the provided regexp. The url doesn't have to fully match the regexp,
   * even if the regexp matches only part of the url - that url will be ignored.
   *
   * Ignore list is reset between tests. So if you need to ignore errors for
   * multiple/all tests - use `beforeEach`.
   */
  ignoreErrorsFromUrl(regexp: RegExp) {
    this.urlsToIgnore.push(regexp)
  }
}
