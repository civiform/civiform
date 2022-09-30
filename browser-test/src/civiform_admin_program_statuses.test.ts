import {
  createTestContext,
  dismissModal,
  enableFeatureFlag,
  loginAsAdmin,
  validateScreenshot,
} from './support'
import {waitForAnyModal} from './support/wait'

describe('modify program statuses', () => {
  const ctx = createTestContext(/* clearDb= */ false)

  beforeEach(async () => {
    const {page} = ctx
    await loginAsAdmin(page)
    await enableFeatureFlag(page, 'application_status_tracking_enabled')
  })

  describe('statuses list', () => {
    it('creates a new program and has no statuses', async () => {
      const {page, adminPrograms, adminProgramStatuses} = ctx
      // Add a draft program, no questions are needed.
      const programName = 'test-program-without-statuses'
      await adminPrograms.addProgram(programName)
      await adminPrograms.gotoDraftProgramManageStatusesPage(programName)
      await adminProgramStatuses.expectNoStatuses()
      await validateScreenshot(page, 'status_list_with_no_statuses')
    })
  })

  describe('new status creation', () => {
    const programName = 'test-program-create-statuses'

    beforeAll(async () => {
      const {page, adminPrograms} = ctx
      await loginAsAdmin(page)
      await adminPrograms.addProgram(programName)
    })

    beforeEach(async () => {
      await ctx.adminPrograms.gotoDraftProgramManageStatusesPage(programName)
    })

    it('renders create new status modal', async () => {
      const {page} = ctx
      await page.click('button:has-text("Create a new status")')

      const modal = await waitForAnyModal(page)
      expect(await modal.innerText()).toContain('Create a new status')
      await validateScreenshot(page, 'create_new_status_modal')
    })

    it('creates a new status with no email', async () => {
      const {adminProgramStatuses} = ctx
      await adminProgramStatuses.createStatus('Status with no email')
      await adminProgramStatuses.expectProgramManageStatusesPage(programName)
      await adminProgramStatuses.expectStatusExists({
        statusName: 'Status with no email',
        expectEmailExists: false,
      })
    })

    it('creates a new status with email', async () => {
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

    it('fails to create status with an empty name', async () => {
      const {page, adminProgramStatuses} = ctx
      await adminProgramStatuses.createStatus('')
      await adminProgramStatuses.expectProgramManageStatusesPage(programName)
      await adminProgramStatuses.expectCreateStatusModalWithError(
        'This field is required',
      )
      await dismissModal(page)
    })

    it('fails to create status with an existing name', async () => {
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
    const programName = 'test-program-edit-statuses'
    const firstStatusName = 'First status'
    const secondStatusName = 'Second status'

    beforeAll(async () => {
      const {page, adminPrograms, adminProgramStatuses} = ctx
      await loginAsAdmin(page)
      await enableFeatureFlag(page, 'application_status_tracking_enabled')
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

    it('renders existing statuses', async () => {
      const {page} = ctx
      await validateScreenshot(page, 'status_list_with_statuses')
    })

    it('fails to edit status when providing an existing status name', async () => {
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

    it('fails to edit status with an empty name', async () => {
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

    it('edits an existing status name', async () => {
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

    it('edits an existing status, configures email, and deletes the configured email', async () => {
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
    const programName = 'test-program-delete-status'
    const firstStatusName = 'First status'
    const secondStatusName = 'Second status'

    beforeAll(async () => {
      const {page, adminPrograms, adminProgramStatuses} = ctx
      await loginAsAdmin(page)
      await enableFeatureFlag(page, 'application_status_tracking_enabled')
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

    it('deletes an existing status', async () => {
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
})
