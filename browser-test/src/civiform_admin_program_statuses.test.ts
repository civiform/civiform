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
    const programWithoutStatusesName = 'test program without statuses'
    await adminPrograms.addProgram(programWithoutStatusesName)
    await adminPrograms.gotoDraftProgramManageStatusesPage(
      programWithoutStatusesName,
    )
    await adminProgramStatuses.expectNoStatuses()
  })
})
