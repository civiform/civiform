import {expect} from './civiform_fixtures'
import {Page} from '@playwright/test'

/** Class for working with the file upload question that applicants see. */
export class ApplicantFileQuestion {
  private fileSelectionErrorLocator = '[data-fileupload-error="required"]'
  private fileTooLargeErrorLocator = '[data-fileupload-error="too-large"]'
  private continueButtonLocator = '#fileupload-continue-button'
  private continueFormLocator = '#cf-fileupload-continue-form'
  private skipButtonLocator = '#fileupload-skip-button'
  private deleteButtonLocator = '#fileupload-delete-button'
  private questionErrorLocator = '.cf-question-error-message'

  private page!: Page

  constructor(page: Page) {
    this.page = page
  }

  async expectQuestionErrorShown() {
    await expect(this.page.locator(this.questionErrorLocator)).toBeVisible()
  }

  async expectQuestionErrorHidden() {
    await expect(this.page.locator(this.questionErrorLocator)).toBeHidden()
  }

  async expectFileSelectionErrorHidden() {
    await expect(this.page.locator(this.fileSelectionErrorLocator)).toBeHidden()
  }

  async expectFileTooLargeErrorShown() {
    await expect(this.page.locator(this.fileTooLargeErrorLocator)).toBeVisible()
  }

  async expectFileTooLargeErrorHidden() {
    await expect(this.page.locator(this.fileTooLargeErrorLocator)).toBeHidden()
  }

  async expectLegacyFileTooLargeErrorShown() {
    await expect(
      this.page.locator('#cf-fileupload-too-large-error'),
    ).toBeVisible()
  }

  async expectLegacyFileTooLargeErrorHidden() {
    await expect(
      this.page.locator('#cf-fileupload-too-large-error'),
    ).toBeHidden()
  }

  async expectFileNameDisplayed(fileName: string) {
    await expect(this.page.locator('body')).toContainText(fileName)
  }

  async expectFileNameCount(fileName: string, count: number) {
    await expect(
      this.page
        .getByRole('list', {name: 'Uploaded files'})
        .locator('li')
        .filter({hasText: fileName}),
    ).toHaveCount(count)
  }

  async expectHasSkipButton() {
    await expect(this.page.locator(this.skipButtonLocator)).toBeVisible()
  }

  async expectNoSkipButton() {
    await expect(this.page.locator(this.skipButtonLocator)).toBeHidden()
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

  async expectFileInputShowsValidationError() {
    const container = this.page.locator('[data-cf-file-upload-container]')
    const fileInput = container.locator('input[type=file]')
    const validationErrors = container.locator(
      '[id^="cf-fileupload-validation-errors-"]',
    )
    const validationErrorsId = await validationErrors.getAttribute('id')
    await expect(fileInput).toHaveAttribute('aria-invalid', 'true')
    await expect(fileInput).toHaveAttribute(
      'aria-describedby',
      new RegExp(validationErrorsId ?? ''),
    )
    await expect(container).toHaveClass(/cf-question-field-with-error/)
  }

  async clickSkip() {
    await this.page.locator(this.skipButtonLocator).click()
  }

  async expectHasContinueButton() {
    await expect(this.page.locator(this.continueButtonLocator)).toBeVisible()
  }

  async expectNoContinueButton() {
    await expect(this.page.locator(this.continueButtonLocator)).toBeHidden()
  }

  // The Continue button has form="cf-fileupload-continue-form".
  async expectHasContinueForm() {
    await expect(this.page.locator(this.continueFormLocator)).toBeAttached()
  }

  async expectNoContinueForm() {
    await expect(this.page.locator(this.continueFormLocator)).not.toBeAttached()
  }

  async clickContinue() {
    await this.page.locator(this.continueButtonLocator).click()
  }

  async expectHasDeleteButton() {
    await expect(this.page.locator(this.deleteButtonLocator)).toBeVisible()
  }

  async expectNoDeleteButton() {
    await expect(this.page.locator(this.deleteButtonLocator)).toBeHidden()
  }

  async clickDelete() {
    await this.page.locator(this.deleteButtonLocator).click()
  }

  /**
   * @deprecated To be removed when client side file upload end of life
   */
  async removeFileUploadLegacy(fileName: string) {
    await this.page
      .getByRole('list', {name: 'Uploaded files'})
      .locator('li')
      .filter({hasText: fileName})
      .getByText('Remove File')
      .click()
  }

  async removeFileUpload(fileName: string) {
    await this.page
      .getByRole('list', {name: 'Uploaded files'})
      .locator('li')
      .filter({hasText: fileName})
      .getByRole('button', {name: `Remove ${fileName} file`})
      .click()
  }
}
