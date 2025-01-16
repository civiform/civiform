import {Page, expect} from '@playwright/test'

/**
 * Test support class for interacting with the applicant program overview page
 * @class
 * @param {Page} page the Playwright page object in which to operate against
 */
export class ApplicantProgramOverview {
  public page!: Page

  constructor(page: Page) {
    this.page = page
  }

  async expectProgramOverviewPage() {
    await expect(
      this.page.getByText('Welcome to the program overview page!'),
    ).toBeVisible()
  }
}
