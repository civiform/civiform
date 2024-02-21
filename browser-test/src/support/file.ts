import {Page} from 'playwright'

export class File {
  private fileSelectionErrorLocator = '.cf-fileupload-error'
  private continueButtonLocator = '#fileupload-continue-button'
  private skipButtonLocator = '#fileupload-skip-button'
  private deleteButtonLocator = '#fileupload-delete-button'

  private page!: Page

  constructor(page: Page) {
    this.page = page
  }

  async expectFileSelectionErrorShown() {
    const error = await this.page.$('.cf-fileupload-error')
    expect(await error?.isHidden()).toEqual(false)
  }

  async expectFileSelectionErrorHidden() {
    const error = await this.page.$('.cf-fileupload-error')
    expect(await error?.isHidden()).toEqual(true)
  }

  async expectFileNameDisplayed(fileName: string) {
    expect(await this.page.innerHTML('body')).toContain(fileName)
  }

  async expectHasSkipButton() {
    expect(await this.page.locator(this.skipButtonLocator).count()).toEqual(1)
  }

  async expectNoSkipButton() {
    expect(await this.page.locator(this.skipButtonLocator).count()).toEqual(0)
  }

  async clickSkip() {
    await this.page.locator(this.skipButtonLocator).click()
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

  async clickContinue() {
    await this.page.locator(this.continueButtonLocator).click()
  }

  async expectDeleteButton() {
    expect(await this.page.locator(this.deleteButtonLocator).count()).toEqual(1)
  }

  async expectNoDeleteButton() {
    expect(await this.page.locator(this.deleteButtonLocator).count()).toEqual(0)
  }

  async clickDelete() {
    await this.page.locator(this.deleteButtonLocator).click()
  }
}
