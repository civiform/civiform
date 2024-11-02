import {Page} from '@playwright/test'
import {test, expect} from '../../support/civiform_fixtures'
import {
  AdminPrograms,
  AdminQuestions,
  enableFeatureFlag,
  loginAsAdmin,
  logout,
  validateAccessibility,
  validateScreenshot,
} from '../../support'

test.describe(
  'Checkbox question for applicant flow',
  {tag: ['@northstar']},
  () => {
    test.beforeEach(async ({page}) => {
      await enableFeatureFlag(page, 'north_star_applicant_ui')
    })

    test.describe('single checkbox question', () => {
      const programName = 'Test program for single checkbox'

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

        await adminQuestions.createCheckboxQuestion(
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
        await adminQuestions.createCheckboxQuestion(
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
        await applicantQuestions.applyProgram(
          programName,
          /* northStarEnabled= */ true,
        )

        await validateScreenshot(page, 'checkbox-north-star')
      })

      test('validate screenshot with errors', async ({
        page,
        applicantQuestions,
      }) => {
        await applicantQuestions.applyProgram(
          programName,
          /* northStarEnabled= */ true,
        )
        await applicantQuestions.clickContinue()

        await validateScreenshot(page, 'checkbox-errors-north-star')
      })

      test('with single checked box submits successfully', async ({
        applicantQuestions,
      }) => {
        await applicantQuestions.applyProgram(
          programName,
          /* northStarEnabled= */ true,
        )
        await applicantQuestions.answerCheckboxQuestion(['blue'])
        await applicantQuestions.clickContinue()

        await applicantQuestions.submitFromReviewPage(
          /* northStarEnabled= */ true,
        )
      })

      test('with no checked boxes does not submit', async ({
        page,
        applicantQuestions,
      }) => {
        await applicantQuestions.applyProgram(
          programName,
          /* northStarEnabled= */ true,
        )

        // No validation errors on first page load.
        await expect(page.locator('.cf-applicant-question-errors')).toBeHidden()

        // Click "Continue" without selecting anything.
        await applicantQuestions.clickContinue()

        // Check checkbox error and required error are present.
        await expect(
          page.getByText('Error: Please select at least 1.'),
        ).toBeVisible()
        await expect(
          page.getByText('Error: This question is required.'),
        ).toBeVisible()
      })

      test('with greater than max allowed checked boxes does not submit', async ({
        page,
        applicantQuestions,
      }) => {
        await applicantQuestions.applyProgram(
          programName,
          /* northStarEnabled= */ true,
        )
        const checkBoxError = '.cf-applicant-question-errors'
        // No validation errors on first page load.
        await expect(page.locator(checkBoxError)).toBeHidden()

        // Max of two checked boxes are allowed, but we select three.
        await applicantQuestions.answerCheckboxQuestion([
          'blue',
          'green',
          'orange',
        ])
        await applicantQuestions.clickContinue()

        // Check error is shown.
        await expect(page.locator(checkBoxError)).toBeVisible()
      })

      test('markdown applied to options shows in preview', async ({
        page,
        adminQuestions,
      }) => {
        await loginAsAdmin(page)
        await adminQuestions.createCheckboxQuestion(
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
          page,
          'checkbox-options-with-markdown-north-star',
        )
      })

      test('options with long text render correctly', async ({
        page,
        adminQuestions,
        adminPrograms,
        applicantQuestions,
      }) => {
        const longTextProgramName = 'Long text program name'

        await test.step('Create program', async () => {
          await loginAsAdmin(page)
          await adminQuestions.createCheckboxQuestion(
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
            page,
            'checkbox-options-long-text-preview-north-star',
          )
          await adminQuestions.clickSubmitButtonAndNavigate('Create')
          await adminPrograms.addAndPublishProgramWithQuestions(
            ['long-option-test'],
            longTextProgramName,
          )
          await logout(page)
        })

        await applicantQuestions.applyProgram(
          longTextProgramName,
          /* northStarEnabled= */ true,
        )
        await validateScreenshot(
          page,
          'checkbox-options-long-text-applicant-north-star',
        )
      })
    })

    test.describe('multiple checkbox questions', () => {
      const programName = 'Test program for multiple checkboxes'

      test.beforeEach(async ({page, adminQuestions, adminPrograms}) => {
        await loginAsAdmin(page)

        await adminQuestions.addCheckboxQuestion({
          questionName: 'checkbox-fave-color-q',
          options: [
            {adminName: 'red_admin', text: 'red'},
            {adminName: 'green_admin', text: 'green'},
            {adminName: 'orange_admin', text: 'orange'},
            {adminName: 'blue_admin', text: 'blue'},
          ],
          minNum: 1,
          maxNum: 2,
        })
        await adminQuestions.addCheckboxQuestion({
          questionName: 'checkbox-vacation-q',
          options: [
            {adminName: 'beach_admin', text: 'beach'},
            {adminName: 'mountains_admin', text: 'mountains'},
            {adminName: 'city_admin', text: 'city'},
            {adminName: 'cruise_admin', text: 'cruise'},
          ],
          minNum: 1,
          maxNum: 2,
        })

        await adminPrograms.addProgram(programName)
        await adminPrograms.editProgramBlockWithOptional(
          programName,
          'Optional question block',
          ['checkbox-fave-color-q'],
          'checkbox-vacation-q', // optional
        )
        await adminPrograms.publishAllDrafts()

        await logout(page)
      })

      test('with valid checkboxes submits successfully', async ({
        applicantQuestions,
      }) => {
        await applicantQuestions.applyProgram(
          programName,
          /* northStarEnabled= */ true,
        )
        await applicantQuestions.answerCheckboxQuestion(['blue'])
        await applicantQuestions.answerCheckboxQuestion(['beach'])
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
        await applicantQuestions.answerCheckboxQuestion(['red'])
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
        const checkboxError = '.cf-applicant-question-errors'
        // No validation errors on first page load.
        await expect(page.locator(checkboxError)).toBeHidden()

        // Max of 2 answers allowed.
        await applicantQuestions.answerCheckboxQuestion([
          'red',
          'green',
          'orange',
        ])
        await applicantQuestions.answerCheckboxQuestion(['beach'])
        await applicantQuestions.clickContinue()

        await expect(page.locator(checkboxError)).toBeVisible()
      })

      test('with second invalid does not submit', async ({
        page,
        applicantQuestions,
      }) => {
        await applicantQuestions.applyProgram(
          programName,
          /* northStarEnabled= */ true,
        )
        const checkboxError = '.cf-applicant-question-errors'
        // No validation errors on first page load.
        await expect(page.locator(checkboxError)).toBeHidden()

        await applicantQuestions.answerCheckboxQuestion(['red'])
        // Max of 2 answers allowed.
        await applicantQuestions.answerCheckboxQuestion([
          'beach',
          'mountains',
          'city',
        ])
        await applicantQuestions.clickContinue()

        await expect(page.locator(checkboxError)).toBeVisible()
      })

      test('has no accessibility violations', async ({
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
      // As admin, create program with single checkbox question.
      await loginAsAdmin(page)

      await adminQuestions.addCheckboxQuestion({
        questionName: 'checkbox-color-q',
        options: [
          {adminName: 'red_admin', text: 'red'},
          {adminName: 'green_admin', text: 'green'},
          {adminName: 'orange_admin', text: 'orange'},
          {adminName: 'blue_admin', text: 'blue'},
        ],
        minNum: 1,
        maxNum: 2,
      })
      await adminPrograms.addAndPublishProgramWithQuestions(
        ['checkbox-color-q'],
        programName,
      )

      await logout(page)
    }
  },
)
