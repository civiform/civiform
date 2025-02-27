import {test} from '../support/civiform_fixtures'
import {
  enableFeatureFlag,
  loginAsAdmin,
  loginAsProgramAdmin,
  loginAsTestUser,
  logout,
} from '../support'

test.describe(
  'Applicant application download test',
  {tag: ['@northstar']},
  () => {
    test.beforeEach(async ({page, seeding}) => {
      await seeding.seedQuestions()
      await enableFeatureFlag(page, 'north_star_applicant_ui')
    })

    test('download finished application', async ({
      page,
      adminPrograms,
      applicantQuestions,
    }) => {
      await loginAsAdmin(page)
      await enableFeatureFlag(page, 'application_exportable')

      const programName = 'Test program'
      await adminPrograms.addAndPublishProgramWithQuestions(
        ['Sample Name Question'],
        programName,
      )

      await logout(page)
      await loginAsTestUser(page)
      await applicantQuestions.applyProgram(
        programName,
        /* northStarEnabled= */ true,
      )
      await applicantQuestions.answerNameQuestion('sarah', 'smith')
      await applicantQuestions.clickContinue()
      await applicantQuestions.submitFromReviewPage(
        /* northStarEnabled= */ true,
      )
      await applicantQuestions.downloadFromConfirmationPage(
        /* northStarEnabled= */ true,
      )

      await logout(page)
      await loginAsProgramAdmin(page)
    })
  },
)
