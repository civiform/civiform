import {Page} from 'playwright'
import {waitForPageJsLoad} from './wait'
import {dismissToast} from '.'

export class AdminProgramImage {
  private imageUploadLocator = 'input[type=file]'
  private imageDescriptionLocator = 'input[name="summaryImageDescription"]'
  private translationsButtonLocator = 'button:has-text("Manage translations")'

  private page!: Page

  constructor(page: Page) {
    this.page = page
  }

  async clickBackButton() {
    await this.page.click('a:has-text("Back")')
  }

  async setImageDescriptionAndSubmit(description: string) {
    await this.page.fill(this.imageDescriptionLocator, description)
    await this.page.click('button[form=image-description-form]')
    await waitForPageJsLoad(this.page)
  }

  async setImageFile(imageFileName: string) {
    const currentDescription = await this.page
      .locator(this.imageDescriptionLocator)
      .inputValue()
    if (currentDescription == '') {
      // A description has to be set before an image can be uploaded
      await this.setImageDescriptionAndSubmit('desc')
      await dismissToast(this.page)
    }

    await this.page
      .locator(this.imageUploadLocator)
      .setInputFiles(imageFileName)
  }

  async setImageFileAndSubmit(imageFileName: string) {
    await this.setImageFile(imageFileName)
    await this.page.click('button[form=image-file-upload-form]')
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
    expect(await descriptionElement.inputValue()).toBe(description)
  }

  async expectDisabledImageFileUpload() {
    expect(
      await this.page.getAttribute(this.imageUploadLocator, 'disabled'),
    ).not.toBeNull()
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
    expect(
      await this.page.getAttribute(this.translationsButtonLocator, 'disabled'),
    ).not.toBeNull()
  }

  async expectEnabledTranslationButton() {
    expect(
      await this.page.getAttribute(this.translationsButtonLocator, 'disabled'),
    ).toBeNull()
  }

  async clickTranslationButton() {
    await this.page.click(this.translationsButtonLocator)
  }

  descriptionUpdatedToastMessage(description: string) {
    return `Image description set to ${description}`
  }

  descriptionClearedToastMessage() {
    return `Image description removed`
  }

  imageUpdatedToastMessage() {
    return `Image set`
  }

  imageRemovedToastMessage() {
    return `Image removed`
  }
}
