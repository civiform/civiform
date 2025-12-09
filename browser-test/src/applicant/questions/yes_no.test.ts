import {Page} from '@playwright/test'
import {expect, test} from '../../support/civiform_fixtures'
import {
  AdminQuestions,
  AdminPrograms,
  loginAsAdmin,
  logout,
  validateAccessibility,
  validateScreenshot,
  selectApplicantLanguageNorthstar,
} from '../../support'

test.describe('Yes/no question for applicant flow', () => {
  test.describe('single yes/no question', () => {
    const programName = 'Test program for single yes/no question'

    test.beforeEach(async ({page, adminQuestions, adminPrograms}) => {
      await createYesNoQuestionAndAddToProgram(
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
          {fullPage: false},
        )
      })

      await test.step('Screenshot with errors', async () => {
        await applicantQuestions.clickContinue()
        await validateScreenshot(
          page.getByTestId('questionRoot'),
          'yes-no-applicant-view-errors',
          {fullPage: false},
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
        {fullPage: false},
      )
    })

    test('options translate to Spanish', async ({page, applicantQuestions}) => {
      await applicantQuestions.applyProgram(
        programName,
        /* northStarEnabled= */ true,
      )
      await selectApplicantLanguageNorthstar(page, 'es-US')

      // Verify Spanish translations are shown
      await expect(page.getByText('Sí', {exact: true})).toBeVisible()
      await expect(page.getByText('No', {exact: true})).toBeVisible()
      await expect(page.getByText('Tal vez', {exact: true})).toBeVisible()
      await expect(page.getByText('No lo sé', {exact: true})).toBeVisible()

      // Verify English text is not shown
      await expect(page.getByText('Yes', {exact: true})).toBeHidden()
      await expect(page.getByText('Maybe', {exact: true})).toBeHidden()
      await expect(page.getByText('Not sure', {exact: true})).toBeHidden()
    })
  })

  test.describe('yes/no question with options not displayed to applicant', () => {
    const programName =
      'Test program for single yes/no question with some options hidden'
    test.beforeEach(async ({page, adminQuestions, adminPrograms}) => {
      await loginAsAdmin(page)

      await adminQuestions.addYesNoQuestion({
        questionName: 'yes-no-question-one',
        optionTextToExclude: ['Not sure'],
      })

      await adminPrograms.addAndPublishProgramWithQuestions(
        ['yes-no-question-one'],
        programName,
      )
      await logout(page)
    })

    test('validate option hidden', async ({page, applicantQuestions}) => {
      await applicantQuestions.applyProgram(
        programName,
        /* northStarEnabled= */ true,
      )

      await expect(page.getByText('Yes', {exact: true})).toBeVisible()
      await expect(page.getByText('No', {exact: true})).toBeVisible()
      await expect(page.getByText('Maybe', {exact: true})).toBeVisible()

      await expect(page.getByText('Not sure', {exact: true})).toBeHidden()
    })
  })

  test.describe('multiple yes/no questions', () => {
    const programName = 'Test program for multiple yes/no questions'

    test.beforeEach(async ({page, adminQuestions, adminPrograms}) => {
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

  async function createYesNoQuestionAndAddToProgram(
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
})
