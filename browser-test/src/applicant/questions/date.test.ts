import {Page} from '@playwright/test'
import {test, expect} from '../../support/civiform_fixtures'
import {
  AdminQuestions,
  AdminPrograms,
  loginAsAdmin,
  logout,
  validateAccessibility,
  validateScreenshot,
} from '../../support'

test.describe('Date question for applicant flow', {tag: ['@northstar']}, () => {
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
        await validateScreenshot(page.getByTestId('questionRoot'), 'date', {
          fullPage: false,
        })
      })

      await test.step('Screenshot with errors', async () => {
        await applicantQuestions.clickContinue()
        await validateScreenshot(
          page.getByTestId('questionRoot'),
          'date-errors',
          {
            fullPage: false,
          },
        )
      })

      await test.step('when returning to the page, expect date is filled in', async () => {
        await applicantQuestions.answerMemorableDateQuestion(
          '2025',
          '05 - May',
          '2',
        )
        await applicantQuestions.clickContinue()
        // Return to page.
        await applicantQuestions.clickEdit()
        await validateScreenshot(page, 'date-filled-in')
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

      await test.step('with unallowable date in past does not submit', async () => {
        await applicantQuestions.answerMemorableDateQuestion(
          '2000',
          '05 - May',
          '2',
        )
        await applicantQuestions.clickContinue()

        await expect(
          page.getByText('Date must be after 2025-01-01'),
        ).toBeVisible()
      })

      await test.step('with filled in date submits successfully', async () => {
        await applicantQuestions.answerMemorableDateQuestion(
          '2025',
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
      await expect(page.getByLabel('Day')).toHaveAttribute('aria-required')
      await expect(page.getByLabel('Month')).toHaveAttribute('aria-required')
      await expect(page.getByLabel('Year')).toHaveAttribute('aria-required')

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

  test.describe('date question autofill functionality', () => {
    const programName = 'Test program for date autofill'

    test.beforeEach(async ({page, adminQuestions, adminPrograms}) => {
      await loginAsAdmin(page)
      
      await adminQuestions.addDateQuestion({
        questionName: 'date_autofill_test',
        questionText: 'What is today\'s date?',
        minDateType: 'APPLICATION_DATE',
        maxDateType: 'APPLICATION_DATE',
      })

      // Create a program, have this question in the program
      await adminPrograms.addProgram(programName)
      await adminPrograms.editProgramBlock(programName, 'first block', [
        'date_autofill_test',
      ])
      await adminPrograms.publishProgram(programName)
      await logout(page)
    })

    test('autofills current date when both min and max date are APPLICATION_DATE', async ({
      page,
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(programName, /* northStarEnabled= */ true)

      // Get today's date in the format expected by the date input
      const today = new Date()
      const todayString = today.toISOString().split('T')[0] // YYYY-MM-DD

      // Check that the date input is whatever today's date is 
      const dateInput = page.locator('input[type="date"]')
      await expect(dateInput).toHaveValue(todayString)
    })

    test('does not autofill when applicant already has a date value', async ({
      page,
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(programName, /* northStarEnabled= */ true)

      const customDate = '2023-12-25'
      await page.fill('input[type="date"]', customDate)
      await applicantQuestions.clickContinue()

      await applicantQuestions.clickEdit()

      const dateInput = page.locator('input[type="date"]')
      await expect(dateInput).toHaveValue(customDate)
    })

    test('does not autofill when only one of min/max date is APPLICATION_DATE', async ({
      page,
      adminQuestions,
      adminPrograms,
      applicantQuestions,
    }) => {
      await loginAsAdmin(page)
      await adminQuestions.addDateQuestion({
        questionName: 'date_partial_validation_test',
        questionText: 'What is your birthday?',
        minDateType: 'APPLICATION_DATE',
        maxDateType: 'ANY',
      })

      const partialProgramName = 'Test program for partial date validation'
      await adminPrograms.addProgram(partialProgramName)
      await adminPrograms.editProgramBlock(partialProgramName, 'first block', [
        'date_partial_validation_test',
      ])
      await adminPrograms.publishProgram(partialProgramName)
      await logout(page)

      await applicantQuestions.applyProgram(partialProgramName, /* northStarEnabled= */ true)

      const dateInput = page.locator('input[type="date"]')
      await expect(dateInput).toHaveValue('')
    })
  })

  async function setUpSingleDateQuestion(
    programName: string,
    page: Page,
    adminQuestions: AdminQuestions,
    adminPrograms: AdminPrograms,
    withValidation: boolean = true,
  ) {
    // As admin, create program with single date question.
    await loginAsAdmin(page)

    if (withValidation) {
      await adminQuestions.addDateQuestion({
        questionName: 'general-date-q',
        minDateType: 'CUSTOM',
        minDateDay: 1,
        minDateMonth: 1,
        minDateYear: 2025,
        maxDateType: 'ANY',
      })
    } else {
      await adminQuestions.addDateQuestion({questionName: 'general-date-q'})
    }
    await adminPrograms.addAndPublishProgramWithQuestions(
      ['general-date-q'],
      programName,
    )

    await logout(page)
  }
})
