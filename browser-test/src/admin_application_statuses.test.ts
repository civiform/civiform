import {
  startSession,
  logout,
  loginAsGuest,
  loginAsProgramAdmin,
  loginAsAdmin,
  selectApplicantLanguage,
  ApplicantQuestions,
  AdminPrograms,
  userDisplayName,
} from './support'
import {Page} from 'playwright'

describe('view program statuses', () => {
  let pageObject: Page
  const programWithoutStatusesName = 'test program without statuses'

  beforeAll(async () => {
    const {page} = await startSession()
    pageObject = page
  })

  describe('without program statuses', () => {

    beforeAll(async () => {
      // Timeout for clicks and element fills. If your selector fails to locate
      // the HTML element, the test hangs. If you find the tests time out, you
      // want to verify that your selectors are working as expected first.
      // Because all tests are run concurrently, it could be that your selector
      // selects a different entity from another test.
      pageObject.setDefaultTimeout(4000)

      await loginAsAdmin(pageObject)
      const adminPrograms = new AdminPrograms(pageObject)
      const applicantQuestions = new ApplicantQuestions(pageObject)

      // Add a program, no questions are needed.
      await adminPrograms.addProgram(programWithoutStatusesName)
      await adminPrograms.publishProgram(programWithoutStatusesName)
      await adminPrograms.expectActiveProgram(programWithoutStatusesName)

      await logout(pageObject)
      await loginAsGuest(pageObject)
      await selectApplicantLanguage(pageObject, 'English')

      // Submit an application.
      await applicantQuestions.clickApplyProgramButton(programWithoutStatusesName)
      await applicantQuestions.submitFromPreviewPage(programWithoutStatusesName)

      await logout(pageObject)
    })

    it('does not Show status options', async () => {
      await loginAsProgramAdmin(pageObject)
      const adminPrograms = new AdminPrograms(pageObject)

      await adminPrograms.viewApplications(programWithoutStatusesName)

      await adminPrograms.viewApplicationForApplicant(userDisplayName())

      expect(await adminPrograms.expectStatusSelectorVisible()).toBeFalsy()
    })
  })
})
