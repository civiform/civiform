import {expect} from '../support/civiform_fixtures'
import {Page} from '@playwright/test'
import {waitForPageJsLoad} from './wait'
import {dismissToast} from '.'

export class AdminProgramImage {
  // These values should be kept in sync with views/admin/programs/ProgramImageView
  // and views/admin/programs/ProgramImageFragment.html
  private imageUploadLocator = 'input[type=file]'
  private imageDescriptionLocator = 'input[name="summaryImageDescription"]'
  private imageUploadSubmitButtonLocator =
    'button[form=image-file-upload-form][type="submit"]'
  private imageDescriptionSubmitButtonLocator =
    'button[form=image-description-form][type="submit"]'
  private programImageFormSubmitButtonLocator = '#save-button'
  private translationsButtonLocator = 'button:has-text("Manage translations")'
  private continueButtonLocator = '#continue-button'
  // This should be kept in sync with views/fileupload/FileUploadViewStrategy#createFileTooLargeError.
  private tooLargeErrorLocator = '#cf-fileupload-too-large-error'

  private page!: Page

  constructor(page: Page) {
    this.page = page
  }

  async clickBackButton() {
    await this.page.click('a:has-text("Back")')
  }

  async clickContinueButton() {
    await this.page.click(this.continueButtonLocator)
  }

  async submitProgramImageForm() {
    await this.page.click(this.programImageFormSubmitButtonLocator)
    await waitForPageJsLoad(this.page)
  }

  async expectHasContinueButton() {
    await expect(this.page.locator(this.continueButtonLocator)).toHaveCount(1)
  }

  async expectContinueButtonText(expectedText: string) {
    await expect(this.page.locator(this.continueButtonLocator)).toHaveText(
      expectedText,
    )
  }

  async expectNoContinueButton() {
    await expect(this.page.locator(this.continueButtonLocator)).toHaveCount(0)
  }

  async expectHasSaveButton() {
    await expect(
      this.page.locator(this.programImageFormSubmitButtonLocator),
    ).toHaveCount(1)
    await expect(
      this.page.locator(this.programImageFormSubmitButtonLocator),
    ).toHaveText('Save')
  }

  async expectOnProgramImagePageWithEditStatus(
    editStatus: 'CREATION' | 'CREATION_EDIT' | 'EDIT',
  ) {
    await expect(this.page).toHaveURL(new RegExp(`/image/${editStatus}`))
    await this.expectProgramImagePage()
  }

  async setImageDescription(description: string) {
    await this.page.fill(this.imageDescriptionLocator, description)
  }

  async submitImageDescription() {
    await this.submitProgramImageForm()
    await waitForPageJsLoad(this.page)
  }

  async setImageDescriptionAndSubmit(description: string) {
    await this.setImageDescription(description)
    await this.submitImageDescription()
  }

  async setImageFileAndSubmit(imagePath: string, description = 'desc') {
    await this.setImageDescription(description)
    await this.setImageFile(imagePath)
    await this.submitProgramImageForm()
    await waitForPageJsLoad(this.page)
  }

  /** Selects a file without saving. */
  async setImageFile(imagePath: string) {
    await this.page.setInputFiles(this.imageUploadLocator, imagePath)
  }

  /** @deprecated Use {@link submitImageDescription} for the improved program image form. */
  async legacySubmitImageDescription() {
    await this.page.click(this.imageDescriptionSubmitButtonLocator)
    await waitForPageJsLoad(this.page)
  }

  /** @deprecated Use {@link setImageDescriptionAndSubmit} for the improved program image form. */
  async legacySetImageDescriptionAndSubmit(description: string) {
    await this.setImageDescription(description)
    await this.legacySubmitImageDescription()
  }

  /**
   * @deprecated Use {@link setImageFile} for the improved program image form.
   * Sets the given image file on the file <input> element for the legacy program image page.
   *
   * @param {string} imageFileName specifies a path to the image file.
   *   If this string is empty, the file currently set on the element
   *   will be removed.
   */
  async legacySetImageFile(imageFileName: string) {
    const currentDescription = await this.page
      .locator(this.imageDescriptionLocator)
      .inputValue()
    if (currentDescription == '') {
      await this.legacySetImageDescriptionAndSubmit('desc')
      await dismissToast(this.page)
    }

    if (imageFileName !== '') {
      await this.page
        .locator(this.imageUploadLocator)
        .setInputFiles(imageFileName)
    } else {
      await this.page.locator(this.imageUploadLocator).setInputFiles([])
    }
  }

