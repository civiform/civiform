import {
  dismissModal,
  startSession,
  logout,
  loginAsGuest,
  loginAsProgramAdmin,
  loginAsAdmin,
  selectApplicantLanguage,
  ApplicantQuestions,
  AdminPrograms,
  AdminProgramStatuses,
  userDisplayName,
} from './support'
import {Page} from 'playwright'

describe('modify program statuses', () => {
  let pageObject: Page
  let adminPrograms: AdminPrograms
  let adminProgramStatuses: AdminProgramStatuses

  beforeAll(async () => {
    const {page} = await startSession()
    pageObject = page
    adminPrograms = new AdminPrograms(pageObject)
    adminProgramStatuses = new AdminProgramStatuses(pageObject)
  })

  it('new program has no statuses', async () => {
    await loginAsAdmin(pageObject)

    // Add a draft program, no questions are needed.
    const programName = 'test program without statuses'
    await adminPrograms.addProgram(programName)
    await adminPrograms.gotoDraftProgramManageStatusesPage(programName)
    await adminProgramStatuses.expectNoStatuses()
  })

  describe('new status creation', () => {
    const programName = 'test program create statuses'

    beforeAll(async () => {
      await adminPrograms.addProgram(programName)
      await adminPrograms.gotoDraftProgramManageStatusesPage(programName)
    })

    it('creates a new status with no email', async () => {
      await adminProgramStatuses.createStatus('Status with no email')
      await adminPrograms.expectProgramManageStatusesPage(programName)
      await adminProgramStatuses.expectStatusExists(
        'Status with no email',
        false,
      )
    })

    it('creates a new status with email', async () => {
      await adminProgramStatuses.createStatus('Status with email', 'An email')
      await adminPrograms.expectProgramManageStatusesPage(programName)
      await adminProgramStatuses.expectStatusExists('Status with email', true)
    })

    it('fails to create status with an empty name', async () => {
      await adminProgramStatuses.createStatus('')
      await adminPrograms.expectProgramManageStatusesPage(programName)
      await adminProgramStatuses.expectCreateStatusModalWithError(
        'This field is required',
      )
      await dismissModal(pageObject)
    })

    it('fails to create status with an existing name', async () => {
      await adminProgramStatuses.createStatus('Existing status')
      await adminPrograms.expectProgramManageStatusesPage(programName)
      await adminProgramStatuses.createStatus('Existing status')
      await adminPrograms.expectProgramManageStatusesPage(programName)
      await adminProgramStatuses.expectCreateStatusModalWithError(
        'A status with name Existing status already exists',
      )
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
      await adminPrograms.expectProgramManageStatusesPage(programName)
      await adminProgramStatuses.createStatus(secondStatusName)
      await adminPrograms.expectProgramManageStatusesPage(programName)
      await adminProgramStatuses.expectStatusExists(firstStatusName, false)
      await adminProgramStatuses.expectStatusExists(secondStatusName, false)
    })

    it('fails to edit status when providing an existing status name', async () => {
      await adminProgramStatuses.editStatus(firstStatusName, secondStatusName)
      await adminPrograms.expectProgramManageStatusesPage(programName)
      await adminProgramStatuses.expectEditStatusModalWithError(
        `A status with name ${secondStatusName} already exists`,
      )
      await dismissModal(pageObject)
    })

    it('fails to edit status with an empty name', async () => {
      await adminProgramStatuses.editStatus(firstStatusName, '')
      await adminPrograms.expectProgramManageStatusesPage(programName)
      await adminProgramStatuses.expectEditStatusModalWithError(
        'This field is required',
      )
      await dismissModal(pageObject)
    })

    it('edits an existing status name', async () => {
      await adminProgramStatuses.editStatus(
        secondStatusName,
        'Updated status name',
      )
      await adminPrograms.expectProgramManageStatusesPage(programName)
      await adminProgramStatuses.expectStatusExists(
        'Updated status name',
        false,
      )
      await adminProgramStatuses.expectStatusNotExists(secondStatusName)
    })

    it('edits an existing status and configures email', async () => {
      await adminProgramStatuses.editStatus(
        firstStatusName,
        firstStatusName,
        'An email body',
      )
      await adminPrograms.expectProgramManageStatusesPage(programName)
      await adminProgramStatuses.expectStatusExists(firstStatusName, true)
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
      await adminPrograms.expectProgramManageStatusesPage(programName)
      await adminProgramStatuses.createStatus(secondStatusName)
      await adminPrograms.expectProgramManageStatusesPage(programName)
      await adminProgramStatuses.expectStatusExists(firstStatusName, false)
      await adminProgramStatuses.expectStatusExists(secondStatusName, false)
    })

    it('deletes an existing status', async () => {
      await adminProgramStatuses.deleteStatus(firstStatusName)
      await adminPrograms.expectProgramManageStatusesPage(programName)
      await adminProgramStatuses.expectStatusNotExists(firstStatusName)
      await adminProgramStatuses.expectStatusExists(secondStatusName, false)
    })
  })
})
