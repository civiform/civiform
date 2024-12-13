import {test} from '../support/civiform_fixtures'
import {
  enableFeatureFlag,
  loginAsAdmin,
  loginAsTestUser,
  logout,
  seedQuestions,
} from '../support'

test.describe('Application PDF download test', () => {
  test.beforeEach(async ({page}) => {
    await seedQuestions(page)
    await page.goto('/')
  })

  test('download finished application', async ({
    page,
    adminPrograms,
    applicantQuestions,
  }) => {
    const programName = 'Test program'
    await test.step('Setup program with exportable feature enabled', async () => {
      await loginAsAdmin(page)
      await enableFeatureFlag(page, 'application_exportable')
      await adminPrograms.addAndPublishProgramWithQuestions(
        ['Sample Name Question'],
        programName,
      )
      await logout(page)
    })

    await test.step('Applicant submits and downloads application', async () => {
      await loginAsTestUser(page)
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerNameQuestion('sarah', 'smith')
      await applicantQuestions.clickNext()
      await applicantQuestions.submitFromReviewPage()
      await applicantQuestions.downloadFromConfirmationPage()
      await logout(page)
    })
  })
})
