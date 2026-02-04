import {test} from '../support/civiform_fixtures'
import {
  loginAsAdmin,
  loginAsProgramAdmin,
  loginAsTestUser,
  logout,
} from '../support'
import {BASE_URL} from '../support/config'

test.describe('Applicant application download test', () => {
  test.beforeEach(async ({page, seeding}) => {
    await seeding.seedQuestions()
    await page.goto(BASE_URL)
  })

  test('download finished application', async ({
    page,
    adminPrograms,
    applicantQuestions,
  }) => {
    await loginAsAdmin(page)

    const programName = 'Test program'
    await adminPrograms.addAndPublishProgramWithQuestions(
      ['Sample Name Question'],
      programName,
    )

    await logout(page)
    await loginAsTestUser(page)
    await applicantQuestions.applyProgram(programName)
    await applicantQuestions.answerNameQuestion('sarah', 'smith')
    await applicantQuestions.clickContinue()
    await applicantQuestions.submitFromReviewPage()
    await applicantQuestions.downloadFromConfirmationPage()

    await logout(page)
    await loginAsProgramAdmin(page)
  })
})
