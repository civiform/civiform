import {expect} from './civiform_fixtures'
import {Page} from '@playwright/test'
import {waitForPageJsLoad} from './wait'

export class AdminQuestionImage {
  private page: Page

  constructor(page: Page) {
    this.page = page
  }

  getImageUploadInput() {
    return this.page.locator('#question-image-input')
  }

  getAltTextInput() {
    return this.page.getByRole('textbox', {
      name: 'Enter image description (Alt Text)',
    })
  }

  getDeleteButton() {
    return this.page.getByRole('button', {name: 'Delete image'})
  }

  getExistingAlert() {
    return this.page
      .getByRole('alert')
      .filter({hasText: 'A file is currently uploaded for this question.'})
  }

  getUpdateButton() {
    return this.page.getByRole('button', {name: 'Update'})
  }

  async setImageDescription(description: string) {
    await this.getAltTextInput().fill(description)
  }

  async clearImageDescription() {
    await this.getAltTextInput().fill('')
  }

  async expectDescription(expectedText: string) {
    await expect(this.getAltTextInput()).toHaveValue(expectedText)
  }

  async setImageFile(imagePath: string) {
    await this.getImageUploadInput().setInputFiles(imagePath)
  }

  async uploadImageFile(imagePath: string) {
    await this.setImageFile(imagePath)
    await this.expectDeleteButtonVisible()
  }

  async clickDeleteImageButton() {
    await this.getDeleteButton().click()
    await this.expectDeleteButtonHidden()
  }

  async submitUpdate() {
    await this.getUpdateButton().click()
    await waitForPageJsLoad(this.page)
  }

  async expectDropzoneEnabled() {
    await expect(this.getImageUploadInput()).toBeEnabled()
  }

  async expectDropzoneDisabled() {
    await expect(this.getImageUploadInput()).toBeDisabled()
  }

  async expectHasExistingImageAlert() {
    await expect(this.getExistingAlert()).toBeVisible()
  }

  async expectNoExistingImageAlert() {
    await expect(this.getExistingAlert()).toBeHidden()
  }

  async expectDeleteButtonVisible() {
    await expect(this.getDeleteButton()).toBeVisible()
  }

  async expectDeleteButtonHidden() {
    await expect(this.getDeleteButton()).toBeHidden()
  }
}
