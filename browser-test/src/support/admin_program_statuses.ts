import {expect} from './civiform_fixtures'
import {Page, Locator} from '@playwright/test'
import {dismissModal, waitForAnyModalLocator, waitForPageJsLoad} from './wait'

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
    await expect(statusLocator).toBeVisible()
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

  async expectStatusIsDefault(statusName: string) {
    const statusLocator = this.page.locator(
      this.programStatusItemSelector(statusName),
    )
    await expect(statusLocator).toBeVisible()
    expect(await statusLocator.innerText()).toContain('Default status')
  }

  async expectStatusIsNotDefault(statusName: string) {
    const statusLocator = this.page.locator(
      this.programStatusItemSelector(statusName),
    )
    await expect(statusLocator).toBeVisible()
    expect(await statusLocator.innerText()).not.toContain('Default status')
  }

  async expectStatusNotExists(statusName: string) {
    const statusLocator = this.page.locator(
      this.programStatusItemSelector(statusName),
    )
    await expect(statusLocator).toBeHidden()
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

    const modal = await waitForAnyModalLocator(this.page)
    await expect(modal).toContainText('Create a new status')

    emailBody = emailBody ?? ''
    await this.fillStatusUpdateModalValuesAndSubmit(modal, {
      statusName,
      emailBody,
    })
    await waitForPageJsLoad(this.page)
  }

  // Creates an initial status, and sets it as the default status.
  async createInitialDefaultStatus(statusName: string) {
    const confirmHandle =
      await this.createDefaultStatusWithoutClickingConfirm(statusName)
    this.acceptDialogWithMessage(this.newDefaultStatusMessage(statusName))
    await confirmHandle.click()
  }

  async createDefaultStatusWithoutClickingConfirm(
    statusName: string,
  ): Promise<Locator> {
    await this.page.click('button:has-text("Create a new status")')

    const modal = await waitForAnyModalLocator(this.page)
    await expect(modal).toContainText('Create a new status')

    const statusFieldHandle = modal.locator('text="Status name (required)"')
    await statusFieldHandle.fill(statusName)

    const defaultCheckboxHandle = modal.locator(
      'input[name="defaultStatusCheckbox"]',
    )
    await defaultCheckboxHandle.check()

    return modal.locator('button:has-text("Confirm")')
  }

  acceptDialogWithMessage(message?: string, dialogType = 'confirm') {
    this.page.once('dialog', (dialog) => {
      void dialog.accept()
      expect(dialog.type()).toEqual(dialogType)
      if (message) {
        expect(dialog.message()).toEqual(message)
      }
    })
  }

  dismissDialogWithMessage(message: string, dialogType = 'confirm') {
    this.page.once('dialog', (dialog) => {
      void dialog.dismiss()
      expect(dialog.type()).toEqual(dialogType)
      expect(dialog.message()).toEqual(message)
    })
  }

  newDefaultStatusMessage(statusName: string) {
    return `The default status will be updated to ${statusName}. Are you sure?`
  }

  changeDefaultStatusMessage(oldDefault: string, newDefault: string) {
    return `The default status will be updated from ${oldDefault} to ${newDefault}. Are you sure?`
  }

  defaultStatusUpdateToastMessage(statusName: string) {
    return `${statusName} has been updated to the default status`
  }

  async editStatusDefault(
    statusName: string,
    defaultChecked: boolean,
    dialogMessage?: string,
  ) {
    await this.page.click(
      this.programStatusItemSelector(statusName) + ' button:has-text("Edit")',
    )
    const modal = await waitForAnyModalLocator(this.page)
    await expect(modal).toContainText('Edit this status')

    const defaultCheckboxHandle = modal.locator(
      'input[name="defaultStatusCheckbox"]',
    )

    if (defaultChecked) {
      await defaultCheckboxHandle.check()
    } else {
      await defaultCheckboxHandle.uncheck()
    }

    const confirmHandle = modal.locator('button:has-text("Confirm")')

    if (defaultChecked) {
      this.acceptDialogWithMessage(dialogMessage)
    }

    await confirmHandle.click()
    await waitForPageJsLoad(this.page)
  }

  async expectCreateStatusModalWithError(expectErrorContains: string) {
    const modal = await waitForAnyModalLocator(this.page)
    await expect(modal).toContainText('Create a new status')
    await expect(modal).toContainText(expectErrorContains)
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

    const modal = await waitForAnyModalLocator(this.page)
    await expect(modal).toContainText('Edit this status')

    await this.fillStatusUpdateModalValuesAndSubmit(modal, {
      statusName: editedStatusName,
      emailBody: editedEmailBody,
    })
    await waitForPageJsLoad(this.page)
  }

  async expectEditStatusModalWithError(expectErrorContains: string) {
    const modal = await waitForAnyModalLocator(this.page)
    expect(await modal.innerText()).toContain('Edit this status')
    if (expectErrorContains) {
      expect(await modal.innerText()).toContain(expectErrorContains)
    }
  }

  async deleteStatus(statusName: string) {
    await this.page.click(
      this.programStatusItemSelector(statusName) + ' button:has-text("Delete")',
    )
    const modal = await waitForAnyModalLocator(this.page)

    const deleteHandle = modal.locator('button:has-text("Delete")')
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

    const modal = await waitForAnyModalLocator(this.page)
    await expect(modal).toContainText('Edit this status')

    // We perform selectors within the modal since using the typical
    // selectors will match multiple modals on the page.
    const emailBody = modal.getByText(
      'Email the applicant about the status change',
    )

    // Close the modal prior to any assertions to avoid affecting
    // subsequent tests.
    await expect(emailBody).toHaveValue(expectedEmailBody)
    await dismissModal(this.page)
  }

  async expectEmailTranslationWarningVisibility(
    statusName: string,
    visible: boolean,
  ) {
    await this.page.click(
      this.programStatusItemSelector(statusName) + ' button:has-text("Edit")',
    )

    const modal = await waitForAnyModalLocator(this.page)
    await expect(modal).toContainText('Edit this status')

    // Close the modal prior to any assertions to avoid affecting subsequent tests.
    await expect(
      modal.getByText(
        'clearing the email body will also clear any associated translations',
      ),
    ).toBeVisible({visible: visible})
    await dismissModal(this.page)
  }

  async expectProgramManageStatusesPage(programName: string) {
    expect(await this.page.innerText('h1')).toContain(
      `Manage application statuses for ${programName}`,
    )
  }

  private async fillStatusUpdateModalValuesAndSubmit(
    modal: Locator,
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
    const statusFieldHandle = modal.locator('text="Status name (required)"')
    await statusFieldHandle.fill(statusName)

    const emailFieldHandle = modal.locator(
      'text="Email the applicant about the status change"',
    )
    await emailFieldHandle.fill(emailBody || '')

    const confirmHandle = modal.locator('button:has-text("Confirm")')
    await confirmHandle.click()
  }

  private programStatusItemSelector(statusName: string): string {
    return `.cf-admin-program-status-item:has(:text("${statusName}"))`
  }
}
