import { Page } from 'playwright'


export default class AdminPrograms {
  public page!: Page

  constructor(page: Page) {
    this.page = page
  }
}
