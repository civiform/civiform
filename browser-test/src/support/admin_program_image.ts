import {Page} from 'playwright'

export class AdminProgramImage {
  private imageDescriptionLocator = 'label:has-text("Image description")'

  private page!: Page

  constructor(page: Page) {
    this.page = page
  }

  async setImageDescriptionAndSubmit(description: string) {
    await this.page.fill(this.imageDescriptionLocator, description)
    await this.page.click('button:has-text("Save description")')
  }

  async expectProgramImagePage(programName: string) {
    expect(await this.page.innerText('h1')).toContain(
      `Manage program image for ${programName}`,
    )
  }

  async expectDescriptionIs(description: string) {
    const descriptionElement = this.page.locator(this.imageDescriptionLocator)
    expect(await descriptionElement.innerText()).toContain(description)
  }

  descriptionUpdatedToastMessage(description: string) {
    return `Image description set to ${description}`
  }

  descriptionClearedToastMessage() {
    return `Image description removed`
  }
}
