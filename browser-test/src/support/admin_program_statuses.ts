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

  async expectStatusNotExists(statusName: string) {
    const statusLocator = this.page.locator(
      this.programStatusItemSelector(statusName),
    )
    expect(await statusLocator.isVisible()).toEqual(false)
  }

  async createStatus(statusName: string, emailBody = '') {
    await this.page.click('button:has-text("Create a new status")')

    const modal = await waitForAnyModal(this.page)
    expect(await modal.innerText()).toContain('Create a new status')

    const statusFieldHandle = (await modal.$('text="Status name (required)"'))!
    await statusFieldHandle.fill(statusName)
    const emailFieldHandle = (await modal.$(
      'text="Applicant status change email"',
    ))!
    await emailFieldHandle.fill(emailBody)

    const confirmHandle = (await modal.$('button:has-text("Confirm")'))!
    await confirmHandle.click()
    await waitForPageJsLoad(this.page)
  }

  async expectCreateStatusModalWithError(expectErrorContains: string) {
    const modal = await waitForAnyModal(this.page)
    expect(await modal.innerText()).toContain('Create a new status')
    if (!!expectErrorContains) {
      expect(await modal.innerText()).toContain(expectErrorContains)
    }
  }

  async editStatus(
    originalStatusName: string,
    editedStatusName: string,
    editedEmailBody = '',
  ) {
    await this.page.click(
      this.programStatusItemSelector(originalStatusName) +
        ' button:has-text("Edit")',
    )

    const modal = await waitForAnyModal(this.page)
    expect(await modal.innerText()).toContain('Edit this status')

    const statusFieldHandle = (await modal.$('text="Status name (required)"'))!
    await statusFieldHandle.fill(editedStatusName)
    const emailFieldHandle = (await modal.$(
      'text="Applicant status change email"',
    ))!
    await emailFieldHandle.fill(editedEmailBody)

    const confirmHandle = (await modal.$('button:has-text("Confirm")'))!
    await confirmHandle.click()
    await waitForPageJsLoad(this.page)
  }

  async expectEditStatusModalWithError(expectErrorContains: string) {
    const modal = await waitForAnyModal(this.page)
    expect(await modal.innerText()).toContain('Edit this status')
    if (!!expectErrorContains) {
      expect(await modal.innerText()).toContain(expectErrorContains)
    }
  }

  async deleteStatus(statusName: string) {
    await this.page.click(
      this.programStatusItemSelector(statusName) + ' button:has-text("Delete")',
    )
    const modal = await waitForAnyModal(this.page)

    const deleteHandle = (await modal.$('button:has-text("Delete")'))!
    await deleteHandle.click()
    await waitForPageJsLoad(this.page)
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
