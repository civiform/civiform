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
  isLocalDevEnvironment, getUserAdminName,
} from './support'
import {Page} from "playwright";

describe('view program statuses', () => {
  let pageObject: Page
  const programName = 'test program with statuses'

  beforeAll(async () => {
    const {page} = await startSession()
    pageObject = page
  })

  describe('with program statuses', () => {
    let userName: String

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

    await adminPrograms.addAndPublishProgramWithQuestions( ['Name'], programName )

    await logout(pageObject)
    await loginAsTestUser(pageObject)
    await selectApplicantLanguage(pageObject, 'English')
      userName = await getUserAdminName(pageObject)

    await applicantQuestions.applyProgram(programName)

    // Applicant fills out first application block.
    await applicantQuestions.answerNameQuestion('sarah', 'smith')
    await applicantQuestions.clickNext()

    // Applicant submits answers from review pageObject.
    await applicantQuestions.submitFromReviewPage(programName)

    await logout(pageObject)
    })

    it('Shows status options', async () => {
      await loginAsProgramAdmin(pageObject)
      const adminPrograms = new AdminPrograms(pageObject)

      await adminPrograms.viewApplications(programName)

  })
  })
})
