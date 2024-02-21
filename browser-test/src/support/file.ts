import {Page} from 'playwright'

export class File {
  private fileSelectionErrorLocator = '.cf-fileupload-error'

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
}
