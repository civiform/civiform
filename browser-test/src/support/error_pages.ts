import { Page } from 'playwright'

import { BASE_URL } from './'

export class NotFoundPage {
  public page!: Page

  constructor(page: Page) {
    this.page = page
  }

  async gotoInvalidPage(page: Page) {
    // Use /dev/abc for now till I get error handler figured out
    return await page.goto(BASE_URL + "/dev/abc");
  }

  async checkPageHeaderEnUS() {
    expect(await this.page.innerText('h1')).toContain("We're sorry, we lost the page");
  }
}
