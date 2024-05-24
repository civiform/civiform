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

test.describe('Date question for applicant flow', () => {
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

  test.describe(
    'single date question with North Star flag enabled',
    {tag: ['@northstar']},
    () => {
      const programName = 'Test program for single date'

      test.beforeEach(async ({page, adminQuestions, adminPrograms}) => {
        await setUpSingleDateQuestion(
          programName,
          page,
          adminQuestions,
          adminPrograms,
        )
        await enableFeatureFlag(page, 'north_star_applicant_ui')
      })

      test('validate screenshot', async ({page, applicantQuestions}) => {
        await applicantQuestions.applyProgram(programName)

        await test.step('Screenshot without errors', async () => {
          await validateScreenshot(
            page.getByTestId('questionRoot'),
            'date-north-star',
            /* fullPage= */ false,
            /* mobileScreenshot= */ true,
          )
        })

        await test.step('Screenshot with errors', async () => {
          await applicantQuestions.clickContinue()
          await validateScreenshot(
            page.getByTestId('questionRoot'),
            'date-errors-north-star',
            /* fullPage= */ false,
            /* mobileScreenshot= */ true,
          )
        })
      })

      test('with filled in date submits successfully', async ({
        applicantQuestions,
      }) => {
        await applicantQuestions.applyProgram(programName)
        await applicantQuestions.answerMemorableDateQuestion(
          '2022',
          '05 - May',
          '2',
        )
        await applicantQuestions.clickContinue()

        await applicantQuestions.submitFromReviewPage(
          /* northStarEnabled= */ true,
        )
      })

      test('Renders existing values', async ({page, applicantQuestions}) => {
        await applicantQuestions.applyProgram(programName)
        await applicantQuestions.answerMemorableDateQuestion(
          '2022',
          '05 - May',
          '2',
        )
        await applicantQuestions.clickContinue()
        // Return to page.
        await applicantQuestions.clickReview()
        await validateScreenshot(
          page,
          'date-filled-in-north-star',
          /* fullPage= */ true,
          /* mobileScreenshot= */ true,
        )
      })
    },
  )

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
