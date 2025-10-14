import {Locator, Page} from '@playwright/test'
import {BaseAdminPage} from '../base_admin_page'

/**
 * This is representation of the /admin/tools/urlChecker page.
 */
export class UrlCheckerPage extends BaseAdminPage {
  constructor(page: Page) {
    super(page)
  }

  async goto() {
    await this.page.goto('/admin/tools/urlChecker')
  }

  getPageHeading(): Locator {
    return super.getPageHeading('URL Checker')
  }

  async fillUrl(url: string) {
    await this.page.getByRole('textbox', {name: 'URL'}).fill(url)
  }

  async clickCheckButton() {
    await this.page.getByRole('button', {name: 'Check'}).click()
    await this.waitForHtmxReady()
  }

  getOutput(): Locator {
    return this.page.getByRole('status')
  }
}
