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
  'Dropdown question for applicant flow',
  {tag: ['@uses-fixtures']},
  () => {
    test.describe('single dropdown question', () => {
      const programName = 'Test program for single dropdown'

      test.beforeEach(async ({page, adminQuestions, adminPrograms}) => {
        await setUpSingleDropdownQuestion(
          programName,
          page,
          adminQuestions,
          adminPrograms,
        )
      })

      test('Updates options in preview', async ({page, adminQuestions}) => {
        await loginAsAdmin(page)

        await adminQuestions.createDropdownQuestion(
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
        await adminQuestions.createDropdownQuestion(
          {
            questionName: '',
            questionText: 'Sample question text',
            helpText: 'Sample question help text',
            options: [],
          },
          /* clickSubmit= */ false,
        )
        await adminQuestions.expectPreviewOptions(['Sample question option\n'])
      })

      test('validate screenshot', async ({page, applicantQuestions}) => {
        await applicantQuestions.applyProgram(programName)

        await validateScreenshot(page, 'dropdown')
      })

      test('validate screenshot with errors', async ({
        page,
        applicantQuestions,
      }) => {
        await applicantQuestions.applyProgram(programName)
        await applicantQuestions.clickNext()

        await validateScreenshot(page, 'dropdown-errors')
      })

      test('with selected option submits successfully', async ({
        applicantQuestions,
      }) => {
        await applicantQuestions.applyProgram(programName)
        await applicantQuestions.answerDropdownQuestion('green')
        await applicantQuestions.clickNext()

        await applicantQuestions.submitFromReviewPage()
      })

      test('with no selection does not submit', async ({
        page,
        applicantQuestions,
      }) => {
        await applicantQuestions.applyProgram(programName)
        // Click next without selecting anything.
        await applicantQuestions.clickNext()

        const dropdownId = '.cf-question-dropdown'
        expect(await page.innerText(dropdownId)).toContain(
          'This question is required.',
        )
      })
    })

    test.describe('multiple dropdown questions', () => {
      const programName = 'Test program for multiple dropdowns'

      test.beforeEach(async ({page, adminQuestions, adminPrograms}) => {
        await loginAsAdmin(page)

        await adminQuestions.addDropdownQuestion({
          questionName: 'dropdown-fave-vacation-q',
          options: [
            {adminName: 'beach_admin', text: 'beach'},
            {adminName: 'mountains_admin', text: 'mountains'},
            {adminName: 'city_admin', text: 'city'},
            {adminName: 'cruise_admin', text: 'cruise'},
          ],
        })
        await adminQuestions.addDropdownQuestion({
          questionName: 'dropdown-fave-color-q',
          options: [
            {adminName: 'red_admin', text: 'red'},
            {adminName: 'green_admin', text: 'green'},
            {adminName: 'orange_admin', text: 'orange'},
            {adminName: 'blue_admin', text: 'blue'},
          ],
        })

        await adminPrograms.addProgram(programName)
        await adminPrograms.editProgramBlockWithOptional(
          programName,
          'Optional question block',
          ['dropdown-fave-color-q'],
          'dropdown-fave-vacation-q', // optional
        )
        await adminPrograms.publishAllDrafts()

        await logout(page)
      })
      await adminQuestions.expectPreviewOptions([
        'red\n',
        'green\n',
        'orange\n',
        'blue\n',
      ])

      test('with selected options submits successfully', async ({
        applicantQuestions,
      }) => {
        await applicantQuestions.applyProgram(programName)
        await applicantQuestions.answerDropdownQuestion('beach', 0)
        await applicantQuestions.answerDropdownQuestion('blue', 1)
        await applicantQuestions.clickNext()

        await applicantQuestions.submitFromReviewPage()
      })

      test('with unanswered optional question submits successfully', async ({
        applicantQuestions,
      }) => {
        // Only answer second question. First is optional.
        await applicantQuestions.applyProgram(programName)
        await applicantQuestions.answerDropdownQuestion('red', 1)
        await applicantQuestions.clickNext()

        await applicantQuestions.submitFromReviewPage()
      })

      test('has no accessibility violations', async ({
        page,
        applicantQuestions,
      }) => {
        await applicantQuestions.applyProgram(programName)

        await validateAccessibility(page)
      })
    })

    test.describe('single dropdown question with north star flag enabled', () => {
      const programName = 'Test program for single dropdown'

      test.beforeEach(async ({page, adminQuestions, adminPrograms}) => {
        await setUpSingleDropdownQuestion(
          programName,
          page,
          adminQuestions,
          adminPrograms,
        )
        await enableFeatureFlag(page, 'north_star_applicant_ui')
      })

      test(
        'validate screenshot',
        {tag: ['@northstar']},
        async ({page, applicantQuestions}) => {
          await applicantQuestions.applyProgram(programName)

          await test.step('Screenshot without errors', async () => {
            await validateScreenshot(
              page,
              'dropdown-north-star',
              /* fullPage= */ true,
              /* mobileScreenshot= */ true,
            )
          })

          await test.step('Screenshot with errors', async () => {
            await applicantQuestions.clickContinue()
            await validateScreenshot(
              page,
              'dropdown-errors-north-star',
              /* fullPage= */ true,
              /* mobileScreenshot= */ true,
            )
          })
        },
      )
    })

    async function setUpSingleDropdownQuestion(
      programName: string,
      page: Page,
      adminQuestions: AdminQuestions,
      adminPrograms: AdminPrograms,
    ) {
      // As admin, create program with single dropdown question.
      await loginAsAdmin(page)

      await adminQuestions.addDropdownQuestion({
        questionName: 'dropdown-color-q',
        options: [
          {adminName: 'red_admin', text: 'red'},
          {adminName: 'green_admin', text: 'green'},
          {adminName: 'orange_admin', text: 'orange'},
          {adminName: 'blue_admin', text: 'blue'},
        ],
      })
      await adminPrograms.addAndPublishProgramWithQuestions(
        ['dropdown-color-q'],
        programName,
      )

      await logout(page)
    }
  },
)
