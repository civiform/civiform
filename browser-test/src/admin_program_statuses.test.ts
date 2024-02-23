import {
  createTestContext,
  dismissModal,
  loginAsAdmin,
  validateScreenshot,
  validateToastMessage,
} from './support'
import {waitForAnyModal, waitForPageJsLoad} from './support/wait'

describe('modify program statuses', () => {
  const ctx = createTestContext(/* clearDb= */ false)

  beforeEach(async () => {
    const {page} = ctx
    await loginAsAdmin(page)
  })

  describe('statuses list', () => {
    test('creates a new program and has no statuses', async () => {
      const {page, adminPrograms, adminProgramStatuses} = ctx
      // Add a draft program, no questions are needed.
      const programName = 'Test program without statuses'
      await adminPrograms.addProgram(programName)
      await adminPrograms.gotoDraftProgramManageStatusesPage(programName)
      await adminProgramStatuses.expectNoStatuses()
      await validateScreenshot(page, 'status-list-with-no-statuses')
    })
  })

  describe('new status creation', () => {
    const programName = 'Test program create statuses'

    beforeAll(async () => {
      const {page, adminPrograms} = ctx
      await loginAsAdmin(page)
      await adminPrograms.addProgram(programName)
      await adminPrograms.gotoAdminProgramsPage()
    })

    beforeEach(async () => {
      await ctx.adminPrograms.gotoDraftProgramManageStatusesPage(programName)
    })

    test('renders create new status modal', async () => {
      const {page} = ctx
      await page.click('button:has-text("Create a new status")')

      const modal = await waitForAnyModal(page)
      expect(await modal.innerText()).toContain('Create a new status')
      await validateScreenshot(page, 'create-new-status-modal')
    })

    test('creates a new status with no email', async () => {
      const {adminProgramStatuses} = ctx
      await adminProgramStatuses.createStatus('Status with no email')
      await adminProgramStatuses.expectProgramManageStatusesPage(programName)
      await adminProgramStatuses.expectStatusExists({
        statusName: 'Status with no email',
        expectEmailExists: false,
      })
    })

    test('creates a new status with email', async () => {
      const {adminProgramStatuses} = ctx
      await adminProgramStatuses.createStatus('Status with email', {
        emailBody: 'An email',
      })
      await adminProgramStatuses.expectProgramManageStatusesPage(programName)
      await adminProgramStatuses.expectStatusExists({
        statusName: 'Status with email',
        expectEmailExists: true,
      })
    })

    test('fails to create status with an empty name', async () => {
      const {page, adminProgramStatuses} = ctx
      await adminProgramStatuses.createStatus('')
      await adminProgramStatuses.expectProgramManageStatusesPage(programName)
      await adminProgramStatuses.expectCreateStatusModalWithError(
        'This field is required',
      )
      await dismissModal(page)
    })

    test('fails to create status with an existing name', async () => {
      const {page, adminProgramStatuses} = ctx
      await adminProgramStatuses.createStatus('Existing status')
      await adminProgramStatuses.expectProgramManageStatusesPage(programName)
      await adminProgramStatuses.createStatus('Existing status')
      await adminProgramStatuses.expectProgramManageStatusesPage(programName)
      await adminProgramStatuses.expectCreateStatusModalWithError(
        'A status with name Existing status already exists',
      )
      await dismissModal(page)
    })
  })

  describe('edit existing statuses', () => {
    const programName = 'Test program edit statuses'
    const firstStatusName = 'First status'
    const secondStatusName = 'Second status'

    beforeAll(async () => {
      const {page, adminPrograms, adminProgramStatuses} = ctx
      await loginAsAdmin(page)
      await adminPrograms.addProgram(programName)
      await adminPrograms.gotoDraftProgramManageStatusesPage(programName)

      // Create two existing statuses.
      await adminProgramStatuses.createStatus(firstStatusName)
      await adminProgramStatuses.expectProgramManageStatusesPage(programName)
      await adminProgramStatuses.createStatus(secondStatusName)
      await adminProgramStatuses.expectProgramManageStatusesPage(programName)
      await adminProgramStatuses.expectStatusExists({
        statusName: firstStatusName,
        expectEmailExists: false,
      })
      await adminProgramStatuses.expectStatusExists({
        statusName: secondStatusName,
        expectEmailExists: false,
      })
    })

    beforeEach(async () => {
      await ctx.adminPrograms.gotoDraftProgramManageStatusesPage(programName)
    })

    test('renders existing statuses', async () => {
      const {page} = ctx
      await validateScreenshot(page, 'status-list-with-statuses')
    })

    test('fails to edit status when providing an existing status name', async () => {
      const {page, adminProgramStatuses} = ctx
      await adminProgramStatuses.editStatus(firstStatusName, {
        editedStatusName: secondStatusName,
      })
      await adminProgramStatuses.expectProgramManageStatusesPage(programName)
      await adminProgramStatuses.expectEditStatusModalWithError(
        `A status with name ${secondStatusName} already exists`,
      )
      await dismissModal(page)
    })

    test('fails to edit status with an empty name', async () => {
      const {page, adminProgramStatuses} = ctx
      await adminProgramStatuses.editStatus(firstStatusName, {
        editedStatusName: '',
      })
      await adminProgramStatuses.expectProgramManageStatusesPage(programName)
      await adminProgramStatuses.expectEditStatusModalWithError(
        'This field is required',
      )
      await dismissModal(page)
    })

    test('edits an existing status name', async () => {
      const {adminProgramStatuses} = ctx
      await adminProgramStatuses.editStatus(secondStatusName, {
        editedStatusName: 'Updated status name',
      })
      await adminProgramStatuses.expectProgramManageStatusesPage(programName)
      await adminProgramStatuses.expectStatusExists({
        statusName: 'Updated status name',
        expectEmailExists: false,
      })
      await adminProgramStatuses.expectStatusNotExists(secondStatusName)
      const emailWarningVisible =
        await adminProgramStatuses.emailTranslationWarningIsVisible(
          'Updated status name',
        )
      expect(emailWarningVisible).toBe(false)
    })

    test('edits an existing status, configures email, and deletes the configured email', async () => {
      const {adminProgramStatuses} = ctx
      await adminProgramStatuses.editStatus(firstStatusName, {
        editedStatusName: firstStatusName,
        editedEmailBody: 'An email body',
      })
      await adminProgramStatuses.expectProgramManageStatusesPage(programName)
      await adminProgramStatuses.expectStatusExists({
        statusName: firstStatusName,
        expectEmailExists: true,
      })
      await adminProgramStatuses.expectExistingStatusEmail({
        statusName: firstStatusName,
        expectedEmailBody: 'An email body',
      })
      const emailWarningVisible =
        await adminProgramStatuses.emailTranslationWarningIsVisible(
          firstStatusName,
        )
      expect(emailWarningVisible).toBe(true)

      // Edit the configured email.
      await adminProgramStatuses.editStatus(firstStatusName, {
        editedStatusName: firstStatusName,
        editedEmailBody: 'Updated email body',
      })
      await adminProgramStatuses.expectProgramManageStatusesPage(programName)
      await adminProgramStatuses.expectStatusExists({
        statusName: firstStatusName,
        expectEmailExists: true,
      })
      await adminProgramStatuses.expectExistingStatusEmail({
        statusName: firstStatusName,
        expectedEmailBody: 'Updated email body',
      })

      // Delete the configured email (e.g. set to empty).
      await adminProgramStatuses.editStatus(firstStatusName, {
        editedStatusName: firstStatusName,
        editedEmailBody: '',
      })
      await adminProgramStatuses.expectProgramManageStatusesPage(programName)
      await adminProgramStatuses.expectStatusExists({
        statusName: firstStatusName,
        expectEmailExists: false,
      })
      await adminProgramStatuses.expectExistingStatusEmail({
        statusName: firstStatusName,
        expectedEmailBody: '',
      })
    })
  })

  describe('delete existing status', () => {
    const programName = 'Test program delete status'
    const firstStatusName = 'First status'
    const secondStatusName = 'Second status'

    beforeAll(async () => {
      const {page, adminPrograms, adminProgramStatuses} = ctx
      await loginAsAdmin(page)
      await adminPrograms.addProgram(programName)
      await adminPrograms.gotoDraftProgramManageStatusesPage(programName)

      // Create two existing statuses.
      await adminProgramStatuses.createStatus(firstStatusName)
      await adminProgramStatuses.expectProgramManageStatusesPage(programName)
      await adminProgramStatuses.createStatus(secondStatusName)
      await adminProgramStatuses.expectProgramManageStatusesPage(programName)
      await adminProgramStatuses.expectStatusExists({
        statusName: firstStatusName,
        expectEmailExists: false,
      })
      await adminProgramStatuses.expectStatusExists({
        statusName: secondStatusName,
        expectEmailExists: false,
      })
    })

    test('deletes an existing status', async () => {
      const {adminPrograms, adminProgramStatuses} = ctx
      await adminPrograms.gotoDraftProgramManageStatusesPage(programName)
      await adminProgramStatuses.deleteStatus(firstStatusName)
      await adminProgramStatuses.expectProgramManageStatusesPage(programName)
      await adminProgramStatuses.expectStatusNotExists(firstStatusName)
      await adminProgramStatuses.expectStatusExists({
        statusName: secondStatusName,
        expectEmailExists: false,
      })
    })
  })

  describe('default status', () => {
    const programName = 'Test program default statuses'

    beforeAll(async () => {
      const {page, adminPrograms} = ctx
      await loginAsAdmin(page)
      await adminPrograms.addProgram(programName)
      await adminPrograms.gotoAdminProgramsPage()
    })

    beforeEach(async () => {
      await ctx.adminPrograms.gotoDraftProgramManageStatusesPage(programName)
    })

    test('creates a new status as default', async () => {
      const {page, adminProgramStatuses} = ctx
      const statusName = 'Test Status 1'

      const confirmHandle =
        await adminProgramStatuses.createStatusWithoutClickingConfirm(
          statusName,
        )
      adminProgramStatuses.acceptDialogWithMessage(
        adminProgramStatuses.newDefaultStatusMessage(statusName),
      )
      await confirmHandle.click()

      await waitForPageJsLoad(page)
      await adminProgramStatuses.expectProgramManageStatusesPage(programName)
      await validateToastMessage(
        page,
        adminProgramStatuses.defaultStatusUpdateToastMessage(statusName),
      )
      await adminProgramStatuses.expectStatusIsDefault(statusName)
      await validateScreenshot(page, 'status-list-with-default-status')
    })

    test('dismissing the confirmation dialog does not create the new status', async () => {
      const {page, adminProgramStatuses} = ctx
      const oldDefault = 'Test Status 1'
      const statusName = 'Test Status 2'

      const confirmHandle =
        await adminProgramStatuses.createStatusWithoutClickingConfirm(
          statusName,
        )
      adminProgramStatuses.dismissDialogWithMessage(
        adminProgramStatuses.changeDefaultStatusMessage(oldDefault, statusName),
      )
      await confirmHandle.click()
      await dismissModal(page)

      await adminProgramStatuses.expectProgramManageStatusesPage(programName)
      await adminProgramStatuses.expectStatusIsDefault(oldDefault)
      await validateScreenshot(page, 'status-list-test-1-remains-default')
    })

    test('creating a new status as default changes default to the new status', async () => {
      const {page, adminProgramStatuses} = ctx
      const oldDefault = 'Test Status 1'
      const statusName = 'Test Status 2'

      const confirmHandle =
        await adminProgramStatuses.createStatusWithoutClickingConfirm(
          statusName,
        )
      adminProgramStatuses.acceptDialogWithMessage(
        adminProgramStatuses.changeDefaultStatusMessage(oldDefault, statusName),
      )
      await confirmHandle.click()

      await adminProgramStatuses.expectProgramManageStatusesPage(programName)
      await validateToastMessage(
        page,
        adminProgramStatuses.defaultStatusUpdateToastMessage(statusName),
      )
      await adminProgramStatuses.expectStatusIsDefault(statusName)
      await adminProgramStatuses.expectStatusIsNotDefault(oldDefault)
      await validateScreenshot(page, 'status-list-test-2-default')
    })

    test('changes the default status', async () => {
      const {page, adminProgramStatuses} = ctx
      const oldDefault = 'Test Status 2'
      const newDefault = 'Test Status 1'

      await adminProgramStatuses.editStatusDefault(
        newDefault,
        true,
        adminProgramStatuses.changeDefaultStatusMessage(oldDefault, newDefault),
      )

      await adminProgramStatuses.expectProgramManageStatusesPage(programName)
      await validateToastMessage(
        page,
        adminProgramStatuses.defaultStatusUpdateToastMessage(newDefault),
      )
      await adminProgramStatuses.expectStatusIsDefault(newDefault)
      await adminProgramStatuses.expectStatusIsNotDefault(oldDefault)
      await validateScreenshot(page, 'status-list-test-1-as-default-again')
    })

    test('unsets the default status', async () => {
      const {page, adminProgramStatuses} = ctx
      const statusName = 'Test Status 1'

      await adminProgramStatuses.editStatusDefault(statusName, false)

      await adminProgramStatuses.expectProgramManageStatusesPage(programName)
      await adminProgramStatuses.expectStatusIsNotDefault(statusName)
      await validateToastMessage(page, 'Status updated')
      await validateScreenshot(page, 'status-list-unset-default')
    })
  })
})
