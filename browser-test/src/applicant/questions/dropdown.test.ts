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
  {tag: ['@northstar']},
  () => {
    test.beforeEach(async ({page}) => {
      await enableFeatureFlag(page, 'north_star_applicant_ui')
    })

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

      test('validate screenshot', async ({page, applicantQuestions}) => {
        await applicantQuestions.applyProgram(
          programName,
          /* northStarEnabled= */ true,
        )

        await test.step('Screenshot without errors', async () => {
          await validateScreenshot(
            page.getByTestId('questionRoot'),
            'dropdown-north-star',
            /* fullPage= */ false,
            /* mobileScreenshot= */ false,
          )
          await validateAccessibility(page)
        })

        await test.step('Screenshot with errors', async () => {
          await applicantQuestions.clickContinue()
          await validateScreenshot(
            page.getByTestId('questionRoot'),
            'dropdown-errors-north-star',
            /* fullPage= */ false,
            /* mobileScreenshot= */ false,
          )
          await validateAccessibility(page)
        })
      })

      // TODO(#7892): When admin console supports dropdown previews, unskip this test
      test.skip('Updates options in preview', async ({
        page,
        adminQuestions,
      }) => {
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
          'red\n',
          'green\n',
          'orange\n',
          'blue\n',
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

      test('attempts to submit', async ({applicantQuestions, page}) => {
        await applicantQuestions.applyProgram(
          programName,
          /* northStarEnabled= */ true,
        )

        await test.step('with no selection does not submit', async () => {
          // Click next without selecting anything
          await applicantQuestions.clickContinue()

          await expect(
            page.getByText('This question is required.'),
          ).toBeVisible()
        })

        await test.step('with selected option submits successfully', async () => {
          await applicantQuestions.answerDropdownQuestion('green')
          await applicantQuestions.clickContinue()

          await applicantQuestions.expectReviewPage(
            /* northStarEnabled= */ true,
          )
        })
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

      test('with selected options submits successfully', async ({
        applicantQuestions,
      }) => {
        await applicantQuestions.applyProgram(
          programName,
          /* northStarEnabled= */ true,
        )
        await applicantQuestions.answerDropdownQuestion('beach', 0)
        await applicantQuestions.answerDropdownQuestion('blue', 1)
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
        await applicantQuestions.answerDropdownQuestion('red', 1)
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