  /** @deprecated Use {@link setImageFileAndSubmit} for the improved program image form. */
  async legacySetImageFileAndSubmit(imageFileName: string) {
    await this.legacySetImageFile(imageFileName)
    await this.page.click(this.imageUploadSubmitButtonLocator)
    await waitForPageJsLoad(this.page)
  }

  /**
   * Clicks the "Delete image" button on the main program image edit page,
   * which will bring up the confirmation modal.
   * {@link confirmDeleteImageButton} will need to be used afterwards to
   * actually delete the image.
   */
  async clickDeleteImageButton() {
    await this.page.click('button:has-text("Delete image")')
  }

  /**
   * Clicks the "Delete image" button in the confirmation modal.
   *
   * {@link clickDeleteImageButton} should be called first.
   */
  async confirmDeleteImageButton() {
    await this.page.click('button[type="submit"]:has-text("Delete image")')
  }

  async expectProgramImagePage() {
    expect(await this.page.innerText('h1')).toContain(`Image upload`)
  }

  async expectDescriptionIs(description: string) {
    const descriptionElement = this.page.locator(this.imageDescriptionLocator)
    await expect(descriptionElement).toHaveValue(description)
  }

  /** @deprecated Use {@link expectDisabledProgramImageFormSubmit} for the improved program image form. */
  async expectDisabledImageDescriptionSubmit() {
    await expect(
      this.page.locator(this.imageDescriptionSubmitButtonLocator),
    ).toBeDisabled()
  }

  /** @deprecated Use {@link expectEnabledProgramImageFormSubmit} for the improved program image form. */
  async legacyExpectEnabledImageDescriptionSubmit() {
    await expect(
      this.page.locator(this.imageDescriptionSubmitButtonLocator),
    ).toBeEnabled()
  }

  async expectDisabledProgramImageFormSubmit() {
    await expect(
      this.page.locator(this.programImageFormSubmitButtonLocator),
    ).toBeDisabled()
  }

  async expectEnabledProgramImageFormSubmit() {
    await expect(
      this.page.locator(this.programImageFormSubmitButtonLocator),
    ).toBeEnabled()
  }

  async expectTooLargeErrorShown() {
    await expect(this.page.locator(this.tooLargeErrorLocator)).toBeVisible()
  }

  async expectAltTextRequiredClientErrorVisible() {
    await expect(
      this.page.locator('#error-message-summaryImageDescription'),
    ).toBeVisible()
  }

  /** Expects that the program card preview does not contain an image. */
  async expectNoImagePreview() {
    await expect(
      this.page.locator('.cf-application-card').locator('img'),
    ).toHaveCount(0)
  }

  /** Expects that the program card preview contains an image. */
  async expectImagePreview() {
    await expect(
      this.page.locator('.cf-application-card').locator('img'),
    ).toHaveCount(1)
  }

  /** @deprecated Translation button is not on the improved program image page. */
  async legacyExpectDisabledTranslationButton() {
    await expect(
      this.page.locator(this.translationsButtonLocator),
    ).toBeDisabled()
  }

  /** @deprecated Translation button is not on the improved program image page. */
  async legacyExpectEnabledTranslationButton() {
    await expect(
      this.page.locator(this.translationsButtonLocator),
    ).toBeEnabled()
  }

  /** @deprecated Translation button is not on the improved program image page. */
  async legacyClickTranslationButton() {
    await this.page.click(this.translationsButtonLocator)
  }

  async expectProgramPreviewCard(
    programName: string,
    programDescription: string,
    shortDescription = '',
  ) {
    await expect(this.page.getByText(programName)).toBeVisible()
    if (shortDescription !== '') {
      // Short description is rendered on the program card if we have it
      await expect(this.page.getByText(shortDescription)).toBeVisible()
    } else {
      await expect(this.page.getByText(programDescription)).toBeVisible()
    }
    await expect(this.page.getByText('View and apply')).toBeVisible()
  }

  descriptionUpdatedToastMessage(description: string): string {
    return `Image description set to ${description}`
  }

  descriptionClearedToastMessage() {
    return `Image description removed`
  }

  descriptionNotClearedToastMessage() {
    return `Description can't be removed because an image is present. Delete the image before deleting the description.`
  }

  imageUpdatedToastMessage() {
    return `Image set`
  }

  imageRemovedToastMessage() {
    return `Image removed`
  }
}
