import {Page} from '@playwright/test'
import {test} from '../../support/civiform_fixtures'
import {
  AdminQuestions,
  AdminPrograms,
  enableFeatureFlag,
  loginAsAdmin,
  logout,
  validateAccessibility,
  validateScreenshot,
  selectApplicantLanguageNorthstar,
} from '../../support'

test.describe(
  'Yes/no question for applicant flow',
  {tag: ['@northstar']},
  () => {
    test.beforeEach(async ({page}) => {
      await enableFeatureFlag(page, 'north_star_applicant_ui')
      await enableFeatureFlag(page, 'yes_no_question_enabled')
    })

    test.describe('single yes/no question', {tag: ['@northstar']}, () => {
      const programName = 'Test program for single yes/no question'

      test.beforeEach(async ({page, adminQuestions, adminPrograms}) => {
        await setUpForSingleQuestion(
          programName,
          page,
          adminQuestions,
          adminPrograms,
        )
      })

      test('validate screenshot', async ({page, applicantQuestions}) => {
        await applicantQuestions.applyProgram(
          programName,
          /* northStarEnabled= */ true,
        )

        await test.step('Screenshot without errors', async () => {
          await validateScreenshot(
            page.getByTestId('questionRoot'),
            'yes-no-applicant-view',
            /* fullPage= */ false,
            /* mobileScreenshot= */ false,
          )
        })

        await test.step('Screenshot with errors', async () => {
          await applicantQuestions.clickContinue()
          await validateScreenshot(
            page.getByTestId('questionRoot'),
            'yes-no-applicant-view-errors',
            /* fullPage= */ false,
            /* mobileScreenshot= */ false,
          )
        })
      })

      test('has no accessiblity violations', async ({
        page,
        applicantQuestions,
      }) => {
        await applicantQuestions.applyProgram(
          programName,
          /* northStarEnabled= */ true,
        )

        await validateAccessibility(page)
      })

      test('renders correctly right to left', async ({
        page,
        applicantQuestions,
      }) => {
        await applicantQuestions.applyProgram(
          programName,
          /* northStarEnabled= */ true,
        )
        await selectApplicantLanguageNorthstar(page, 'ar')
        await validateScreenshot(
          page.getByTestId('questionRoot'),
          'yes-no-right-to-left',
          /* fullPage= */ false,
          /* mobileScreenshot= */ true,
        )
      })
    })

    test.describe('yes/no question with options not displayed to applicant', () => {
      test.beforeEach(async ({page, adminQuestions, adminPrograms}) => {
        await enableFeatureFlag(page, 'yes_no_question_enabled')
        await loginAsAdmin(page)

        await adminQuestions.addYesNoQuestion({
          questionName: 'yes-no-question-one',
          // TODO(dwaterman): configure options here
        })

        await adminPrograms.addProgram(programName)
        await adminPrograms.editProgramBlockWithOptional(
          programName,
          'Question block',
          ['yes-no-question-one'],
        )
        await adminPrograms.publishAllDrafts()

        await logout(page)
      })

      // TODO(dwaterman): test assertions here

    })

    test.describe('multiple yes/no questions', () => {
      const programName = 'Test program for multiple yes/no questions'

      test.beforeEach(async ({page, adminQuestions, adminPrograms}) => {
        await enableFeatureFlag(page, 'yes_no_question_enabled')
        await loginAsAdmin(page)

        await adminQuestions.addYesNoQuestion({
          questionName: 'yes-no-question-one',
        })

        await adminQuestions.addYesNoQuestion({
          questionName: 'yes-no-question-two',
        })

        await adminPrograms.addProgram(programName)
        await adminPrograms.editProgramBlockWithOptional(
          programName,
          'Optional question block',
          ['yes-no-question-one'],
          'yes-no-question-two', // optional
        )
        await adminPrograms.publishAllDrafts()

        await logout(page)
      })

      test('with both selections submits successfully', async ({
        applicantQuestions,
      }) => {
        await applicantQuestions.applyProgram(
          programName,
          /* northStarEnabled= */ true,
        )
        await applicantQuestions.answerYesNoQuestion('Yes')
        await applicantQuestions.answerYesNoQuestion('No', /* order= */ 1)
        await applicantQuestions.clickContinue()

        await applicantQuestions.expectReviewPage(/* northStarEnabled= */ true)
      })

      test('with unanswered optional question submits successfully', async ({
        applicantQuestions,
      }) => {
        // Only answer second question. First is optional.
        await applicantQuestions.applyProgram(
          programName,
          /* northStarEnabled= */ true,
        )
        await applicantQuestions.answerYesNoQuestion('Yes', /* order= */ 1)
        await applicantQuestions.clickContinue()

        await applicantQuestions.expectReviewPage(/* northStarEnabled= */ true)
      })

      test('has no accessiblity violations', async ({
        page,
        applicantQuestions,
      }) => {
        await applicantQuestions.applyProgram(
          programName,
          /* northStarEnabled= */ true,
        )

        await validateAccessibility(page)
      })
    })

    async function setUpForSingleQuestion(
      programName: string,
      page: Page,
      adminQuestions: AdminQuestions,
      adminPrograms: AdminPrograms,
    ) {
      // As admin, create program with yes/no question.
      await loginAsAdmin(page)

      await adminQuestions.addYesNoQuestion({
        questionName: 'yes-no-q',
      })
      await adminPrograms.addAndPublishProgramWithQuestions(
        ['yes-no-q'],
        programName,
      )

      await logout(page)
    }
  },
)
