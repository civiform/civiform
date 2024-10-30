import {Page} from '@playwright/test'
import {test, expect} from '../../support/civiform_fixtures'
import {
  AdminQuestions,
  AdminPrograms,
  enableFeatureFlag,
  loginAsAdmin,
  logout,
  validateAccessibility,
  validateScreenshot,
} from '../../support'

test.describe(
  'Number question for applicant flow',
  {tag: ['@northstar']},
  () => {
    const errorMessage = 'This question is required.'
    const invalidErrorMessage =
      'Error: Number must be a positive whole number and can only contain numeric characters 0-9.'

    test.beforeEach(async ({page}) => {
      await enableFeatureFlag(page, 'north_star_applicant_ui')
    })

    test.describe('single number question', () => {
      const programName = 'Test program for single number'

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
            'number-north-star',
            /* fullPage= */ false,
            /* mobileScreenshot= */ false,
          )
        })

        await test.step('Screenshot with errors', async () => {
          await applicantQuestions.clickContinue()
          await validateScreenshot(
            page.getByTestId('questionRoot'),
            'number-errors-north-star',
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

      test('with valid number submits successfully', async ({
        applicantQuestions,
      }) => {
        await applicantQuestions.applyProgram(
          programName,
          /* northStarEnabled= */ true,
        )
        await applicantQuestions.answerNumberQuestion('8')
        await applicantQuestions.clickContinue()

        await applicantQuestions.submitFromReviewPage(
          /* northStarEnabled= */ true,
        )
      })

      test('with no input does not submit', async ({
        page,
        applicantQuestions,
      }) => {
        await applicantQuestions.applyProgram(
          programName,
          /* northStarEnabled= */ true,
        )
        // Leave field blank.
        await applicantQuestions.clickContinue()

        await expect(page.getByText(errorMessage)).toBeVisible()
      })

      test('with invalid inputs does not submit', async ({
        page,
        applicantQuestions,
      }) => {
        await applicantQuestions.applyProgram(
          programName,
          /* northStarEnabled= */ true,
        )
        const testValues = ['12e3', '12E3', '-123', '1.23']

        for (const testValue of testValues) {
          await applicantQuestions.answerNumberQuestion(testValue)
          await applicantQuestions.clickContinue()

          await expect(page.getByText(invalidErrorMessage)).toBeVisible()
          await applicantQuestions.answerNumberQuestion('')
        }
      })
    })

    test.describe('multiple number questions', () => {
      const programName = 'Test program for multiple numbers'

      test.beforeEach(async ({page, adminQuestions, adminPrograms}) => {
        await loginAsAdmin(page)

        await adminQuestions.addNumberQuestion({
          questionName: 'my-number-q',
        })
        await adminQuestions.addNumberQuestion({
          questionName: 'your-number-q',
        })

        await adminPrograms.addProgram(programName)
        await adminPrograms.editProgramBlockWithOptional(
          programName,
          'Optional question block',
          ['my-number-q'],
          'your-number-q', // optional
        )
        await adminPrograms.publishAllDrafts()

        await logout(page)
      })

      test('with valid numbers submits successfully', async ({
        applicantQuestions,
      }) => {
        await applicantQuestions.applyProgram(
          programName,
          /* northStarEnabled= */ true,
        )
        await applicantQuestions.answerNumberQuestion('100', 0)
        await applicantQuestions.answerNumberQuestion('33', 1)
        await applicantQuestions.clickContinue()

        await applicantQuestions.submitFromReviewPage(
          /* northStarEnabled= */ true,
        )
      })

      test('with unanswered optional question submits successfully', async ({
        applicantQuestions,
      }) => {
        // Only answer required question.
        await applicantQuestions.applyProgram(
          programName,
          /* northStarEnabled= */ true,
        )
        await applicantQuestions.answerNumberQuestion('33', 1)
        await applicantQuestions.clickContinue()

        await applicantQuestions.submitFromReviewPage(
          /* northStarEnabled= */ true,
        )
      })

      test('with first invalid does not submit', async ({
        page,
        applicantQuestions,
      }) => {
        await applicantQuestions.applyProgram(
          programName,
          /* northStarEnabled= */ true,
        )
        await applicantQuestions.answerNumberQuestion('-10', 0)
        await applicantQuestions.answerNumberQuestion('33', 1)
        await applicantQuestions.clickContinue()

        await expect(page.getByText(invalidErrorMessage)).toBeVisible()
      })

      test('with second invalid does not submit', async ({
        page,
        applicantQuestions,
      }) => {
        await applicantQuestions.applyProgram(
          programName,
          /* northStarEnabled= */ true,
        )
        await applicantQuestions.answerNumberQuestion('10', 0)
        await applicantQuestions.answerNumberQuestion('-5', 1)
        await applicantQuestions.clickContinue()

        await expect(page.getByText(invalidErrorMessage)).toBeVisible()
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
      // As admin, create program with single number question.
      await loginAsAdmin(page)

      await adminQuestions.addNumberQuestion({
        questionName: 'fave-number-q',
      })
      await adminPrograms.addAndPublishProgramWithQuestions(
        ['fave-number-q'],
        programName,
      )

      await logout(page)
    }
  },
)
