import {Page} from 'playwright'
import {
  AdminPrograms,
  AdminQuestions,
  ApplicantQuestions,
  loginAsAdmin,
  loginAsGuest,
  logout,
  resetSession,
  selectApplicantLanguage,
  startSession,
  validateAccessibility,
  validateScreenshot,
} from './support'

describe('Number question for applicant flow', () => {
  let pageObject: Page
  const numberInputError = 'div.cf-question-number-error'

  beforeAll(async () => {
    const {page} = await startSession()
    pageObject = page
  })

  afterEach(async () => {
    await resetSession(pageObject)
  })

  describe('single number question', () => {
    let applicantQuestions: ApplicantQuestions
    const programName = 'test program for single number'

    beforeAll(async () => {
      // As admin, create program with single number question.
      await loginAsAdmin(pageObject)
      const adminQuestions = new AdminQuestions(pageObject)
      const adminPrograms = new AdminPrograms(pageObject)
      applicantQuestions = new ApplicantQuestions(pageObject)

      await adminQuestions.addNumberQuestion({
        questionName: 'fave-number-q',
      })
      await adminPrograms.addAndPublishProgramWithQuestions(
        ['fave-number-q'],
        programName,
      )

      await logout(pageObject)
    })

    it('with valid number submits successfully', async () => {
      await loginAsGuest(pageObject)
      await selectApplicantLanguage(pageObject, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerNumberQuestion('8')
      await applicantQuestions.clickNext()

      await applicantQuestions.submitFromReviewPage(programName)
    })

    it('with no input does not submit', async () => {
      await loginAsGuest(pageObject)
      await selectApplicantLanguage(pageObject, 'English')

      await applicantQuestions.applyProgram(programName)
      // Leave field blank.
      await applicantQuestions.clickNext()

      const numberId = '.cf-question-number'
      expect(await pageObject.innerText(numberId)).toContain(
        'This question is required.',
      )
      await validateScreenshot(pageObject)
    })

    it('with non-numeric inputs does not submit', async () => {
      await loginAsGuest(pageObject)
      await selectApplicantLanguage(pageObject, 'English')

      await applicantQuestions.applyProgram(programName)
      const testValues = ['12e3', '12E3', '-123', '1.23']

      for (const testValue of testValues) {
        await applicantQuestions.answerNumberQuestion(testValue)
        await applicantQuestions.clickNext()
        expect(await pageObject.isHidden(numberInputError)).toEqual(false)
        await applicantQuestions.answerNumberQuestion('')
      }
      await validateScreenshot(pageObject)
    })
  })

  describe('multiple number questions', () => {
    let applicantQuestions: ApplicantQuestions
    const programName = 'test program for multiple numbers'

    beforeAll(async () => {
      await loginAsAdmin(pageObject)
      const adminQuestions = new AdminQuestions(pageObject)
      const adminPrograms = new AdminPrograms(pageObject)
      applicantQuestions = new ApplicantQuestions(pageObject)

      await adminQuestions.addNumberQuestion({
        questionName: 'my-number-q',
      })
      await adminQuestions.addNumberQuestion({
        questionName: 'your-number-q',
      })

      await adminPrograms.addProgram(programName)
      await adminPrograms.editProgramBlockWithOptional(
        programName,
        'Optional question block',
        ['my-number-q'],
        'your-number-q', // optional
      )
      await adminPrograms.gotoAdminProgramsPage()
      await adminPrograms.publishAllPrograms()

      await logout(pageObject)
    })

    it('with valid numbers submits successfully', async () => {
      await loginAsGuest(pageObject)
      await selectApplicantLanguage(pageObject, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerNumberQuestion('100', 0)
      await applicantQuestions.answerNumberQuestion('33', 1)
      await applicantQuestions.clickNext()
      await validateScreenshot(pageObject)

      await applicantQuestions.submitFromReviewPage(programName)
    })

    it('with unanswered optional question submits successfully', async () => {
      await loginAsGuest(pageObject)
      await selectApplicantLanguage(pageObject, 'English')

      // Only answer required question.
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerNumberQuestion('33', 1)
      await applicantQuestions.clickNext()
      await validateScreenshot(pageObject)

      await applicantQuestions.submitFromReviewPage(programName)
    })

    it('with first invalid does not submit', async () => {
      await loginAsGuest(pageObject)
      await selectApplicantLanguage(pageObject, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerNumberQuestion('-10', 0)
      await applicantQuestions.answerNumberQuestion('33', 1)
      await applicantQuestions.clickNext()
      await validateScreenshot(pageObject)

      expect(await pageObject.isHidden(numberInputError)).toEqual(false)
    })

    it('with second invalid does not submit', async () => {
      await loginAsGuest(pageObject)
      await selectApplicantLanguage(pageObject, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerNumberQuestion('10', 0)
      await applicantQuestions.answerNumberQuestion('-5', 1)
      await applicantQuestions.clickNext()
      await validateScreenshot(pageObject)

      expect(await pageObject.isHidden(numberInputError + ' >> nth=1')).toEqual(
        false,
      )
    })

    it('has no accessiblity violations', async () => {
      await loginAsGuest(pageObject)
      await selectApplicantLanguage(pageObject, 'English')

      await applicantQuestions.applyProgram(programName)

      await validateAccessibility(pageObject)
    })
  })
})
