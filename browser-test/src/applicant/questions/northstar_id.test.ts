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

test.describe('Id question for applicant flow', () => {
  test.beforeEach(async ({page}) => {
    await enableFeatureFlag(page, 'north_star_applicant_ui')
  })

  test.describe('single id question', () => {
    const programName = 'Test program for single id'

    test.beforeEach(async ({page, adminQuestions, adminPrograms}) => {
      await setUpProgramWithSingleIdQuestion(
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
          'id-north-star',
          /* fullPage= */ false,
          /* mobileScreenshot= */ false,
        )
      })

      await test.step('Screenshot with errors', async () => {
        // Do not fill in the question
        await applicantQuestions.clickContinue()
        await validateScreenshot(
          page.getByTestId('questionRoot'),
          'id-errors-north-star',
          /* fullPage= */ false,
          /* mobileScreenshot= */ false,
        )
      })
    })

    test('attempts to submit', async ({applicantQuestions, page}) => {
      await applicantQuestions.applyProgram(
        programName,
        /* northStarEnabled= */ true,
      )

      await test.step('with empty id does not submit', async () => {
        // Click "Continue" without inputting anything
        await applicantQuestions.clickContinue()

        await expect(
          page.getByText('Error: Must contain at least 5 characters.'),
        ).toBeVisible()
      })

      await test.step('with id submits successfully', async () => {
        await applicantQuestions.answerIdQuestion('12345')
        await applicantQuestions.clickContinue()

        await applicantQuestions.expectReviewPage(/* northStarEnabled= */ true)
      })
    })

    test('with too short id does not submit', async ({
      page,
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(
        programName,
        /* northStarEnabled= */ true,
      )
      await applicantQuestions.answerIdQuestion('123')
      await applicantQuestions.clickContinue()

      await expect(
        page.getByText('Error: Must contain at least 5 characters.'),
      ).toBeVisible()
    })

    test('with too long id does not submit', async ({
      page,
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(
        programName,
        /* northStarEnabled= */ true,
      )
      await applicantQuestions.answerIdQuestion('123456')
      await applicantQuestions.clickContinue()

      await expect(
        page.getByText('Error: Must contain at most 5 characters.'),
      ).toBeVisible()
    })

    test('with non-numeric characters does not submit', async ({
      page,
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(
        programName,
        /* northStarEnabled= */ true,
      )
      await applicantQuestions.answerIdQuestion('abcde')
      await applicantQuestions.clickContinue()

      await expect(
        page.getByText('Error: Must contain only numbers.'),
      ).toBeVisible()
    })
  })

  test.describe('multiple id questions', () => {
    const programName = 'Test program for multiple ids'

    test.beforeEach(async ({page, adminQuestions, adminPrograms}) => {
      await loginAsAdmin(page)

      await adminQuestions.addIdQuestion({
        questionName: 'my-id-q',
      })
      await adminQuestions.addIdQuestion({
        questionName: 'your-id-q',
      })

      await adminPrograms.addProgram(programName)
      await adminPrograms.editProgramBlockWithOptional(
        programName,
        'Optional question block',
        ['my-id-q'],
        'your-id-q', // optional
      )
      await adminPrograms.publishAllDrafts()

      await logout(page)
    })

    test('with both id inputs submits successfully', async ({
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(
        programName,
        /* northStarEnabled= */ true,
      )
      await applicantQuestions.answerIdQuestion('12345', 0)
      await applicantQuestions.answerIdQuestion('67890', 1)
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
      await applicantQuestions.answerIdQuestion('67890', 1)
      await applicantQuestions.clickContinue()

      await applicantQuestions.expectReviewPage(/* northStarEnabled= */ true)
    })

    test('with first invalid does not submit', async ({
      page,
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(
        programName,
        /* northStarEnabled= */ true,
      )
      await applicantQuestions.answerIdQuestion('abcde', 0)
      await applicantQuestions.answerIdQuestion('67890', 1)
      await applicantQuestions.clickContinue()

      await expect(
        page.getByText('Error: Must contain only numbers.'),
      ).toBeVisible()
    })

    test('with second invalid does not submit', async ({
      page,
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(
        programName,
        /* northStarEnabled= */ true,
      )
      await applicantQuestions.answerIdQuestion('67890', 0)
      await applicantQuestions.answerIdQuestion('abcde', 1)
      await applicantQuestions.clickContinue()

      await expect(
        page.getByText('Error: Must contain only numbers.'),
      ).toBeVisible()
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

  async function setUpProgramWithSingleIdQuestion(
    programName: string,
    page: Page,
    adminQuestions: AdminQuestions,
    adminPrograms: AdminPrograms,
  ) {
    // As admin, create program with single id question.
    await loginAsAdmin(page)

    await adminQuestions.addIdQuestion({
      questionName: 'id-q',
      minNum: 5,
      maxNum: 5,
    })
    await adminPrograms.addAndPublishProgramWithQuestions(['id-q'], programName)

    await logout(page)
  }
})
