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

test.describe('Date question for applicant flow', {tag: ['@northstar']}, () => {
  test.beforeEach(async ({page}) => {
    await enableFeatureFlag(page, 'north_star_applicant_ui')
  })

  test.describe('single date question', () => {
    const programName = 'Test program for single date'

    test.beforeEach(async ({page, adminQuestions, adminPrograms}) => {
      await setUpSingleDateQuestion(
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
          'date-north-star',
          /* fullPage= */ false,
          /* mobileScreenshot= */ false,
        )
      })

      await test.step('Screenshot with errors', async () => {
        await applicantQuestions.clickContinue()
        await validateScreenshot(
          page.getByTestId('questionRoot'),
          'date-errors-north-star',
          /* fullPage= */ false,
          /* mobileScreenshot= */ false,
        )
      })

      await test.step('when returning to the page, expect date is filled in', async () => {
        await applicantQuestions.answerMemorableDateQuestion(
          '2022',
          '05 - May',
          '2',
        )
        await applicantQuestions.clickContinue()
        // Return to page.
        await applicantQuestions.clickEdit()
        await validateScreenshot(
          page,
          'date-filled-in-north-star',
          /* fullPage= */ true,
          /* mobileScreenshot= */ false,
        )
      })
    })

    test('attempts to submit', async ({applicantQuestions, page}) => {
      await applicantQuestions.applyProgram(
        programName,
        /* northStarEnabled= */ true,
      )

      await test.step('with no answer does not submit', async () => {
        // Click "Continue" without selecting anything.
        await applicantQuestions.clickContinue()

        await expect(page.getByText('This question is required.')).toBeVisible()
      })

      await test.step('with filled in date submits successfully', async () => {
        await applicantQuestions.answerMemorableDateQuestion(
          '2022',
          '05 - May',
          '2',
        )
        await applicantQuestions.clickContinue()

        await applicantQuestions.expectReviewPage(/* northStarEnabled= */ true)
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
      expect(
        page.getByLabel('Day').getAttribute('aria-required'),
      ).toBeTruthy()
      expect(
        page.getByLabel('Month').getAttribute('aria-required'),
      ).toBeTruthy()
      expect(
        page.getByLabel('Year').getAttribute('aria-required'),
      ).toBeTruthy()

      await validateAccessibility(page)
    })
  })

  test.describe('multiple date questions', () => {
    const programName = 'Test program for multiple date questions'

    test.beforeEach(async ({page, adminQuestions, adminPrograms}) => {
      await loginAsAdmin(page)

      await adminQuestions.addDateQuestion({
        questionName: 'birthday-date-q',
        questionText: 'What is your birthday? (This is required)',
      })
      await adminQuestions.addDateQuestion({
        questionName: 'todays-date-q',
        questionText: "What is today's date? (This is optional)",
      })

      await adminPrograms.addProgram(programName)
      await adminPrograms.editProgramBlockWithOptional(
        programName,
        'Optional question block',
        ['birthday-date-q'],
        'todays-date-q', // optional
      )
      await adminPrograms.publishAllDrafts()

      await logout(page)
    })

    test('with valid dates submits successfully', async ({
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(
        programName,
        /* northStarEnabled= */ true,
      )
      await applicantQuestions.answerMemorableDateQuestion(
        '2022',
        '07 - July',
        '04',
        0,
      )
      await applicantQuestions.answerMemorableDateQuestion(
        '1990',
        '10 - October',
        '10',
        1,
      )

      await applicantQuestions.clickContinue()

      await applicantQuestions.expectReviewPage(/* northStarEnabled= */ true)
    })

    test('with unanswered optional question submits successfully', async ({
      applicantQuestions,
    }) => {
      // Only answer second question.
      await applicantQuestions.applyProgram(
        programName,
        /* northStarEnabled= */ true,
      )
      await applicantQuestions.answerMemorableDateQuestion(
        '1990',
        '10 - October',
        '10',
        1,
      )
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

  async function setUpSingleDateQuestion(
    programName: string,
    page: Page,
    adminQuestions: AdminQuestions,
    adminPrograms: AdminPrograms,
  ) {
    // As admin, create program with single date question.
    await loginAsAdmin(page)

    await adminQuestions.addDateQuestion({questionName: 'general-date-q'})
    await adminPrograms.addAndPublishProgramWithQuestions(
      ['general-date-q'],
      programName,
    )

    await logout(page)
  }
})
