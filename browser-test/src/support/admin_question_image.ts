import {Buffer} from 'buffer'
import {expect} from './civiform_fixtures'
import {Page} from '@playwright/test'
import {waitForHtmxReady, waitForPageJsLoad} from './wait'
import {readFileSync, writeFileSync, unlinkSync} from 'fs'

export class AdminQuestionImage {
  private page: Page

  constructor(page: Page) {
    this.page = page
  }

  getImageUploadInput() {
    return this.page.locator('input[type="file"]')
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
      .filter({hasText: 'A file is currently uploaded for this question'})
  }

  getUpdateButton() {
    return this.page.getByRole('button', {name: 'Update'})
  }

  getImageUploadError() {
    return this.page.getByRole('alert', {name: 'Image upload error'})
  }

  async expectNoUploadError() {
    await expect(this.getImageUploadError()).toBeHidden()
  }

  async expectUploadError(errorMessage: string) {
    await expect(this.getImageUploadError()).toBeVisible()
    await expect(this.getImageUploadError()).toContainText(errorMessage)
  }

  getChangeFileText() {
    return this.page.getByText('Change file')
  }

  async expectChangeFileVisible() {
    await expect(this.getChangeFileText()).toBeVisible()
    await expect(this.page.getByLabel('Change file')).toBeAttached()
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
    await waitForHtmxReady(this.page)
    await this.expectDeleteButtonVisible()
  }

  async uploadImageFileWithMbSize(mbSize: number) {
    const filePath = `image-size-${mbSize}-mb.png`
    const baseImage = readFileSync('src/assets/program-summary-image-wide.png')
    const targetSize = Math.floor(mbSize * 1024 * 1024)
    const padding = Buffer.alloc(Math.max(0, targetSize - baseImage.length))
    writeFileSync(filePath, Buffer.concat([baseImage, padding]))
    try {
      await this.setImageFile(filePath)
    } finally {
      try {
        unlinkSync(filePath)
      } catch {
        // file cleanup
      }
    }
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

  async expectExistingImageFileName(fileName: string) {
    await expect(this.getExistingAlert()).toContainText(fileName)
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

  getPreviewImageContainer() {
    return this.page.getByTestId('question-image-container')
  }

  getPreviewImage(altText?: string) {
    return altText
      ? this.getPreviewImageContainer().getByRole('img', {name: altText})
      : this.getPreviewImageContainer().getByRole('img')
  }

  async expectPreviewImageWithAltText(altText: string) {
    const previewContainer = this.getPreviewImageContainer()
    await expect(previewContainer).toBeVisible()
    await expect(this.getPreviewImage(altText)).toBeVisible()
    const descriptionText = altText.endsWith('.') ? altText : `${altText}.`
    await expect(previewContainer).toContainText(descriptionText)
  }

  async expectNoPreviewImage() {
    await expect(this.getPreviewImageContainer()).toBeHidden()
  }

  getPreviewImageFrameLink(altText?: string) {
    const previewContainer = this.getPreviewImageContainer()
    return altText
      ? previewContainer.getByRole('link', {name: altText})
      : previewContainer.getByRole('link').first()
  }

  async expectPreviewImageAccessibleToScreenReader(altText: string) {
    const previewContainer = this.getPreviewImageContainer()
    const frameLink = this.getPreviewImageFrameLink(altText)

    // The image link must NOT have an aria-label, so the image alt text is used as the accessible name
    await expect(frameLink).not.toHaveAttribute('aria-label')

    // Link's accessible name should be the image's alt text
    await expect(frameLink).toBeVisible()

    // The image inside has the alt text
    await expect(this.getPreviewImage(altText)).toBeVisible()

    // The follow-up enlarge link is also present
    const enlargeLink = previewContainer.getByRole('link', {
      name: 'Click to enlarge',
    })
    await expect(enlargeLink).toBeVisible()
    await expect(enlargeLink).toContainText('Click to enlarge')
  }
}
