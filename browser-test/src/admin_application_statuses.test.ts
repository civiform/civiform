import {
  startSession,
  seedCanonicalQuestions,
  dropTables,
  logout,
  loginAsTestUser,
  loginAsGuest,
  loginAsProgramAdmin,
  loginAsAdmin,
  selectApplicantLanguage,
  ApplicantQuestions,
  AdminQuestions,
  AdminPrograms,
  endSession,
  isLocalDevEnvironment,
  getUserAdminName,
  userDisplayName,
} from './support'
import {Page} from 'playwright'

describe('view program statuses', () => {
  let pageObject: Page
  const programName = 'test program with statuses'

  beforeAll(async () => {
    const {page} = await startSession()
    pageObject = page
  })

  describe('with program statuses', () => {
    let userName: string

    beforeAll(async () => {
      // Timeout for clicks and element fills. If your selector fails to locate
      // the HTML element, the test hangs. If you find the tests time out, you
      // want to verify that your selectors are working as expected first.
      // Because all tests are run concurrently, it could be that your selector
      // selects a different entity from another test.
      pageObject.setDefaultTimeout(4000)

      await loginAsAdmin(pageObject)
      //const adminQuestions = new AdminQuestions(pageObject)
      const adminPrograms = new AdminPrograms(pageObject)
      const applicantQuestions = new ApplicantQuestions(pageObject)

      await adminPrograms.addProgram(programName)
      await adminPrograms.publishProgram(programName)
      await adminPrograms.expectActiveProgram(programName)

      await logout(pageObject)
      await loginAsGuest(pageObject)
      await selectApplicantLanguage(pageObject, 'English')
      userName = await getUserAdminName(pageObject)

      //await applicantQuestions.applyProgram(programName)
      await applicantQuestions.clickApplyProgramButton(programName)
      await pageObject.screenshot({ path: 'tmp/applyClickNext.png', fullPage: true })

      // Applicant submits answers from review pageObject.
      await applicantQuestions.submitFromPreviewPage(programName)

      await logout(pageObject)
    })

    it('Shows status options', async () => {
      await loginAsProgramAdmin(pageObject)
      const adminPrograms = new AdminPrograms(pageObject)

      await adminPrograms.viewApplications(programName)
      await pageObject.screenshot({ path: 'tmp/applicantfind.png', fullPage: true })

      await adminPrograms.viewApplicationForApplicant(userDisplayName())

      expect(await adminPrograms.expectStatusSelectorVisible())
    })
  })
})
