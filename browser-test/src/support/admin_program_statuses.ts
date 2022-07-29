import {Page} from 'playwright'

export class AdminProgramStatuses {
  public page!: Page

  constructor(page: Page) {
    this.page = page
  }

  async expectNoStatuses() {
    expect(
      await this.page.innerText('.cf-admin-program-status-list'),
    ).toContain('No statuses have been created yet')
  }
}
