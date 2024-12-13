import {expect} from '@playwright/test'
import {Page} from 'playwright'
import {waitForPageJsLoad} from './wait'
import {dismissToast, expectDisabled, expectEnabled} from '.'

export class AdminProgramImage {
  // These values should be kept in sync with views/admin/programs/ProgramImageView.java.
  private imageUploadLocator = 'input[type=file]'
  private imageDescriptionLocator = 'input[name="summaryImageDescription"]'
  private imageUploadSubmitButtonLocator =
    'button[form=image-file-upload-form][type="submit"]'
  private imageDescriptionSubmitButtonLocator =
    'button[form=image-description-form][type="submit"]'
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
    expect(await this.page.locator(this.continueButtonLocator).count()).toEqual(
      1,
    )
  }

  async expectNoContinueButton() {
    expect(await this.page.locator(this.continueButtonLocator).count()).toEqual(
      0,
    )
  }

  async setImageDescription(description: string) {
    await this.page.fill(this.imageDescriptionLocator, description)
  }

  async submitImageDescription() {
    await this.page.click(this.imageDescriptionSubmitButtonLocator)
    await waitForPageJsLoad(this.page)
  }

  async setImageDescriptionAndSubmit(description: string) {
    await this.setImageDescription(description)
    await this.submitImageDescription()
  }

  /*
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
      await this.setImageDescriptionAndSubmit('desc')
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
    await expectDisabled(this.page, this.imageDescriptionSubmitButtonLocator)
  }

  async expectEnabledImageDescriptionSubmit() {
    await expectEnabled(this.page, this.imageDescriptionSubmitButtonLocator)
  }

  async expectDisabledImageFileUploadSubmit() {
    await expectDisabled(this.page, this.imageUploadSubmitButtonLocator)
  }

  async expectEnabledImageFileUploadSubmit() {
    await expectEnabled(this.page, this.imageUploadSubmitButtonLocator)
  }

  async expectDisabledImageFileUpload() {
    await expectDisabled(this.page, this.imageUploadLocator)
  }

  async expectEnabledImageFileUpload() {
    await expectEnabled(this.page, this.imageUploadLocator)
  }

  async expectTooLargeErrorShown() {
    await expect(this.page.locator(this.tooLargeErrorLocator)).toBeVisible()
  }

  async expectTooLargeErrorHidden() {
    await expect(this.page.locator(this.tooLargeErrorLocator)).toBeHidden()
  }

  /** Expects that the program card preview does not contain an image. */
  async expectNoImagePreview() {
    expect(
      await this.page.locator('.cf-application-card').locator('img').count(),
    ).toEqual(0)
  }

  /** Expects that the program card preview contains an image. */
  async expectImagePreview() {
    expect(
      await this.page.locator('.cf-application-card').locator('img').count(),
    ).toEqual(1)
  }

  async expectDisabledTranslationButton() {
    await expectDisabled(this.page, this.translationsButtonLocator)
  }

  async expectEnabledTranslationButton() {
    await expectEnabled(this.page, this.translationsButtonLocator)
  }

  async clickTranslationButton() {
    await this.page.click(this.translationsButtonLocator)
  }

  async expectProgramPreviewCard(
    programName: string,
    programDescription: string,
  ) {
    await expect(this.page.getByText(programName)).toBeVisible()
    await expect(this.page.getByText(programDescription)).toBeVisible()
    await expect(this.page.getByText('View and apply')).toBeVisible()
  }

  descriptionUpdatedToastMessage(description: string) {
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
