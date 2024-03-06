import {test, expect} from '../../fixtures/custom_fixture'
import {
  loginAsAdmin,
  logout,
  validateAccessibility,
  validateScreenshot,
} from '../../support'

test.describe('Email question for applicant flow', {tag: ['@migrated']}, () => {
  test.describe('single email question', () => {
    const programName = 'Test program for single email'

    test.beforeEach(async ({page, adminQuestions, adminPrograms}) => {
      // beforeAll
      // As admin, create program with single email question.
      await loginAsAdmin(page)

      await adminQuestions.addEmailQuestion({questionName: 'general-email-q'})
      await adminPrograms.addAndPublishProgramWithQuestions(
        ['general-email-q'],
        programName,
      )

      await logout(page)

      // beforeEach
    })

    test('validate screenshot', async ( {page, applicantQuestions}) => {
      await applicantQuestions.applyProgram(programName)

      await validateScreenshot(page, 'email')
    })

    test('validate screenshot with errors', async ( {page, applicantQuestions}) => {
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.clickNext()

      await validateScreenshot(page, 'email-errors')
    })

    test('with email input submits successfully', async ({applicantQuestions}) => {
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerEmailQuestion('my_email@civiform.gov')
      await applicantQuestions.clickNext()

      await applicantQuestions.submitFromReviewPage()
    })

    test('with no email input does not submit', async ({page, applicantQuestions}) => {
      await applicantQuestions.applyProgram(programName)
      // Click next without inputting anything.
      await applicantQuestions.clickNext()

      const emailId = '.cf-question-email'
      expect(await page.innerText(emailId)).toContain(
        'This question is required.',
      )
      expect(await page.innerHTML(emailId)).toContain('autofocus')
    })
  })

  test.describe('multiple email questions', () => {
    const programName = 'Test program for multiple emails'

    test.beforeEach(async ({page, adminQuestions, adminPrograms}) => {
      // beforeAll
      await loginAsAdmin(page)

      await adminQuestions.addEmailQuestion({questionName: 'my-email-q'})
      await adminQuestions.addEmailQuestion({questionName: 'your-email-q'})

      await adminPrograms.addProgram(programName)
      await adminPrograms.editProgramBlockWithOptional(
        programName,
        'Optional question block',
        ['my-email-q'],
        'your-email-q', // optional
      )
      await adminPrograms.publishAllDrafts()

      await logout(page)

      // beforeEach
    })

    test('with email inputs submits successfully', async ({applicantQuestions}) => {
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerEmailQuestion('your_email@civiform.gov', 0)
      await applicantQuestions.answerEmailQuestion('my_email@civiform.gov', 1)
      await applicantQuestions.clickNext()

      await applicantQuestions.submitFromReviewPage()
    })

    test('with unanswered optional question submits successfully', async ({applicantQuestions}) => {
      // Only answer second question. First is optional.
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerEmailQuestion('my_email@civiform.gov', 1)
      await applicantQuestions.clickNext()

      await applicantQuestions.submitFromReviewPage()
    })

    test('has no accessiblity violations', async ({page, applicantQuestions}) => {
      await applicantQuestions.applyProgram(programName)

      await validateAccessibility(page)
    })
  })
})
