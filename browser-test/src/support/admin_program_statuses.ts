import {Page} from 'playwright'
import {waitForAnyModal, waitForPageJsLoad} from './wait'

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

  async addStatus(statusName: string, emailBody = '') {
    await this.page.click('button:has-text("Create a new status")')
    await waitForAnyModal(this.page)

    await this.page.fill('text="Status name (required)"', statusName)
    await this.page.fill('text="Applicant status change email"', emailBody)

    await this.page.click('button:has-text("Confirm")')
    await waitForPageJsLoad(this.page)
  }

  async expectStatusExists(statusName: string, expectEmailExists: boolean) {
    const statusLocator = this.page.locator(
      this.programStatusItemSelector(statusName),
    )
    expect(await statusLocator.isVisible()).toEqual(true)
    if (expectEmailExists) {
      expect(await statusLocator.innerText()).toContain(
        'Applicant notification email added',
      )
    } else {
      expect(await statusLocator.innerText()).not.toContain(
        'Applicant notification email added',
      )
    }
  }

  programStatusItemSelector(statusName: string): string {
    return `.cf-admin-program-status-item:has(:text("${statusName}"))`
  }

  async expectProgramManageStatusesPage(programName: string) {
    expect(await this.page.innerText('h1')).toContain(
      `Manage application statuses for ${programName}`,
    )
  }
}
