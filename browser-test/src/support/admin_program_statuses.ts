import {ElementHandle, Page} from 'playwright'
import {dismissModal, waitForAnyModal, waitForPageJsLoad} from './wait'

export class AdminProgramStatuses {
  private page!: Page

  constructor(page: Page) {
    this.page = page
  }

  async expectNoStatuses() {
    expect(
      await this.page.innerText('.cf-admin-program-status-list'),
    ).toContain('No statuses have been created yet')
  }

  async expectStatusExists({
    statusName,
    expectEmailExists,
  }: {
    statusName: string
    expectEmailExists: boolean
  }) {
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

  async createStatus(
    statusName: string,
    {
      emailBody,
    }: {
      emailBody?: string
    } = {},
  ) {
    await this.page.click('button:has-text("Create a new status")')

    const modal = await waitForAnyModal(this.page)
    expect(await modal.innerText()).toContain('Create a new status')

    emailBody = emailBody || ''
    await this.fillStatusUpdateModalValuesAndSubmit(modal, {
      statusName,
      emailBody,
    })
    await waitForPageJsLoad(this.page)
  }

  async expectCreateStatusModalWithError(expectErrorContains: string) {
    const modal = await waitForAnyModal(this.page)
    expect(await modal.innerText()).toContain('Create a new status')
    if (expectErrorContains) {
      expect(await modal.innerText()).toContain(expectErrorContains)
    }
  }

  async editStatus(
    originalStatusName: string,
    {
      editedStatusName,
      editedEmailBody = '',
    }: {editedStatusName: string; editedEmailBody?: string},
  ) {
    await this.page.click(
      this.programStatusItemSelector(originalStatusName) +
        ' button:has-text("Edit")',
    )

    const modal = await waitForAnyModal(this.page)
    expect(await modal.innerText()).toContain('Edit this status')

    await this.fillStatusUpdateModalValuesAndSubmit(modal, {
      statusName: editedStatusName,
      emailBody: editedEmailBody,
    })
    await waitForPageJsLoad(this.page)
  }

  async expectEditStatusModalWithError(expectErrorContains: string) {
    const modal = await waitForAnyModal(this.page)
    expect(await modal.innerText()).toContain('Edit this status')
    if (expectErrorContains) {
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

  async expectExistingStatusEmail({
    statusName,
    expectedEmailBody,
  }: {
    statusName: string
    expectedEmailBody: string
  }) {
    await this.page.click(
      this.programStatusItemSelector(statusName) + ' button:has-text("Edit")',
    )

    const modal = await waitForAnyModal(this.page)
    expect(await modal.innerText()).toContain('Edit this status')

    // We perform selectors within the modal since using the typical
    // selectors will match multiple modals on the page.
    const emailFieldHandle = (await modal.$(
      'text="Applicant status change email"',
    ))!
    const emailBody = await emailFieldHandle.inputValue()

    // Close the modal prior to any assertions to avoid affecting
    // subsequent tests.
    await dismissModal(this.page)

    expect(emailBody).toEqual(expectedEmailBody)
  }

  async expectProgramManageStatusesPage(programName: string) {
    expect(await this.page.innerText('h1')).toContain(
      `Manage application statuses for ${programName}`,
    )
  }

  private async fillStatusUpdateModalValuesAndSubmit(
    modal: ElementHandle<HTMLElement>,
    {
      statusName,
      emailBody,
    }: {
      statusName: string
      emailBody: string
    },
  ) {
    // We perform selectors within the modal since using the typical
    // page.fill with a selector will match multiple modals on the page.
    const statusFieldHandle = (await modal.$('text="Status name (required)"'))!
    await statusFieldHandle.fill(statusName)
    const emailFieldHandle = (await modal.$(
      'text="Applicant status change email"',
    ))!
    await emailFieldHandle.fill(emailBody || '')

    const confirmHandle = (await modal.$('button:has-text("Confirm")'))!
    await confirmHandle.click()
  }

  private programStatusItemSelector(statusName: string): string {
    return `.cf-admin-program-status-item:has(:text("${statusName}"))`
  }
}
