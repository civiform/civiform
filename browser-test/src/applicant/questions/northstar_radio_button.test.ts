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
  selectApplicantLanguageNorthstar,
} from '../../support'

test.describe(
  'Radio button question for applicant flow',
  {tag: ['@northstar']},
  () => {
    test.beforeEach(async ({page}) => {
      await enableFeatureFlag(page, 'north_star_applicant_ui')
    })

    test.describe('single radio button question', {tag: ['@northstar']}, () => {
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

      test('validate screenshot', async ({page, applicantQuestions}) => {
        await applicantQuestions.applyProgram(
          programName,
          /* northStarEnabled= */ true,
        )

        await test.step('Screenshot without errors', async () => {
          await validateScreenshot(
            page.getByTestId('questionRoot'),
            'radio-button-north-star',
            /* fullPage= */ false,
            /* mobileScreenshot= */ false,
          )
        })

        await test.step('Screenshot with errors', async () => {
          await applicantQuestions.clickContinue()
          await validateScreenshot(
            page.getByTestId('questionRoot'),
            'radio-button-errors-north-star',
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
          'radio-options-right-to-left',
          /* fullPage= */ false,
          /* mobileScreenshot= */ true,
        )
      })
    })

    test.describe('single radio button question with north star flag disabled', () => {
      const programName = 'Test program for single radio button'

      test.beforeEach(async ({page, adminQuestions, adminPrograms}) => {
        await setUpForSingleQuestion(
          programName,
          page,
          adminQuestions,
          adminPrograms,
        )
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

      test('with selection submits successfully', async ({
        applicantQuestions,
      }) => {
        await applicantQuestions.applyProgram(
          programName,
          /* northStarEnabled= */ true,
        )
        await applicantQuestions.answerRadioButtonQuestion('matcha')
        await applicantQuestions.clickContinue()

        await applicantQuestions.expectReviewPage(/* northStarEnabled= */ true)
      })

      test('with empty selection does not submit', async ({
        page,
        applicantQuestions,
      }) => {
        await applicantQuestions.applyProgram(
          programName,
          /* northStarEnabled= */ true,
        )

        // Click next without inputting anything
        await applicantQuestions.clickContinue()

        const radioButtonId = '.cf-question-radio'
        expect(await page.innerText(radioButtonId)).toContain(
          'This question is required.',
        )
        expect(await page.innerHTML(radioButtonId)).toContain('autofocus')
      })

      test('markdown applied to options shows in preview', async ({
        page,
        adminQuestions,
      }) => {
        await loginAsAdmin(page)
        await adminQuestions.createRadioButtonQuestion(
          {
            questionName: 'markdown-options-test',
            questionText: 'Sample question text',
            helpText: 'Sample question help text',
            options: [
              {adminName: 'red_markdown_admin', text: '_red_'},
              {adminName: 'green_markdown_admin', text: '__green__'},
              {
                adminName: 'orange_markdown_admin',
                text: '[orange](https://www.orange.com)',
              },
              {adminName: 'blue_markdown_admin', text: 'https://www.blue.com'},
            ],
          },
          /* clickSubmit= */ false,
        )

        await adminQuestions.expectPreviewOptionsWithMarkdown([
          '<p><em>red</em></p>\n',
          '<p><strong>green</strong></p>\n',
          '<p><a class="text-blue-600 hover:text-blue-500 underline" target="_blank" href="https://www.orange.com">orange</a></p>\n',
          '<p><a class="text-blue-600 hover:text-blue-500 underline" target="_blank" href="https://www.blue.com">https://www.blue.com</a></p>\n',
        ])
        await validateScreenshot(
          page.getByTestId('questionRoot'),
          'radio-button-options-with-markdown-north-star',
          /* fullPage= */ false,
          /* mobileScreenshot= */ false,
        )
      })

      test('options with long text render correctly', async ({
        page,
        adminQuestions,
        adminPrograms,
        applicantQuestions,
      }) => {
        const longTextProgramName = 'Long text program name'
        await loginAsAdmin(page)
        await adminQuestions.createRadioButtonQuestion(
          {
            questionName: 'long-option-test',
            questionText: 'Sample question text',
            helpText: 'Sample question help text',
            options: [
              {adminName: 'short_text', text: 'short'},
              {
                adminName: 'long_text',
                text: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.',
              },
            ],
          },
          /* clickSubmit= */ false,
        )
        await validateScreenshot(
          page.getByTestId('questionRoot'),
          'radio-options-long-text-preview',
          /* fullPage= */ false,
          /* mobileScreenshot= */ false,
        )
        await adminQuestions.clickSubmitButtonAndNavigate('Create')
        await adminPrograms.addAndPublishProgramWithQuestions(
          ['long-option-test'],
          longTextProgramName,
        )
        await logout(page)

        await applicantQuestions.applyProgram(
          longTextProgramName,
          /* northStarEnabled= */ true,
        )
        await validateScreenshot(
          page.getByTestId('questionRoot'),
          'radio-options-long-text-applicant',
          /* fullPage= */ false,
          /* mobileScreenshot= */ false,
        )
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

        await adminQuestions.addRadioButtonQuestion({
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
        await applicantQuestions.applyProgram(
          programName,
          /* northStarEnabled= */ true,
        )
        await applicantQuestions.answerRadioButtonQuestion('matcha')
        await applicantQuestions.answerRadioButtonQuestion('mountains')
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
        await applicantQuestions.answerRadioButtonQuestion('matcha')
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
