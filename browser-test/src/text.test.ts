import { Page } from 'playwright'
import { AdminPrograms, AdminQuestions, ApplicantQuestions, loginAsAdmin, loginAsGuest, logout, resetSession, selectApplicantLanguage, startSession, validateAccessibility, validateScreenshot, } from './support'

describe('Text question for applicant flow', () => {
  let pageObject: Page

  beforeAll(async () => {
    const {page} = await startSession()
    pageObject = page
  })

  afterEach(async () => {
    await resetSession(pageObject)
  })

  describe('single text question', () => {
    let applicantQuestions: ApplicantQuestions
    const programName = 'test program for single text q'

    beforeAll(async () => {
      // As admin, create program with a free form text question.
      await loginAsAdmin(pageObject)
      const adminQuestions = new AdminQuestions(pageObject)
      const adminPrograms = new AdminPrograms(pageObject)
      applicantQuestions = new ApplicantQuestions(pageObject)

      await adminQuestions.addTextQuestion({
        questionName: 'text-q',
        minNum: 5,
        maxNum: 20,
      })
      await adminPrograms.addAndPublishProgramWithQuestions(
        ['text-q'],
        programName,
      )

      await logout(pageObject)
    })

    it('with text submits successfully', async () => {
      await loginAsGuest(pageObject)
      await selectApplicantLanguage(pageObject, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerTextQuestion('I love CiviForm!')
      await applicantQuestions.clickNext()

      await applicantQuestions.submitFromReviewPage(programName)
    })

    it('with empty text does not submit', async () => {
      await loginAsGuest(pageObject)
      await selectApplicantLanguage(pageObject, 'English')

      await applicantQuestions.applyProgram(programName)

      // Click next without inputting anything
      await applicantQuestions.clickNext()

      const textId = '.cf-question-text'
      expect(await pageObject.innerText(textId)).toContain(
        'This question is required.',
      )
      await validateScreenshot(pageObject);
    })

    it('with too short text does not submit', async () => {
      await loginAsGuest(pageObject)
      await selectApplicantLanguage(pageObject, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerTextQuestion('hi')
      await applicantQuestions.clickNext()

      const textId = '.cf-question-text'
      expect(await pageObject.innerText(textId)).toContain(
        'Must contain at least 5 characters.',
      )
      await validateScreenshot(pageObject);
    })

    it('with too long text does not submit', async () => {
      await loginAsGuest(pageObject)
      await selectApplicantLanguage(pageObject, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerTextQuestion(
        'A long string that exceeds the character limit',
      )
      await applicantQuestions.clickNext()

      const textId = '.cf-question-text'
      expect(await pageObject.innerText(textId)).toContain(
        'Must contain at most 20 characters.',
      )
      await validateScreenshot(pageObject);
    })
  })

  describe('multiple text questions', () => {
    let applicantQuestions: ApplicantQuestions
    const programName = 'test program for multiple text qs'

    beforeAll(async () => {
      await loginAsAdmin(pageObject)
      const adminQuestions = new AdminQuestions(pageObject)
      const adminPrograms = new AdminPrograms(pageObject)
      applicantQuestions = new ApplicantQuestions(pageObject)

      await adminQuestions.addTextQuestion({
        questionName: 'first-text-q',
        minNum: 5,
        maxNum: 20,
      })
      await adminQuestions.addTextQuestion({
        questionName: 'second-text-q',
        minNum: 5,
        maxNum: 20,
      })

      await adminPrograms.addProgram(programName)
      await adminPrograms.editProgramBlockWithOptional(
        programName,
        'Optional question block',
        ['second-text-q'],
        'first-text-q', // optional
      )
      await adminPrograms.gotoAdminProgramsPage()
      await adminPrograms.publishAllPrograms()

      await logout(pageObject)
    })

    it('with both selections submits successfully', async () => {
      await loginAsGuest(pageObject)
      await selectApplicantLanguage(pageObject, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerTextQuestion('I love CiviForm!', 0)
      await applicantQuestions.answerTextQuestion('You love CiviForm!', 1)
      await applicantQuestions.clickNext()
      await validateScreenshot(pageObject);

      await applicantQuestions.submitFromReviewPage(programName)
    })

    it('with unanswered optional question submits successfully', async () => {
      await loginAsGuest(pageObject)
      await selectApplicantLanguage(pageObject, 'English')

      // Only answer second question. First is optional.
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerTextQuestion('You love CiviForm!', 1)
      await applicantQuestions.clickNext()
      await validateScreenshot(pageObject);

      await applicantQuestions.submitFromReviewPage(programName)
    })

    it('with first invalid does not submit', async () => {
      await loginAsGuest(pageObject)
      await selectApplicantLanguage(pageObject, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerTextQuestion(
        'A long string that exceeds the character limit',
        0,
      )
      await applicantQuestions.answerTextQuestion('You love CiviForm!', 1)
      await applicantQuestions.clickNext()

      const textId = '.cf-question-text'
      expect(await pageObject.innerText(textId)).toContain(
        'Must contain at most 20 characters.',
      )
      await validateScreenshot(pageObject);
    })

    it('with second invalid does not submit', async () => {
      await loginAsGuest(pageObject)
      await selectApplicantLanguage(pageObject, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerTextQuestion('I love CiviForm!', 0)
      await applicantQuestions.answerTextQuestion(
        'A long string that exceeds the character limit',
        1,
      )
      await applicantQuestions.clickNext()

      const textId = `.cf-question-text >> nth=1`
      expect(await pageObject.innerText(textId)).toContain(
        'Must contain at most 20 characters.',
      )
      await validateScreenshot(pageObject);
    })

    it('has no accessiblity violations', async () => {
      await loginAsGuest(pageObject)
      await selectApplicantLanguage(pageObject, 'English')

      await applicantQuestions.applyProgram(programName)

      await validateAccessibility(pageObject)
    })
  })
})
