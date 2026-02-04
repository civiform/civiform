import {Locator, Page} from '@playwright/test'
import {waitForHtmxReady} from '../support/wait'

/**
 * Represents a page and related global components
 */
export class BasePage {
  protected page: Page

  constructor(page: Page) {
    this.page = page
  }

  /**
   * Convenience wrapper around waitForHtmxReady
   */
  protected async waitForHtmxReady() {
    await waitForHtmxReady(this.page)
  }

  /**
   * Get the primary page heading, the h1 element
   */
  protected getPageHeading(pageHeadingName: string): Locator {
    return this.page.getByRole('heading', {name: pageHeadingName, level: 1})
  }
}
