import {expect} from '@playwright/test'
import {Page} from 'playwright'

/** Class for working with the file upload question that applicants see. */
export class ApplicantFileQuestion {
  private fileSelectionErrorLocator = '#cf-fileupload-required-error'
  private fileTooLargeErrorLocator = '#cf-fileupload-too-large-error'
  private continueButtonLocator = '#fileupload-continue-button'
  private continueFormLocator = '#cf-fileupload-continue-form'
  private skipButtonLocator = '#fileupload-skip-button'
  private deleteButtonLocator = '#fileupload-delete-button'
  private questionErrorLocator = '.cf-question-error-message'
  private uploadedFilesLocator = '#cf-fileupload-uploaded-files'

  private page!: Page

  constructor(page: Page) {
    this.page = page
  }

  async expectQuestionErrorShown() {
    const error = this.page.locator(this.questionErrorLocator)
    expect(await error?.isHidden()).toEqual(false)
  }

  async expectQuestionErrorHidden() {
    const error = this.page.locator(this.questionErrorLocator)
    expect(await error?.isHidden()).toEqual(true)
  }

  async expectFileSelectionErrorShown() {
    const error = this.page.locator(this.fileSelectionErrorLocator)
    expect(await error?.isHidden()).toEqual(false)
  }

  async expectFileSelectionErrorHidden() {
    const error = this.page.locator(this.fileSelectionErrorLocator)
    expect(await error?.isHidden()).toEqual(true)
  }

  async expectNorthStarNoFileSelectedErrorShown() {
    const error = this.page.locator(this.fileSelectionErrorLocator)
    expect(await error?.isHidden()).toEqual(false)
  }

  async expectNorthStarNoFileSelectedErrorHidden() {
    const error = this.page.locator(this.fileSelectionErrorLocator)
    expect(await error?.isHidden()).toEqual(true)
  }

  async expectFileTooLargeErrorShown() {
    const error = this.page.locator(this.fileTooLargeErrorLocator)
    expect(await error?.isHidden()).toEqual(false)
  }

  async expectFileTooLargeErrorHidden() {
    const error = this.page.locator(this.fileTooLargeErrorLocator)
    expect(await error?.isHidden()).toEqual(true)
  }

  async expectFileNameDisplayed(fileName: string) {
    expect(await this.page.innerHTML('body')).toContain(fileName)
  }

  async expectFileNameCount(fileName: string, count: number) {
    await expect(
      this.page
        .getByRole('list', {name: 'Uploaded files'})
        .locator('li')
        .filter({hasText: fileName}),
    ).toHaveCount(count)
  }

  async expectHasSubmitButton() {
    expect(this.page.locator("button[type='submit']"))
  }

  async expectHasSkipButton() {
    expect(await this.page.locator(this.skipButtonLocator).count()).toEqual(1)
  }

  async expectNoSkipButton() {
    expect(await this.page.locator(this.skipButtonLocator).count()).toEqual(0)
  }

  async expectFileInputEnabled() {
    await expect(
      this.page.getByLabel('Drag file here or choose from folder'),
    ).toBeEnabled()
  }

  async expectFileInputDisabled() {
    await expect(
      this.page.getByLabel('Drag file here or choose from folder'),
    ).toBeDisabled()
  }

  async clickSkip() {
    await this.page.locator(this.skipButtonLocator).click()
  }

  async expectHasContinueButton() {
    expect(await this.page.locator(this.continueButtonLocator).count()).toEqual(
      1,
    )
  }

  async expectNoContinueButton() {
    expect(await this.page.locator(this.continueButtonLocator).count()).toEqual(
      0,
    )
  }

  // In North Star, the Continue button has form="cf-fileupload-continue-form".
  async expectHasContinueForm() {
    expect(await this.page.locator(this.continueFormLocator).count()).toEqual(1)
  }

  async expectNoContinueForm() {
    expect(await this.page.locator(this.continueFormLocator).count()).toEqual(0)
  }

  async clickContinue() {
    await this.page.locator(this.continueButtonLocator).click()
  }

  async expectHasDeleteButton() {
    expect(await this.page.locator(this.deleteButtonLocator).count()).toEqual(1)
  }

  async expectNoDeleteButton() {
    expect(await this.page.locator(this.deleteButtonLocator).count()).toEqual(0)
  }

  async clickDelete() {
    await this.page.locator(this.deleteButtonLocator).click()
  }

  async removeFileUpload(fileName: string) {
    await this.page
      .getByRole('list', {name: 'Uploaded files'})
      .locator('li')
      .filter({hasText: fileName})
      .getByText('Remove File')
      .click()
  }
}
