import {expect} from './civiform_fixtures'
import {Page} from '@playwright/test'
import {waitForPageJsLoad} from './wait'

export class AdminQuestionImage {
  private imageUploadLocator = '#question-image-input'
  private altTextLocator = '#questionImageDescription'
  private deleteButtonLocator = '#delete-question-image-button'
  private existingAlertLocator = '#existing-image-alert'
  private updateButtonLocator = 'button:has-text("Update")'

  private page: Page

  constructor(page: Page) {
    this.page = page
  }

  async setImageDescription(description: string) {
    await this.page.fill(this.altTextLocator, description)
  }

  async clearImageDescription() {
    await this.page.fill(this.altTextLocator, '')
  }

  async expectDescription(expectedText: string) {
    await expect(this.page.locator(this.altTextLocator)).toHaveValue(
      expectedText,
    )
  }

  async setImageFile(imagePath: string) {
    await this.page.setInputFiles(this.imageUploadLocator, imagePath)
  }

  async uploadImageFile(imagePath: string) {
    await this.setImageFile(imagePath)
    await this.expectDeleteButtonVisible()
  }

  async clickDeleteImageButton() {
    await this.page.click(this.deleteButtonLocator)
    await this.expectDeleteButtonHidden()
  }

  async submitUpdate() {
    await this.page.click(this.updateButtonLocator)
    await waitForPageJsLoad(this.page)
  }

  async expectDropzoneEnabled() {
    await expect(this.page.locator(this.imageUploadLocator)).toBeEnabled()
  }

  async expectDropzoneDisabled() {
    await expect(this.page.locator(this.imageUploadLocator)).toBeDisabled()
  }

  async expectHasExistingImageAlert() {
    await expect(this.page.locator(this.existingAlertLocator)).toBeVisible()
  }

  async expectNoExistingImageAlert() {
    await expect(this.page.locator(this.existingAlertLocator)).toBeHidden()
  }

  async expectDeleteButtonVisible() {
    await expect(this.page.locator(this.deleteButtonLocator)).toBeVisible()
  }

  async expectDeleteButtonHidden() {
    await expect(this.page.locator(this.deleteButtonLocator)).toBeHidden()
  }
}
