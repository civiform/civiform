import {Locator, Page} from '@playwright/test'
import {BaseAdminPage} from '../base_admin_page'
import {waitForPageJsLoad} from '../../../support/wait'

/**
 * This is representation of the /docs/api/schemas page.
 */
export class ApiSchemaViewerPage extends BaseAdminPage {
  constructor(page: Page) {
    super(page)
  }

  async goto() {
    await this.page.goto('/docs/api/schemas')
    await waitForPageJsLoad(this.page)
  }

  async gotoViaNav() {
    await this.clickPrimaryNavSubMenuLink('API', 'Schema viewer')
    await waitForPageJsLoad(this.page)
  }

  getPageHeading(): Locator {
    return super.getPageHeading('API schema viewer')
  }

  getDocsTab(): Locator {
    return this.page.getByRole('link', {name: 'Docs'})
  }

  getSchemaTab(): Locator {
    return this.page.getByRole('link', {name: 'Schema viewer'})
  }

  getProgramSelect(): Locator {
    return this.page.getByRole('combobox', {name: 'Program'})
  }

  getStatusSelect(): Locator {
    return this.page.getByRole('combobox', {name: 'Status'})
  }

  getOpenApiVersionSelect(): Locator {
    return this.page.getByRole('combobox', {name: 'OpenApi Version'})
  }

  async selectProgram(slug: string) {
    await this.getProgramSelect().selectOption(slug)
    await waitForPageJsLoad(this.page)
  }

  async selectStatus(status: 'active' | 'draft') {
    await this.getStatusSelect().selectOption(status)
    await waitForPageJsLoad(this.page)
  }

  async selectOpenApiVersion(version: string) {
    await this.getOpenApiVersionSelect().selectOption(version)
    await waitForPageJsLoad(this.page)
  }

  getSwaggerProgramHeading(programName: string): Locator {
    return this.page.getByRole('heading', {name: programName})
  }

  getErrorMessage(): Locator {
    return this.page.getByTestId('ui-error')
  }
}
