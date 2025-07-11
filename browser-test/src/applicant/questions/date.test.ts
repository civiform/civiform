import {Page} from '@playwright/test'
import {test, expect} from '../../support/civiform_fixtures'
import {
  AdminQuestions,
  AdminPrograms,
  loginAsAdmin,
  logout,
  validateAccessibility,
  validateScreenshot,
  disableFeatureFlag,
} from '../../support'

test.describe('Date question for applicant flow', () => {
  test.beforeEach(async ({page}) => {
    await disableFeatureFlag(page, 'north_star_applicant_ui')
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
})
