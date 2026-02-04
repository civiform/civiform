import {Locator, Page} from '@playwright/test'
import {BaseAdminPage} from '../base_admin_page'

/**
 * This is representation of the /admin/programs/:programId/api-bridge/edit page.
 */
export class ProgramBridgeConfigurationPage extends BaseAdminPage {
  constructor(page: Page) {
    super(page)
  }

  getPageHeading(): Locator {
    return super.getPageHeading('Edit Program Bridge Definitions')
  }

  async changeBridgeAdminName(value: string) {
    await this.page
      .getByRole('combobox', {name: 'API Bridge'})
      .selectOption(value)
    await super.waitForHtmxReady()
  }

  async save() {
    await this.page.getByRole('button', {name: 'Save'}).click()
  }

  getInputQuestion(externalName: string): Locator {
    return this.page.getByRole('combobox', {
      name: `Input field question for ${externalName}`,
    })
  }

  getInputScalar(externalName: string): Locator {
    return this.page.getByRole('combobox', {
      name: `Input field scalar for ${externalName}`,
    })
  }

  getOutputQuestion(externalName: string): Locator {
    return this.page.getByRole('combobox', {
      name: `Output field question for ${externalName}`,
    })
  }

  getOutputScalar(externalName: string): Locator {
    return this.page.getByRole('combobox', {
      name: `Output field scalar for ${externalName}`,
    })
  }

  async setInputQuestion(externalName: string, value: string) {
    await this.getInputQuestion(externalName).selectOption({value: value})
  }

  async setInputScalar(externalName: string, value: string) {
    await this.getInputScalar(externalName).selectOption({value: value})
  }

  async setOutputQuestion(externalName: string, value: string) {
    await this.getOutputQuestion(externalName).selectOption({value: value})
  }

  async setOutputScalar(externalName: string, value: string) {
    await this.getOutputScalar(externalName).selectOption({value: value})
  }

  getInputFieldsHeading(): Locator {
    return this.page.getByRole('heading', {name: 'Input Fields'})
  }

  getOutputFieldsHeading(): Locator {
    return this.page.getByRole('heading', {name: 'Output Fields'})
  }
}
