import {test, expect} from '@playwright/test'
import {
  createTestContext,
  disableFeatureFlag,
  enableFeatureFlag,
  loginAsAdmin,
  logout,
  validateAccessibility,
  validateScreenshot,
} from '../../support'

test.describe('Id question for applicant flow', () => {
  const ctx = createTestContext(/* clearDb= */ false)

  test.describe('single id question', () => {
    const programName = 'Test program for single id'

    test.beforeAll(async () => {
      await setUpForSingleInput(programName)
    })

    test.beforeEach(async () => {
      const {page} = ctx
      await disableFeatureFlag(page, 'north_star_applicant_ui')
    })

    test('validate screenshot', async () => {
      const {page, applicantQuestions} = ctx
      await applicantQuestions.applyProgram(programName)

      await validateScreenshot(page, 'id')
    })

    test('validate screenshot with errors', async () => {
      const {page, applicantQuestions} = ctx
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.clickNext()

      await validateScreenshot(page, 'id-errors')
    })

    test('with id submits successfully', async () => {
      const {applicantQuestions} = ctx
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerIdQuestion('12345')
      await applicantQuestions.clickNext()

      await applicantQuestions.submitFromReviewPage()
    })

    test('with empty id does not submit', async () => {
      const {page, applicantQuestions} = ctx
      await applicantQuestions.applyProgram(programName)

      // Click next without inputting anything
      await applicantQuestions.clickNext()

      const identificationId = '.cf-question-id'
      expect(await page.innerText(identificationId)).toContain(
        'This question is required.',
      )
    })

    test('with too short id does not submit', async () => {
      const {page, applicantQuestions} = ctx
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerIdQuestion('123')
      await applicantQuestions.clickNext()

      const identificationId = '.cf-question-id'
      expect(await page.innerText(identificationId)).toContain(
        'Must contain at least 5 characters.',
      )
    })

    test('with too long id does not submit', async () => {
      const {page, applicantQuestions} = ctx
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerIdQuestion('123456')
      await applicantQuestions.clickNext()

      const identificationId = '.cf-question-id'
      expect(await page.innerText(identificationId)).toContain(
        'Must contain at most 5 characters.',
      )
    })

    test('with non-numeric characters does not submit', async () => {
      const {page, applicantQuestions} = ctx
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerIdQuestion('abcde')
      await applicantQuestions.clickNext()

      const identificationId = '.cf-question-id'
      expect(await page.innerText(identificationId)).toContain(
        'Must contain only numbers.',
      )
    })
  })

  test.describe('multiple id questions', () => {
    const programName = 'Test program for multiple ids'

    test.beforeAll(async () => {
      const {page, adminQuestions, adminPrograms} = ctx
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

    test('with both id inputs submits successfully', async () => {
      const {applicantQuestions} = ctx
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerIdQuestion('12345', 0)
      await applicantQuestions.answerIdQuestion('67890', 1)
      await applicantQuestions.clickNext()

      await applicantQuestions.submitFromReviewPage()
    })

    test('with unanswered optional question submits successfully', async () => {
      const {applicantQuestions} = ctx
      // Only answer second question. First is optional.
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerIdQuestion('67890', 1)
      await applicantQuestions.clickNext()

      await applicantQuestions.submitFromReviewPage()
    })

    test('with first invalid does not submit', async () => {
      const {page, applicantQuestions} = ctx
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerIdQuestion('abcde', 0)
      await applicantQuestions.answerIdQuestion('67890', 1)
      await applicantQuestions.clickNext()

      const identificationId = '.cf-question-id'
      expect(await page.innerText(identificationId)).toContain(
        'Must contain only numbers.',
      )
    })

    test('with second invalid does not submit', async () => {
      const {page, applicantQuestions} = ctx
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerIdQuestion('67890', 0)
      await applicantQuestions.answerIdQuestion('abcde', 1)
      await applicantQuestions.clickNext()

      const identificationId = `.cf-question-id >> nth=1`
      expect(await page.innerText(identificationId)).toContain(
        'Must contain only numbers.',
      )
    })

    test('has no accessiblity violations', async () => {
      const {page, applicantQuestions} = ctx
      await applicantQuestions.applyProgram(programName)

      await validateAccessibility(page)
    })
  })

  test.describe('single id question with north star flag enabled', () => {
    const programName = 'Test program for single id'

    test.beforeAll(async () => {
      await setUpForSingleInput(programName);
    })

    test.beforeEach(async () => {
      const {page} = ctx
      await enableFeatureFlag(page, 'north_star_applicant_ui')
    })

    test(
      'validate screenshot with north star flag enabled',
      {tag: ['@northstar']},
      async () => {
        const {page, applicantQuestions} = ctx
        await applicantQuestions.applyProgram(programName)

        await test.step('Screenshot without errors', async () => {
          await validateScreenshot(
            page,
            'id-north-star',
            /* fullPage= */ true,
            /* mobileScreenshot= */ true,
          )
        })

        await test.step('Screenshot with errors', async () => {
          await applicantQuestions.clickContinue()
          await validateScreenshot(
            page,
            'id-errors-north-star',
            /* fullPage= */ true,
            /* mobileScreenshot= */ true,
          )
        })
      },
    )
  })

  async function setUpForSingleInput(programName: string) {
    const {page, adminQuestions, adminPrograms} = ctx
    // As admin, create program with single id question.
    await loginAsAdmin(page)

    await adminQuestions.addIdQuestion({
      questionName: 'id-q',
      minNum: 5,
      maxNum: 5,
    })
    await adminPrograms.addAndPublishProgramWithQuestions(
      ['id-q'],
      programName,
    )

    await logout(page)
  }
})
