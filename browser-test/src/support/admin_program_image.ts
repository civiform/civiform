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
    await this.page.click('button:has-text("Save description")')
    await waitForPageJsLoad(this.page)
  }

  async setImageFileAndSubmit() {
    await this.page
      .locator('input[type=file]')
      .setInputFiles('src/assets/program-summary-image.png')
    await this.page.click('button:has-text("Save image")')
    await waitForPageJsLoad(this.page)
  }

  async expectProgramImagePage(programName: string) {
    expect(await this.page.innerText('h1')).toContain(
      `Manage program image for ${programName}`,
    )
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
