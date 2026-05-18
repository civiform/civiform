import {expect} from '../support/civiform_fixtures'
import {Page} from '@playwright/test'
import {waitForPageJsLoad, waitForHtmxReady} from './wait'
import {dismissToast} from '.'

export class AdminProgramImage {
  // These values should be kept in sync with views/admin/programs/ProgramImageView
  // and views/admin/programs/ProgramImageFragment.html
  private imageUploadLocator = 'input[type=file]'
  private imageDescriptionLocator = 'input[name="summaryImageDescription"]'
  private imageUploadSubmitButtonLocator =
    'button[form=image-file-upload-form][type="submit"]'
  private legacyImageDescriptionSubmitButtonLocator =
    'button[form=image-description-form][type="submit"]'
  private imageDescriptionSubmitButtonLocator = '#continue-button'
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

  async setImageDescription(description: string) {
    await this.page.fill(this.imageDescriptionLocator, description)
  }

  async submitImageDescription() {
    await this.page.click(this.imageDescriptionSubmitButtonLocator)
    await waitForPageJsLoad(this.page)
  }

  /**
   * @deprecated
   */
  async legacySubmitImageDescription() {
    await this.page.click(this.legacyImageDescriptionSubmitButtonLocator)
    await waitForPageJsLoad(this.page)
  }

  async setImageDescriptionAndSubmit(description: string) {
    await this.setImageDescription(description)
    await this.submitImageDescription()
  }

  /**
   * @deprecated
   * @param description
   */
  async legacySetImageDescriptionAndSubmit(description: string) {
    await this.setImageDescription(description)
    await this.legacySubmitImageDescription()
  }

  async setImageFileFromAssets(fileName: string) {
    await this.page.setInputFiles('input[type=file]', 'src/assets/' + fileName)
    await waitForHtmxReady(this.page)
    await this.page.waitForTimeout(1000)
  }

  /**
   * @deprecated
   * Sets the given image file on the file <input> element.
   *
   * @param {string} imageFileName specifies a path to the image file.
   *   If this string is empty, the file currently set on the element
   *   will be removed.
   */
  async setImageFile(imageFileName: string) {
    const currentDescription = await this.page
      .locator(this.imageDescriptionLocator)
      .inputValue()
    if (currentDescription == '') {
      // A description has to be set before an image can be uploaded
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

  /**
   * @deprecated
   * @param imageFileName
   */
  async setImageFileAndSubmit(imageFileName: string) {
    await this.setImageFile(imageFileName)
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

  async expectDisabledImageDescriptionSubmit() {
    await expect(
      this.page.locator(this.imageDescriptionSubmitButtonLocator),
    ).toBeDisabled()
  }

  async expectEnabledImageDescriptionSubmit() {
    await expect(
      this.page.locator(this.imageDescriptionSubmitButtonLocator),
    ).toBeEnabled()
  }

  /**
   * @deprecated
   */
  async legacyExpectDisabledImageDescriptionSubmit() {
    await expect(
      this.page.locator(this.legacyImageDescriptionSubmitButtonLocator),
    ).toBeDisabled()
  }

  /**
   * @deprecated
   */
  async legacyExpectEnabledImageDescriptionSubmit() {
    await expect(
      this.page.locator(this.legacyImageDescriptionSubmitButtonLocator),
    ).toBeEnabled()
  }

  async expectDisabledImageFileUploadSubmit() {
    await expect(
      this.page.locator(this.imageUploadSubmitButtonLocator),
    ).toBeDisabled()
  }

  async expectEnabledImageFileUploadSubmit() {
    await expect(
      this.page.locator(this.imageUploadSubmitButtonLocator),
    ).toBeEnabled()
  }

  async expectDisabledImageFileUpload() {
    await expect(this.page.locator(this.imageUploadLocator)).toBeDisabled()
  }

  async expectEnabledImageFileUpload() {
    await expect(this.page.locator(this.imageUploadLocator)).toBeEnabled()
  }

  async expectTooLargeErrorShown() {
    await expect(this.page.locator(this.tooLargeErrorLocator)).toBeVisible()
  }

  async expectTooLargeErrorHidden() {
    await expect(this.page.locator(this.tooLargeErrorLocator)).toBeHidden()
  }

  async expectAltTextRequiredClientErrorVisible() {
    await expect(
      this.page.locator('#cf-program-image-alt-required-error'),
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

  async expectDisabledTranslationButton() {
    await expect(
      this.page.locator(this.translationsButtonLocator),
    ).toBeDisabled()
  }

  async expectEnabledTranslationButton() {
    await expect(
      this.page.locator(this.translationsButtonLocator),
    ).toBeEnabled()
  }

  async clickTranslationButton() {
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
    return `Image is saved with the description: ${description}`
  }

  /**
   * @deprecated
   * @param description
   */
  legacyDescriptionUpdatedToastMessage(description: string): string {
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
