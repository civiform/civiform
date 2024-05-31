import {expect} from '@playwright/test'
import {Page} from 'playwright'
import {waitForPageJsLoad} from './wait'
import {readFileSync} from 'fs'

export class AdminProgramMigration {
  public page!: Page

  constructor(page: Page) {
    this.page = page
  }

  async goToExportPage() {
    await this.page.getByRole('link', {name: 'Export'}).click()
    await waitForPageJsLoad(this.page)
    await this.expectExportPage()
  }

  async expectExportPage() {
    await expect(
      this.page.getByRole('heading', {name: 'Export a program'}),
    ).toBeVisible()
  }

  async selectProgramToExport(adminName: string) {
    await this.page.check(`text=${adminName}`)
  }

  async generateJSON() {
    await this.page.getByRole('button', {name: 'Generate JSON'}).click()
  }

  async expectJSONPreview() {
    const jsonPreview = this.page.locator('#program-json')

    // The json preview should be a text area and should be disabled to prevent editing
    const tagName = await jsonPreview.evaluate((element) =>
      element.tagName.toLowerCase(),
    )
    expect(tagName).toBe('textarea')
    await expect(jsonPreview).toBeDisabled()

    await expect(
      this.page.getByRole('button', {name: 'Download JSON'}),
    ).toBeVisible()
    await expect(
      this.page.getByRole('button', {name: 'Copy JSON'}),
    ).toBeVisible()

    return jsonPreview.innerHTML()
  }

  async downloadJSON() {
    const [downloadEvent] = await Promise.all([
      this.page.waitForEvent('download'),
      this.page.getByRole('button', {name: 'Download JSON'}).click(),
    ])
    const path = await downloadEvent.path()
    if (path === null) {
      throw new Error('download failed')
    }
    return readFileSync(path, 'utf8')
  }

  async goToImportPage() {
    await this.page.getByRole('link', {name: 'Import'}).click()
    await waitForPageJsLoad(this.page)
    await this.expectImportPage()
  }

  async expectImportPage() {
    await expect(
      this.page.getByRole('heading', {name: 'Import a program'}),
    ).toBeVisible()
  }

  async submitProgramJSON(content: string) {
    await this.page.getByRole('textbox').fill(content)
    await this.page
      .getByRole('button', {name: 'Display program information'})
      .click()
    await waitForPageJsLoad(this.page)
  }

  async expectImportError() {
    await expect(
      this.page
        .getByRole('alert')
        .getByRole('heading', {name: 'Error processing JSON'}),
    ).toBeVisible()
  }

  async expectProgramImported(programName: string) {
    await expect(
      this.page.getByRole('heading', {name: 'Program name: ' + programName}),
    ).toBeVisible()
  }
}
