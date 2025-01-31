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

  async expectProgramOverviewPage(programName: string): Promise<void> {
    expect(await this.page.title()).toBe('test - Program Overview')
    await expect(
      this.page.getByRole('heading', {
        name: `Apply for ${programName} program`,
      }),
    ).toBeVisible()
  }

  async expectFirstPageOfApplication(): Promise<void> {
    expect(await this.page.title()).toBe('test — 1 of 2')
    await expect(this.page.getByText('Screen 1')).toBeVisible()
  }

  async expectYouAreEligibleAlert(): Promise<void> {
    await expect(
      this.page
        .getByTestId('eligibility-alert')
        .getByRole('alert')
        .filter({hasText: 'You are likely eligible'}),
    ).toBeVisible()

    await expect(
      this.page
        .getByTestId('eligibility-alert')
        .getByRole('alert')
        .filter({hasText: 'You may not be eligible'}),
    ).toBeHidden()
  }

  async expectYouMayNotBeEligibleAlert(): Promise<void> {
    await expect(
      this.page
        .getByTestId('eligibility-alert')
        .getByRole('alert')
        .filter({hasText: 'You may not be eligible'}),
    ).toBeVisible()

    await expect(
      this.page
        .getByTestId('eligibility-alert')
        .getByRole('alert')
        .filter({hasText: 'You are likely eligible'}),
    ).toBeHidden()
  }

  async expectNoEligibilityAlerts(): Promise<void> {
    await expect(
      this.page.getByTestId('eligibility-alert').getByRole('alert'),
    ).toBeHidden()
  }
}
