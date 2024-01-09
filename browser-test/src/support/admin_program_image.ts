import {Page} from 'playwright'
import {waitForPageJsLoad} from './wait'

export class AdminProgramImage {
  private imageDescriptionLocator = 'input[name="summaryImageDescription"]'

  private page!: Page

  constructor(page: Page) {
    this.page = page
  }

  async setImageDescriptionAndSubmit(description: string) {
    await this.page.fill(this.imageDescriptionLocator, description)
    await this.page.click('button[form=image-description-form]')
    await waitForPageJsLoad(this.page)
  }

  async setImageFile(imageFileName: string) {
    await this.page.locator('input[type=file]').setInputFiles(imageFileName)
  }

  async setImageFileAndSubmit(imageFileName: string) {
    await this.setImageFile(imageFileName)
    await this.page.click('button[form=image-file-upload-form]')
    await waitForPageJsLoad(this.page)
  }

  async expectProgramImagePage() {
    expect(await this.page.innerText('h1')).toContain(`Image upload`)
  }

  async expectDescriptionIs(description: string) {
    const descriptionElement = this.page.locator(this.imageDescriptionLocator)
    expect(await descriptionElement.inputValue()).toBe(description)
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
}
