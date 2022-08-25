import {Page} from 'playwright'
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
  validateAccessibility,
  validateScreenshot,
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

    afterEach(async () => {
      await logout(pageObject)
    })

    it('Updates options in preview', async () => {
      await loginAsAdmin(pageObject)

      const adminQuestions = new AdminQuestions(pageObject)

      await adminQuestions.createRadioButtonQuestion(
        {
          questionName: 'not-used-in-test',
          questionText: 'Sample question text',
          helpText: 'Sample question help text',
          options: ['red', 'green', 'orange', 'blue'],
        },
        /* clickSubmit= */ false,
      )

      // Verify question preview has the default values.
      await adminQuestions.expectCommonPreviewValues({
        questionText: 'Sample question text',
        questionHelpText: 'Sample question help text',
      })
      await adminQuestions.expectPreviewOptions([
        'red',
        'green',
        'orange',
        'blue',
      ])

      // Empty options renders default text.
      await adminQuestions.createRadioButtonQuestion(
        {
          questionName: '',
          questionText: 'Sample question text',
          helpText: 'Sample question help text',
          options: [],
        },
        /* clickSubmit= */ false,
      )
      await adminQuestions.expectPreviewOptions(['Sample question option'])
    })

    it('validate screenshot', async () => {
      await loginAsGuest(pageObject)
      await selectApplicantLanguage(pageObject, 'English')

      await applicantQuestions.applyProgram(programName)

      await validateScreenshot(pageObject, 'radio-button')
    })

    it('validate screenshot with errors', async () => {
      await loginAsGuest(pageObject)
      await selectApplicantLanguage(pageObject, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.clickNext()

      await validateScreenshot(pageObject, 'radio-button-errors')
    })

    it('with selection submits successfully', async () => {
      await loginAsGuest(pageObject)
      await selectApplicantLanguage(pageObject, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerRadioButtonQuestion('matcha')
      await applicantQuestions.clickNext()

      await applicantQuestions.submitFromReviewPage()
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

      await applicantQuestions.submitFromReviewPage()
    })

    it('with unanswered optional question submits successfully', async () => {
      await loginAsGuest(pageObject)
      await selectApplicantLanguage(pageObject, 'English')

      // Only answer second question. First is optional.
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerRadioButtonQuestion('matcha')
      await applicantQuestions.clickNext()

      await applicantQuestions.submitFromReviewPage()
    })

    it('has no accessiblity violations', async () => {
      await loginAsGuest(pageObject)
      await selectApplicantLanguage(pageObject, 'English')

      await applicantQuestions.applyProgram(programName)

      await validateAccessibility(pageObject)
    })
  })
})
