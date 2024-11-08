import {expect} from '@playwright/test'
import {Page} from 'playwright'
import {waitForPageJsLoad} from './wait'
import {readFileSync} from 'fs'

export class AdminProgramMigration {
  public page!: Page

  constructor(page: Page) {
    this.page = page
  }

  async expectExportPage() {
    await expect(
      this.page.getByRole('heading', {name: 'Export a program'}),
    ).toBeVisible()
  }

  async expectJsonPreview() {
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

  async downloadJson() {
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

  async clickBackButton() {
    const backButton = this.page.getByText('Back to all programs')
    await backButton.click()
    await waitForPageJsLoad(this.page)
  }

  async goToImportPage() {
    await this.page.getByRole('link', {name: 'Import existing program'}).click()
    await waitForPageJsLoad(this.page)
    await this.expectImportPage()
  }

  async expectImportPage() {
    await expect(
      this.page.getByRole('heading', {name: 'Import a program'}),
    ).toBeVisible()
  }

  async submitProgramJson(content: string) {
    await waitForPageJsLoad(this.page)
    await this.page.getByRole('textbox').fill(content)
    await this.clickButton('Preview program')
  }

  async expectAlert(alertText: string, alertType: string) {
    const alert = this.page.getByRole('alert').filter({hasText: alertText})
    await expect(alert).toBeVisible()
    await expect(alert).toHaveClass(new RegExp(alertType))
    return alert
  }

  async expectProgramImported(programName: string) {
    await expect(
      this.page.getByRole('heading', {name: 'Program name: ' + programName}),
    ).toBeVisible()
  }

  async clickButton(buttonText: string) {
    await this.page.getByRole('button', {name: buttonText}).click()
    await waitForPageJsLoad(this.page)
  }
}
