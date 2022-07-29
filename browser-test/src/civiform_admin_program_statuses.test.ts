import {
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
    it('creates a new status with no email', async () => {
      const programName = 'test program create statuses'
      const adminPrograms = new AdminPrograms(pageObject)
      const adminProgramStatuses = new AdminProgramStatuses(pageObject)
      await adminPrograms.addProgram(programName)
      await adminPrograms.gotoDraftProgramManageStatusesPage(programName)

      // Click create status and set to empty.
      await adminProgramStatuses.addStatus('First status')
      await adminPrograms.expectProgramManageStatusesPage(programName)
      await adminProgramStatuses.expectStatusExists('First status', false)
    })

    it('creates a new status with email', async () => {
      const programName = 'test program create statuses with email'
      const adminPrograms = new AdminPrograms(pageObject)
      const adminProgramStatuses = new AdminProgramStatuses(pageObject)
      await adminPrograms.addProgram(programName)
      await adminPrograms.gotoDraftProgramManageStatusesPage(programName)

      // Click create status and set to empty.
      await adminProgramStatuses.addStatus('Second status', 'An email')
      await adminPrograms.expectProgramManageStatusesPage(programName)
      await adminProgramStatuses.expectStatusExists('Second status', true)
    })
  })
})
