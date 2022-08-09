import {
  dismissModal,
  startSession,
  loginAsAdmin,
  validateScreenshot,
  AdminPrograms,
  AdminProgramStatuses,
} from './support'
import {Page} from 'playwright'

// TODO(#3071): Re-enable when the feature flag is controllable in tests.
describe.skip('modify program statuses', () => {
  let pageObject: Page
  let adminPrograms: AdminPrograms
  let adminProgramStatuses: AdminProgramStatuses

  beforeAll(async () => {
    const {page} = await startSession()
    pageObject = page
    adminPrograms = new AdminPrograms(pageObject)
    adminProgramStatuses = new AdminProgramStatuses(pageObject)

    await loginAsAdmin(pageObject)
  })

  describe('statuses list', () => {
    it('creates a new program and has no statuses', async () => {
      // Add a draft program, no questions are needed.
      const programName = 'test program without statuses'
      await adminPrograms.addProgram(programName)
      await adminPrograms.gotoDraftProgramManageStatusesPage(programName)
      await adminProgramStatuses.expectNoStatuses()
    })
  })

  describe('new status creation', () => {
    const programName = 'test program create statuses'

    beforeAll(async () => {
      await adminPrograms.addProgram(programName)
      await adminPrograms.gotoDraftProgramManageStatusesPage(programName)
    })

    it('creates a new status with no email', async () => {
      await adminProgramStatuses.createStatus('Status with no email')
      await adminProgramStatuses.expectProgramManageStatusesPage(programName)
      await adminProgramStatuses.expectStatusExists({
        statusName: 'Status with no email',
        expectEmailExists: false,
      })
      await validateScreenshot(pageObject)
    })

    it('creates a new status with email', async () => {
      await adminProgramStatuses.createStatus('Status with email', {
        emailBody: 'An email',
      })
      await adminProgramStatuses.expectProgramManageStatusesPage(programName)
      await adminProgramStatuses.expectStatusExists({
        statusName: 'Status with email',
        expectEmailExists: true,
      })
      await validateScreenshot(pageObject)
    })

    it('fails to create status with an empty name', async () => {
      await adminProgramStatuses.createStatus('')
      await adminProgramStatuses.expectProgramManageStatusesPage(programName)
      await adminProgramStatuses.expectCreateStatusModalWithError(
        'This field is required',
      )
      await dismissModal(pageObject)
      await validateScreenshot(pageObject)
    })

    it('fails to create status with an existing name', async () => {
      await adminProgramStatuses.createStatus('Existing status')
      await adminProgramStatuses.expectProgramManageStatusesPage(programName)
      await adminProgramStatuses.createStatus('Existing status')
      await adminProgramStatuses.expectProgramManageStatusesPage(programName)
      await adminProgramStatuses.expectCreateStatusModalWithError(
        'A status with name Existing status already exists',
      )
      await validateScreenshot(pageObject)
      await dismissModal(pageObject)
    })
  })

  describe('edit existing statuses', () => {
    const programName = 'test program edit statuses'
    const firstStatusName = 'First status'
    const secondStatusName = 'Second status'

    beforeAll(async () => {
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
      await validateScreenshot(pageObject)
    })

    it('fails to edit status when providing an existing status name', async () => {
      await adminProgramStatuses.editStatus(firstStatusName, {
        editedStatusName: secondStatusName,
      })
      await adminProgramStatuses.expectProgramManageStatusesPage(programName)
      await adminProgramStatuses.expectEditStatusModalWithError(
        `A status with name ${secondStatusName} already exists`,
      )
      await validateScreenshot(pageObject)
      await dismissModal(pageObject)
    })

    it('fails to edit status with an empty name', async () => {
      await adminProgramStatuses.editStatus(firstStatusName, {
        editedStatusName: '',
      })
      await adminProgramStatuses.expectProgramManageStatusesPage(programName)
      await adminProgramStatuses.expectEditStatusModalWithError(
        'This field is required',
      )
      await validateScreenshot(pageObject)
      await dismissModal(pageObject)
    })

    it('edits an existing status name', async () => {
      await adminProgramStatuses.editStatus(secondStatusName, {
        editedStatusName: 'Updated status name',
      })
      await adminProgramStatuses.expectProgramManageStatusesPage(programName)
      await adminProgramStatuses.expectStatusExists({
        statusName: 'Updated status name',
        expectEmailExists: false,
      })
      await adminProgramStatuses.expectStatusNotExists(secondStatusName)
      await validateScreenshot(pageObject)
    })

    it('edits an existing status, configures email, and deletes the configured email', async () => {
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
      await validateScreenshot(pageObject)
    })
  })

  describe('delete existing status', () => {
    const programName = 'test program delete status'
    const firstStatusName = 'First status'
    const secondStatusName = 'Second status'

    beforeAll(async () => {
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
      await adminProgramStatuses.deleteStatus(firstStatusName)
      await adminProgramStatuses.expectProgramManageStatusesPage(programName)
      await adminProgramStatuses.expectStatusNotExists(firstStatusName)
      await adminProgramStatuses.expectStatusExists({
        statusName: secondStatusName,
        expectEmailExists: false,
      })
      await validateScreenshot(pageObject)
    })
  })
})
