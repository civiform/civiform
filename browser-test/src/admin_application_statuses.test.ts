import { AdminPrograms, AdminProgramStatuses, ApplicantQuestions, loginAsAdmin, loginAsGuest, loginAsProgramAdmin, logout, selectApplicantLanguage, startSession, userDisplayName, validateScreenshot, } from './support'
import { Page } from 'playwright'

// TODO(#3071): Re-enable when the feature flag is controllable in tests.
describe.skip('view program statuses', () => {
  let pageObject: Page
  let adminPrograms: AdminPrograms
  let applicantQuestions: ApplicantQuestions
  let adminProgramStatuses: AdminProgramStatuses

  beforeAll(async () => {
    const {page} = await startSession()
    pageObject = page
    adminPrograms = new AdminPrograms(pageObject)
    applicantQuestions = new ApplicantQuestions(pageObject)
    adminProgramStatuses = new AdminProgramStatuses(pageObject)
  })

  describe('without program statuses', () => {
    const programWithoutStatusesName = 'test program without statuses'
    beforeAll(async () => {
      await loginAsAdmin(pageObject)

      // Add a program, no questions are needed.
      await adminPrograms.addProgram(programWithoutStatusesName)
      await adminPrograms.publishProgram(programWithoutStatusesName)
      await adminPrograms.expectActiveProgram(programWithoutStatusesName)

      await logout(pageObject)
      await loginAsGuest(pageObject)
      await selectApplicantLanguage(pageObject, 'English')

      // Submit an application.
      await applicantQuestions.clickApplyProgramButton(
        programWithoutStatusesName,
      )
      await applicantQuestions.submitFromPreviewPage(programWithoutStatusesName)

      await logout(pageObject)
    })

    afterAll(async () => {
      await logout(pageObject)
    })

    it('does not Show status options', async () => {
      await loginAsProgramAdmin(pageObject)

      await adminPrograms.viewApplications(programWithoutStatusesName)

      await adminPrograms.viewApplicationForApplicant(userDisplayName())

      expect(await adminPrograms.isStatusSelectorVisible()).toBe(false)
      await validateScreenshot(pageObject);
    })
  })

  describe('with program statuses', () => {
    const programWithStatusesName = 'test program with statuses'
    const statusName = 'Status 1'
    beforeAll(async () => {
      await loginAsAdmin(pageObject)

      // Add a program, no questions are needed.
      await adminPrograms.addProgram(programWithStatusesName)
      await adminPrograms.gotoDraftProgramManageStatusesPage(
        programWithStatusesName,
      )
      await adminProgramStatuses.createStatus(statusName)
      await adminPrograms.publishProgram(programWithStatusesName)
      await adminPrograms.expectActiveProgram(programWithStatusesName)

      await logout(pageObject)
      await loginAsGuest(pageObject)
      await selectApplicantLanguage(pageObject, 'English')

      // Submit an application.
      await applicantQuestions.clickApplyProgramButton(programWithStatusesName)
      await applicantQuestions.submitFromPreviewPage(programWithStatusesName)

      await logout(pageObject)
      await loginAsProgramAdmin(pageObject)

      await adminPrograms.viewApplications(programWithStatusesName)
      await adminPrograms.viewApplicationForApplicant(userDisplayName())
    })

    afterAll(async () => {
      await logout(pageObject)
    })

    it('shows status selector', async () => {
      expect(await adminPrograms.isStatusSelectorVisible()).toBe(true)
    })

    it('shows default option as placeholder', async () => {
      expect(await adminPrograms.getStatusOption()).toBe('Choose an option:')
      await validateScreenshot(pageObject);
    })
  })
})
