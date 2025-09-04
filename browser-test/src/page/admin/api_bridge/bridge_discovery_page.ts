import {Locator, Page} from '@playwright/test'
import {BaseAdminPage} from '../base_admin_page'

/**
 * This is representation of the /admin/api-bridge-discovery page.
 */
export class BridgeDiscoveryPage extends BaseAdminPage {
  constructor(page: Page) {
    super(page)
  }

  async goto() {
    await this.page.goto('/admin/api-bridge/discovery')
  }

  getPageHeading(): Locator {
    return super.getPageHeading('API Bridge Discovery')
  }

  getErrorAlert(): Locator {
    return this.page.getByRole('alert', {name: 'Error'})
  }

  getSaveSuccessfulAlert(): Locator {
    return this.page.getByRole('alert', {name: 'Success'})
  }

  getEndpointHeading(urlPath: string): Locator {
    return this.page.getByRole('heading', {name: `Endpoint: ${urlPath}`})
  }

  getSelectedTab(): Locator {
    return this.page.getByRole('link', {name: 'Discovery'})
  }

  async fillUrl(url: string) {
    await this.page.getByRole('textbox', {name: 'URL'}).fill(url)
  }

  async clickSearchButton() {
    await this.page.getByRole('button', {name: 'Search'}).click()
    await this.waitForHtmxReady()
  }

  async clickAddButton(urlPath: string) {
    await this.page
      .getByRole('listitem')
      .filter({has: this.getEndpointHeading(urlPath)})
      .getByRole('button', {name: 'Add Endpoint'})
      .click()

    await this.waitForHtmxReady()
  }

  getTable(urlPath: string, tableName: string): Locator {
    return this.page
      .getByRole('listitem')
      .filter({has: this.getEndpointHeading(urlPath)})
      .getByRole('table', {name: tableName})
  }
}
