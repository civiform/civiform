import {Page} from 'playwright';
import {
  AdminPrograms,
  AdminQuestions,
  ApplicantQuestions,
  loginAsAdmin,
  loginAsGuest,
  logout,
  selectApplicantLanguage,
  startSession,
  resetSession,
} from './support'

describe('Radio button question for applicant flow', () => {
  let pageObject: Page

  beforeAll(async () => {
    const {page} = await startSession()
    pageObject = page
  })

  afterEach(async () => {
    await resetSession(pageObject)
  })

  describe('single radio button question', () => {
    let applicantQuestions: ApplicantQuestions
    const programName = 'test program for single radio button'

    beforeAll(async () => {
      // As admin, create program with radio button question.
      await loginAsAdmin(pageObject)
      const adminQuestions = new AdminQuestions(pageObject)
      const adminPrograms = new AdminPrograms(pageObject)
      applicantQuestions = new ApplicantQuestions(pageObject)

      await adminQuestions.addRadioButtonQuestion({
        questionName: 'ice-cream-radio-q',
        options: ['matcha', 'strawberry', 'vanilla'],
      })
      await adminPrograms.addAndPublishProgramWithQuestions(
        ['ice-cream-radio-q'],
        programName,
      )

      await logout(pageObject)
    })

    it('with selection submits successfully', async () => {
      await loginAsGuest(pageObject)
      await selectApplicantLanguage(pageObject, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerRadioButtonQuestion('matcha')
      await applicantQuestions.clickNext()

      await applicantQuestions.submitFromReviewPage(programName)
    })

    it('with empty selection does not submit', async () => {
      await loginAsGuest(pageObject)
      await selectApplicantLanguage(pageObject, 'English')

      await applicantQuestions.applyProgram(programName)

      // Click next without inputting anything
      await applicantQuestions.clickNext()

      const radioButtonId = '.cf-question-radio'
      expect(await pageObject.innerText(radioButtonId)).toContain(
        'This question is required.',
      )
    })
  })

  describe('multiple radio button questions', () => {
    let applicantQuestions: ApplicantQuestions
    const programName = 'test program for multiple radio button qs'

    beforeAll(async () => {
      await loginAsAdmin(pageObject)
      const adminQuestions = new AdminQuestions(pageObject)
      const adminPrograms = new AdminPrograms(pageObject)
      applicantQuestions = new ApplicantQuestions(pageObject)

      await adminQuestions.addRadioButtonQuestion({
        questionName: 'fave-ice-cream-q',
        options: ['matcha', 'strawberry', 'vanilla'],
      })

      await adminQuestions.addCheckboxQuestion({
        questionName: 'fave-vacation-q',
        options: ['beach', 'mountains', 'city', 'cruise'],
      })

      await adminPrograms.addProgram(programName)
      await adminPrograms.editProgramBlockWithOptional(
        programName,
        'Optional question block',
        ['fave-ice-cream-q'],
        'fave-vacation-q', // optional
      )
      await adminPrograms.gotoAdminProgramsPage()
      await adminPrograms.publishAllPrograms()

      await logout(pageObject)
    })

    it('with both selections submits successfully', async () => {
      await loginAsGuest(pageObject)
      await selectApplicantLanguage(pageObject, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerRadioButtonQuestion('matcha')
      await applicantQuestions.answerRadioButtonQuestion('mountains')
      await applicantQuestions.clickNext()

      await applicantQuestions.submitFromReviewPage(programName)
    })

    it('with unanswered optional question submits successfully', async () => {
      await loginAsGuest(pageObject)
      await selectApplicantLanguage(pageObject, 'English')

      // Only answer second question. First is optional.
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerRadioButtonQuestion('matcha')
      await applicantQuestions.clickNext()

      await applicantQuestions.submitFromReviewPage(programName)
    })
  })
})
