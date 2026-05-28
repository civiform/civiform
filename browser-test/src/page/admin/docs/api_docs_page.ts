import {Locator, Page} from '@playwright/test'
import {BaseAdminPage} from '../base_admin_page'
import {waitForPageJsLoad} from '../../../support/wait'

/**
 * This is representation of the /docs/api/programs/:slug/:version page.
 */
export class ApiDocsPage extends BaseAdminPage {
  constructor(page: Page) {
    super(page)
  }

  async gotoViaNav() {
    await this.clickPrimaryNavSubMenuLink('API', 'Documentation')
    await waitForPageJsLoad(this.page)
  }

  getPageHeading(): Locator {
    return super.getPageHeading('API documentation')
  }

  getDocsTab(): Locator {
    return this.page.getByRole('link', {name: 'Docs'})
  }

  getSchemaTab(): Locator {
    return this.page.getByRole('link', {name: 'Schema viewer'})
  }

  getProgramSelect(): Locator {
    return this.page.getByRole('combobox', {name: 'Select a program'})
  }

  getVersionSelect(): Locator {
    return this.page.getByRole('combobox', {name: 'Select version'})
  }

  async selectProgram(slug: string) {
    await this.getProgramSelect().selectOption(slug)
    await waitForPageJsLoad(this.page)
  }

  async selectVersion(version: 'active' | 'draft') {
    await this.getVersionSelect().selectOption(version)
    await waitForPageJsLoad(this.page)
  }

  getAccordionButton(): Locator {
    return this.page.getByRole('button', {name: 'How does this work?'})
  }

  async clickAccordion() {
    await this.getAccordionButton().click()
  }

  getQuestionsHeading(): Locator {
    return this.page.getByRole('heading', {name: 'Questions'})
  }

  getResponsePreviewHeading(): Locator {
    return this.page.getByRole('heading', {name: 'API response preview'})
  }

  getJsonPreview(): Locator {
    return this.page.getByTestId('jsonPreview')
  }

  getNotFoundMessage(): Locator {
    return this.page.getByText('No programs found.')
  }
}
