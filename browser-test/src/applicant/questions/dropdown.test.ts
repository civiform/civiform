import {test, expect} from '../../support/civiform_fixtures'
import {
  loginAsAdmin,
  logout,
  validateAccessibility,
  validateScreenshot,
} from '../../support'
import {SAMPLE_QUESTIONS} from '../../support/seeding'

test.describe('Dropdown question for applicant flow', () => {
  test.describe('single dropdown question', () => {
    const programName = 'Test program for single dropdown'

    test.beforeEach(async ({page, adminPrograms, seeding}) => {
      await seeding.seedQuestions()
      await loginAsAdmin(page)

      await adminPrograms.addAndPublishProgramWithQuestions(
        [SAMPLE_QUESTIONS.dropdown],
        programName,
      )

      await logout(page)
    })

    test('validate screenshot', async ({page, applicantQuestions}) => {
      await applicantQuestions.applyProgram(programName)

      await test.step('Screenshot without errors', async () => {
        await validateScreenshot(page.getByTestId('questionRoot'), 'dropdown', {
          fullPage: false,
        })
        await validateAccessibility(page)
      })

      await test.step('Screenshot with errors', async () => {
        await applicantQuestions.clickContinue()
        await validateScreenshot(
          page.getByTestId('questionRoot'),
          'dropdown-errors',
          {fullPage: false},
        )
        await validateAccessibility(page)
      })
    })

    // TODO(#7892): When admin console supports dropdown previews, unskip this test
    test.skip('Updates options in preview', async ({page, adminQuestions}) => {
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
      await applicantQuestions.applyProgram(programName)

      await test.step('with no selection does not submit', async () => {
        // Click next without selecting anything
        await applicantQuestions.clickContinue()

        await expect(page.getByText('This question is required.')).toBeVisible()
      })

      await test.step('with selected option submits successfully', async () => {
        await applicantQuestions.answerDropdownQuestion('Chocolate')
        await applicantQuestions.clickContinue()

        await applicantQuestions.expectReviewPage()
      })
    })
  })

  test.describe('multiple dropdown questions', () => {
    const programName = 'Test program for multiple dropdowns'

    test.beforeEach(async ({page, adminQuestions, adminPrograms, seeding}) => {
      await seeding.seedQuestions()
      await loginAsAdmin(page)

      // The seed contains a single dropdown question (used as the optional
      // question here), so the second one is still created via the UI.
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
        SAMPLE_QUESTIONS.dropdown, // optional
      )
      await adminPrograms.publishAllDrafts()

      await logout(page)
    })

    test('with selected options submits successfully', async ({
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerDropdownQuestion('Strawberry', 0)
      await applicantQuestions.answerDropdownQuestion('blue', 1)
      await applicantQuestions.clickContinue()

      await applicantQuestions.expectReviewPage()
    })

    test('with unanswered optional question submits successfully', async ({
      applicantQuestions,
    }) => {
      // Only answer second question. First is optional.
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerDropdownQuestion('red', 1)
      await applicantQuestions.clickContinue()

      await applicantQuestions.expectReviewPage()
    })

    test('has no accessiblity violations', async ({
      page,
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(programName)

      await validateAccessibility(page)
    })
  })
})
