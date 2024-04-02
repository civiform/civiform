import {expect} from '@playwright/test'
import {Page} from 'playwright'
import {waitForPageJsLoad} from './wait'
import {writeFileSync, unlinkSync} from 'fs'

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

  async downloadProgram() {
    await this.page.getByRole('button', {name: 'Download program'}).click()
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

  async uploadProgramJson(jsonFileName: string) {
    await this.page.locator('input[type=file]').setInputFiles(jsonFileName)
    await this.uploadFile()
  }

  async uploadProgramJsonWithContent(content: string) {
    await this.page.locator('input[type=file]').setInputFiles({
      name: 'file.json',
      mimeType: 'application/json',
      buffer: Buffer.from(content),
    })
    await this.uploadFile()
  }

  async uploadProgramJsonWithSize(mbSize: int) {
    const filePath = 'file-size-' + mbSize + '-mb.json'
    writeFileSync(filePath, 'C'.repeat(mbSize * 1024 * 1024))
    await this.page.locator('input[type=file]').setInputFiles(filePath)
    await this.uploadFile()
    unlinkSync(filePath)
  }

  async uploadFile() {
    await this.page.getByRole('button', {name: 'Upload program'}).click()
    await waitForPageJsLoad(this.page)
  }

  async expectImportError() {
    await expect(
      this.page
        .getByRole('alert')
        .getByRole('heading', {name: 'Error processing file'}),
    ).toBeVisible()
  }
}
