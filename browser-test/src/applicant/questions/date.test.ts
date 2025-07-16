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
  disableFeatureFlag,
  waitForPageJsLoad,
} from '../../support'

test.describe('Date question for applicant flow', () => {
  test.beforeEach(async ({page}) => {
    await disableFeatureFlag(page, 'north_star_applicant_ui')
    await disableFeatureFlag(page, 'date_validation_enabled')
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
      await applicantQuestions.applyProgram(programName)

      await validateScreenshot(page, 'date')
    })

    test('validate screenshot with errors', async ({
      page,
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.clickNext()

      await validateScreenshot(page, 'date-errors')
    })

    test('with filled in date submits successfully', async ({
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerDateQuestion('2022-05-02')
      await applicantQuestions.clickNext()

      await applicantQuestions.submitFromReviewPage()
    })

    test('with invalid answer does not submit', async ({
      page,
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerDateQuestion('2566-05-02')
      await applicantQuestions.clickNext()

      // Check required error is present
      expect(await page.innerText('.cf-question-date')).toContain(
        'Please enter a date less than the 150 years in future',
      )
      await applicantQuestions.answerDateQuestion('1866-05-02')
      await applicantQuestions.clickNext()

      // Check required error is present
      expect(await page.innerText('.cf-question-date')).toContain(
        'Please enter a date in the last 150 years',
      )
    })

    test('with no answer does not submit', async ({
      page,
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(programName)
      // Click next without selecting anything.
      await applicantQuestions.clickNext()

      // Check required error is present
      const dateId = '.cf-question-date'
      expect(await page.innerText(dateId)).toContain(
        'This question is required.',
      )
    })

    test('with date outside valid range does not submit', async ({
      page,
      applicantQuestions,
      adminQuestions,
      adminPrograms,
    }) => {
      await loginAsAdmin(page)
      await enableFeatureFlag(page, 'date_validation_enabled')
      await logout(page)

      await test.step('Create date question with validation', async () => {
        await setUpDateQuestionWithMinMaxValidation(
          'Test program with date validation',
          page,
          adminQuestions,
          adminPrograms,
        )
      })

      await test.step('Expect date outside valid range to fail validation', async () => {
        await applicantQuestions.applyProgram(
          'Test program with date validation',
        )
        // Date is before min date but not more than 150 years in the past
        await applicantQuestions.answerDateQuestion('2000-01-01')
        await applicantQuestions.clickNext()

        // Check required error is present
        expect(await page.innerText('.cf-question-date')).toContain(
          'Date must be after 2025-01-01',
        )
      })

      await test.step('Expect date within valid range to submit successfully', async () => {
        // Date is after min date
        await applicantQuestions.answerDateQuestion('2026-01-01')
        await applicantQuestions.clickNext()

        await applicantQuestions.submitFromReviewPage()
      })
    })
  })

  test.describe('multiple date questions', () => {
    const programName = 'Test program for multiple date questions'

    test.beforeEach(async ({page, adminQuestions, adminPrograms}) => {
      await loginAsAdmin(page)

      await adminQuestions.addDateQuestion({questionName: 'birthday-date-q'})
      await adminQuestions.addDateQuestion({questionName: 'todays-date-q'})

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
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerDateQuestion('2022-07-04', 0)
      await applicantQuestions.answerDateQuestion('1990-10-10', 1)
      await applicantQuestions.clickNext()

      await applicantQuestions.submitFromReviewPage()
    })

    test('with unanswered optional question submits successfully', async ({
      applicantQuestions,
    }) => {
      // Only answer second question.
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerDateQuestion('1990-10-10', 1)
      await applicantQuestions.clickNext()

      await applicantQuestions.submitFromReviewPage()
    })

    test('has no accessiblity violations', async ({
      page,
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(programName)

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

  async function setUpDateQuestionWithMinMaxValidation(
    programName: string,
    page: Page,
    adminQuestions: AdminQuestions,
    adminPrograms: AdminPrograms,
  ) {
    // As admin, create program with single date question with validation parameters.
    await loginAsAdmin(page)

    await adminQuestions.addDateQuestion({
      questionName: 'date-q-with-validation',
    })
    await adminQuestions.gotoQuestionEditPage('date-q-with-validation')
    await waitForPageJsLoad(page)

    // Set min date to custom date
    await page.selectOption('#min-date-type', {value: 'CUSTOM'})
    await page.locator('#min-custom-date-day').fill('1')
    await page.locator('#min-custom-date-month').selectOption('1')
    await page.locator('#min-custom-date-year').fill('2025')
    // Set max date to application date
    await page.selectOption('#max-date-type', {value: 'ANY'})
    await adminQuestions.clickSubmitButtonAndNavigate('Update')
    await adminPrograms.addAndPublishProgramWithQuestions(
      ['date-q-with-validation'],
      programName,
    )

    await logout(page)
  }
})
