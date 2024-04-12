import {Page} from '@playwright/test'
import {test, expect} from '../../support/civiform_fixtures'
import {
  AdminQuestions,
  AdminPrograms,
  disableFeatureFlag,
  enableFeatureFlag,
  loginAsAdmin,
  logout,
  validateAccessibility,
  validateScreenshot,
} from '../../support'

test.describe(
  'Radio button question for applicant flow',
  {tag: ['@uses-fixtures']},
  () => {
    test.describe('single radio button question with north star flag disabled', () => {
      const programName = 'Test program for single radio button'

      test.beforeEach(async ({page, adminQuestions, adminPrograms}) => {
        await setUpForSingleQuestion(
          programName,
          page,
          adminQuestions,
          adminPrograms,
        )
        await disableFeatureFlag(page, 'north_star_applicant_ui')
      })

      test('Updates options in preview', async ({page, adminQuestions}) => {
        await loginAsAdmin(page)

        await adminQuestions.createRadioButtonQuestion(
          {
            questionName: 'not-used-in-test',
            questionText: 'Sample question text',
            helpText: 'Sample question help text',
            options: [
              {adminName: 'red_admin', text: 'red'},
              {adminName: 'green_admin', text: 'green'},
              {adminName: 'orange_admin', text: 'orange'},
              {adminName: 'blue_admin', text: 'blue'},
            ],
          },
          /* clickSubmit= */ false,
        )

        // Verify question preview has the default values.
        await adminQuestions.expectCommonPreviewValues({
          questionText: 'Sample question text',
          questionHelpText: 'Sample question help text',
        })
        await adminQuestions.expectPreviewOptions([
          'red',
          'green',
          'orange',
          'blue',
        ])

        // Empty options renders default text.
        await adminQuestions.createRadioButtonQuestion(
          {
            questionName: '',
            questionText: 'Sample question text',
            helpText: 'Sample question help text',
            options: [],
          },
          /* clickSubmit= */ false,
        )
        await adminQuestions.expectPreviewOptions(['Sample question option'])
      })

      test('validate screenshot', async ({page, applicantQuestions}) => {
        await applicantQuestions.applyProgram(programName)

        await validateScreenshot(page, 'radio-button')
      })

      test('validate screenshot with errors', async ({
        page,
        applicantQuestions,
      }) => {
        await applicantQuestions.applyProgram(programName)
        await applicantQuestions.clickNext()

        await validateScreenshot(page, 'radio-button-errors')
      })

      test('with selection submits successfully', async ({
        applicantQuestions,
      }) => {
        await applicantQuestions.applyProgram(programName)
        await applicantQuestions.answerRadioButtonQuestion('matcha')
        await applicantQuestions.clickNext()

        await applicantQuestions.submitFromReviewPage()
      })

      test('with empty selection does not submit', async ({
        page,
        applicantQuestions,
      }) => {
        await applicantQuestions.applyProgram(programName)

        // Click next without inputting anything
        await applicantQuestions.clickNext()

        const radioButtonId = '.cf-question-radio'
        expect(await page.innerText(radioButtonId)).toContain(
          'This question is required.',
        )
        expect(await page.innerHTML(radioButtonId)).toContain('autofocus')
      })
    })

    test.describe('multiple radio button questions', () => {
      const programName = 'Test program for multiple radio button qs'

      test.beforeEach(async ({page, adminQuestions, adminPrograms}) => {
        await loginAsAdmin(page)

        await adminQuestions.addRadioButtonQuestion({
          questionName: 'fave-ice-cream-q',
          options: [
            {adminName: 'matcha_admin', text: 'matcha'},
            {adminName: 'strawberry_admin', text: 'strawberry'},
            {adminName: 'vanilla_admin', text: 'vanilla'},
          ],
        })

        await adminQuestions.addCheckboxQuestion({
          questionName: 'fave-vacation-q',
          options: [
            {adminName: 'beach_admin', text: 'beach'},
            {adminName: 'mountains_admin', text: 'mountains'},
            {adminName: 'city_admin', text: 'city'},
            {adminName: 'cruise_admin', text: 'cruise'},
          ],
        })

        await adminPrograms.addProgram(programName)
        await adminPrograms.editProgramBlockWithOptional(
          programName,
          'Optional question block',
          ['fave-ice-cream-q'],
          'fave-vacation-q', // optional
        )
        await adminPrograms.publishAllDrafts()

        await logout(page)
      })

      test('with both selections submits successfully', async ({
        applicantQuestions,
      }) => {
        await applicantQuestions.applyProgram(programName)
        await applicantQuestions.answerRadioButtonQuestion('matcha')
        await applicantQuestions.answerRadioButtonQuestion('mountains')
        await applicantQuestions.clickNext()

        await applicantQuestions.submitFromReviewPage()
      })

      test('with unanswered optional question submits successfully', async ({
        applicantQuestions,
      }) => {
        // Only answer second question. First is optional.
        await applicantQuestions.applyProgram(programName)
        await applicantQuestions.answerRadioButtonQuestion('matcha')
        await applicantQuestions.clickNext()

        await applicantQuestions.submitFromReviewPage()
      })

      test('has no accessiblity violations', async ({
        page,
        applicantQuestions,
      }) => {
        await applicantQuestions.applyProgram(programName)

        await validateAccessibility(page)
      })
    })

    test.describe('single radio button question with north star flag enabled', () => {
      const programName = 'Test program for single radio button'

      test.beforeEach(async ({page, adminQuestions, adminPrograms}) => {
        await setUpForSingleQuestion(
          programName,
          page,
          adminQuestions,
          adminPrograms,
        )
        await enableFeatureFlag(page, 'north_star_applicant_ui')
      })

      test(
        'validate screenshot with north star flag enabled',
        {tag: ['@northstar']},
        async ({page, applicantQuestions}) => {
          await applicantQuestions.applyProgram(programName)

          await test.step('Screenshot without errors', async () => {
            await validateScreenshot(
              page,
              'radio-button-north-star',
              /* fullPage= */ true,
              /* mobileScreenshot= */ true,
            )
          })

          await test.step('Screenshot with errors', async () => {
            await applicantQuestions.clickContinue()
            await validateScreenshot(
              page,
              'radio-button-errors-north-star',
              /* fullPage= */ true,
              /* mobileScreenshot= */ true,
            )
          })
        },
      )
    })

    async function setUpForSingleQuestion(
      programName: string,
      page: Page,
      adminQuestions: AdminQuestions,
      adminPrograms: AdminPrograms,
    ) {
      // As admin, create program with radio button question.
      await loginAsAdmin(page)

      await adminQuestions.addRadioButtonQuestion({
        questionName: 'ice-cream-radio-q',
        options: [
          {adminName: 'matcha_admin', text: 'matcha'},
          {adminName: 'strawberry_admin', text: 'strawberry'},
          {adminName: 'vanilla_admin', text: 'vanilla'},
        ],
      })
      await adminPrograms.addAndPublishProgramWithQuestions(
        ['ice-cream-radio-q'],
        programName,
      )

      await logout(page)
    }
  },
)
