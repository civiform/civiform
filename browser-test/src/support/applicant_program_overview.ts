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

  async startApplicationFromProgramOverviewPage(
    programName: string,
  ): Promise<void> {
    await this.expectProgramOverviewPage(programName)
    await this.page
      .getByRole('link', {name: 'Start an application'})
      .first()
      .click()
  }

  async expectProgramOverviewPage(programName: string): Promise<void> {
    expect(await this.page.title()).toContain('Program Overview')
    await expect(
      this.page.getByRole('heading', {
        name: `Apply for ${programName} program`,
      }),
    ).toBeVisible()
  }

  async startApplicationFromTranslatedProgramOverviewPage(
    pageTitle: string,
    header: string,
    buttonText: string,
  ): Promise<void> {
    await this.expectTranslatedProgramOverviewPage(pageTitle, header)
    await this.page.getByRole('link', {name: buttonText}).first().click()
  }

  async expectTranslatedProgramOverviewPage(pageTitle: string, header: string) {
    expect(await this.page.title()).toContain(pageTitle)
    await expect(
      this.page.getByRole('heading', {
        name: header,
      }),
    ).toBeVisible()
  }

  async expectFirstPageOfApplication(): Promise<void> {
    expect(await this.page.title()).toBe('test — 1 of 2')
    await expect(this.page.getByText('Screen 1')).toBeVisible()
  }
}
