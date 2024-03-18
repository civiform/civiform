import {test, expect} from '../../fixtures/custom_fixture'
import {
  enableFeatureFlag,
  disableFeatureFlag,
  loginAsAdmin,
  logout,
  validateAccessibility,
  validateScreenshot,
} from '../../support'

test.describe(
  'Checkbox question for applicant flow',
  {tag: ['@migrated']},
  () => {
    test.describe('single checkbox question', () => {
      const programName = 'Test program for single checkbox'

      test.beforeEach(async ({page, adminQuestions, adminPrograms}) => {
        // beforeAll

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

        // beforeEach
        await disableFeatureFlag(page, 'north_star_applicant_ui')
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
        await applicantQuestions.applyProgram(programName)

        await validateScreenshot(page, 'checkbox')
      })

      test('validate screenshot with errors', async ({
        page,
        applicantQuestions,
      }) => {
        await applicantQuestions.applyProgram(programName)
        await applicantQuestions.clickNext()

        await validateScreenshot(page, 'checkbox-errors')
      })

      test(
        'validate screenshot with north star flag enabled',
        {tag: ['@northstar']},
        async ({page, applicantQuestions}) => {
          await enableFeatureFlag(page, 'north_star_applicant_ui')
          await applicantQuestions.applyProgram(programName)

          await validateScreenshot(
            page,
            'checkbox-north-star',
            /* fullPage= */ true,
            /* mobileScreenshot= */ true,
          )
        },
      )

      test('with single checked box submits successfully', async ({
        applicantQuestions,
      }) => {
        await applicantQuestions.applyProgram(programName)
        await applicantQuestions.answerCheckboxQuestion(['blue'])
        await applicantQuestions.clickNext()

        await applicantQuestions.submitFromReviewPage()
      })

      test('with no checked boxes does not submit', async ({
        page,
        applicantQuestions,
      }) => {
        await applicantQuestions.applyProgram(programName)

        // No validation errors on first page load.
        const checkBoxError = '.cf-applicant-question-errors'
        await expect(page.locator(checkBoxError)).toBeHidden()

        // Click next without selecting anything.
        await applicantQuestions.clickNext()

        // Check checkbox error and required error are present.
        await expect(page.locator(checkBoxError)).toBeVisible()

        const checkboxId = '.cf-question-checkbox'
        expect(await page.innerText(checkboxId)).toContain(
          'This question is required.',
        )
      })

      test('with greater than max allowed checked boxes does not submit', async ({
        page,
        applicantQuestions,
      }) => {
        await applicantQuestions.applyProgram(programName)
        const checkBoxError = '.cf-applicant-question-errors'
        // No validation errors on first page load.
        await expect(page.locator(checkBoxError)).toBeHidden()

        // Max of two checked boxes are allowed, but we select three.
        await applicantQuestions.answerCheckboxQuestion([
          'blue',
          'green',
          'orange',
        ])
        await applicantQuestions.clickNext()

        // Check error is shown.
        await expect(page.locator(checkBoxError)).toBeVisible()
      })
    })

    test.describe('multiple checkbox questions', () => {
      const programName = 'Test program for multiple checkboxes'

      test.beforeEach(async ({page, adminQuestions, adminPrograms}) => {
        // beforeAll
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

        // beforeEach
      })

      test('with valid checkboxes submits successfully', async ({
        applicantQuestions,
      }) => {
        await applicantQuestions.applyProgram(programName)
        await applicantQuestions.answerCheckboxQuestion(['blue'])
        await applicantQuestions.answerCheckboxQuestion(['beach'])
        await applicantQuestions.clickNext()

        await applicantQuestions.submitFromReviewPage()
      })

      test('with unanswered optional question submits successfully', async ({
        applicantQuestions,
      }) => {
        // Only answer required question.
        await applicantQuestions.applyProgram(programName)
        await applicantQuestions.answerCheckboxQuestion(['red'])
        await applicantQuestions.clickNext()

        await applicantQuestions.submitFromReviewPage()
      })

      test('with first invalid does not submit', async ({
        page,
        applicantQuestions,
      }) => {
        await applicantQuestions.applyProgram(programName)
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
        await applicantQuestions.clickNext()

        await expect(page.locator(checkboxError)).toBeVisible()
      })

      test('with second invalid does not submit', async ({
        page,
        applicantQuestions,
      }) => {
        await applicantQuestions.applyProgram(programName)
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
        await applicantQuestions.clickNext()

        await expect(page.locator(checkboxError)).toBeVisible()
      })

      test('has no accessibility violations', async ({
        page,
        applicantQuestions,
      }) => {
        await applicantQuestions.applyProgram(programName)

        await validateAccessibility(page)
      })
    })
  },
)
