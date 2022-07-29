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

  beforeAll(async () => {
    const {page} = await startSession()
    pageObject = page
  })

  it('new program has no statuses', async () => {
    await loginAsAdmin(pageObject)
    const adminPrograms = new AdminPrograms(pageObject)
    const adminProgramStatuses = new AdminProgramStatuses(pageObject)

    // Add a draft program, no questions are needed.
    const programName = 'test program without statuses'
    await adminPrograms.addProgram(programName)
    await adminPrograms.gotoDraftProgramManageStatusesPage(programName)
    await adminProgramStatuses.expectNoStatuses()
  })

  describe('new status creation', () => {
    let adminPrograms: AdminPrograms
    let adminProgramStatuses: AdminProgramStatuses
    const programName = 'test program create statuses'

    beforeAll(async () => {
      adminPrograms = new AdminPrograms(pageObject)
      adminProgramStatuses = new AdminProgramStatuses(pageObject)

      await adminPrograms.addProgram(programName)
      await adminPrograms.gotoDraftProgramManageStatusesPage(programName)
    })

    it('creates a new status with no email', async () => {
      await adminProgramStatuses.addStatus('Status with no email')
      await adminPrograms.expectProgramManageStatusesPage(programName)
      await adminProgramStatuses.expectStatusExists(
        'Status with no email',
        false,
      )
    })

    it('creates a new status with email', async () => {
      await adminProgramStatuses.addStatus('Status with email', 'An email')
      await adminPrograms.expectProgramManageStatusesPage(programName)
      await adminProgramStatuses.expectStatusExists('Status with email', true)
    })

    it('fails to create status with an empty name', async () => {
      await adminProgramStatuses.addStatus('')
      await adminPrograms.expectProgramManageStatusesPage(programName)
      await adminProgramStatuses.expectCreateStatusModalWithError(
        'This field is required',
      )
      await dismissModal(pageObject)
    })

    it('fails to create status with an existing name', async () => {
      await adminProgramStatuses.addStatus('Existing status')
      await adminPrograms.expectProgramManageStatusesPage(programName)
      await adminProgramStatuses.addStatus('Existing status')
      await adminPrograms.expectProgramManageStatusesPage(programName)
      await adminProgramStatuses.expectCreateStatusModalWithError(
        'A status with name Existing status already exists',
      )
      await dismissModal(pageObject)
    })
  })
})
