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

describe('Checkbox question for applicant flow', () => {
  let pageObject

  beforeAll(async () => {
    const {page} = await startSession()
    pageObject = page
  })

  afterEach(async () => {
    await resetSession(pageObject)
  })

  describe('single checkbox question', () => {
    let applicantQuestions
    const programName = 'test program for single checkbox'

    beforeAll(async () => {
      // As admin, create program with single checkbox question.
      await loginAsAdmin(pageObject)
      const adminQuestions = new AdminQuestions(pageObject)
      const adminPrograms = new AdminPrograms(pageObject)
      applicantQuestions = new ApplicantQuestions(pageObject)

      await adminQuestions.addCheckboxQuestion({
        questionName: 'checkbox-color-q',
        options: ['red', 'green', 'orange', 'blue'],
        minNumChoices: 1,
        maxNumChoices: 2,
      })
      await adminPrograms.addAndPublishProgramWithQuestions(
        ['checkbox-color-q'],
        programName,
      )

      await logout(pageObject)
    })

    it('with single checked box submits successfully', async () => {
      await loginAsGuest(pageObject)
      await selectApplicantLanguage(pageObject, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerCheckboxQuestion(['blue'])
      await applicantQuestions.clickNext()

      await applicantQuestions.submitFromReviewPage(programName)
    })

    it('with no checked boxes does not submit', async () => {
      await loginAsGuest(pageObject)
      await selectApplicantLanguage(pageObject, 'English')

      await applicantQuestions.applyProgram(programName)

      // No validation errors on first page load.
      const checkBoxError = '.cf-applicant-question-errors'
      expect(await pageObject.isHidden(checkBoxError)).toEqual(true)

      // Click next without selecting anything.
      await applicantQuestions.clickNext()

      // Check checkbox error and required error are present.
      expect(await pageObject.isHidden(checkBoxError)).toEqual(false)
      const checkboxId = '.cf-question-checkbox'
<<<<<<< HEAD
=======
      // Contains this substring. Is this brittle? Would have to update text every time. Or can I add an ID just for testing?
>>>>>>> f46b6f4a (Add checkbox browser test)
      expect(await pageObject.innerText(checkboxId)).toContain(
        'This question is required.',
      )
    })

    it('with greater than max allowed checked boxes does not submit', async () => {
      await loginAsGuest(pageObject)
      await selectApplicantLanguage(pageObject, 'English')

      await applicantQuestions.applyProgram(programName)
      const checkBoxError = '.cf-applicant-question-errors'
      // No validation errors on first page load.
      expect(await pageObject.isHidden(checkBoxError)).toEqual(true)

      // Max of two checked boxes are allowed, but we select three.
      await applicantQuestions.answerCheckboxQuestion([
        'blue',
        'green',
        'orange',
      ])
      await applicantQuestions.clickNext()

      // Check error is shown.
      expect(await pageObject.isHidden(checkBoxError)).toEqual(false)
    })
  })

  describe('multiple checkbox questions', () => {
    let applicantQuestions
    const programName = 'test program for multiple checkboxes'

    beforeAll(async () => {
      await loginAsAdmin(pageObject)
      const adminQuestions = new AdminQuestions(pageObject)
      const adminPrograms = new AdminPrograms(pageObject)
      applicantQuestions = new ApplicantQuestions(pageObject)

      await adminQuestions.addCheckboxQuestion({
        questionName: 'checkbox-fave-color-q',
        options: ['red', 'green', 'orange', 'blue'],
        minNumChoices: 1,
        maxNumChoices: 2,
      })
      await adminQuestions.addCheckboxQuestion({
        questionName: 'checkbox-vacation-q',
        options: ['beach', 'mountains', 'city', 'cruise'],
        minNumChoices: 1,
        maxNumChoices: 2,
      })

      await adminPrograms.addProgram(programName)
      await adminPrograms.editProgramBlockWithOptional(
        programName,
        'Optional question block',
        ['checkbox-fave-color-q'],
        'checkbox-vacation-q', // optional
      )
      await adminPrograms.gotoAdminProgramsPage()
      await adminPrograms.publishAllPrograms()

      await logout(pageObject)
    })

    it('with valid checkboxes submits successfully', async () => {
      await loginAsGuest(pageObject)
      await selectApplicantLanguage(pageObject, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerCheckboxQuestion(['blue'], 0)
      await applicantQuestions.answerCheckboxQuestion(['beach'], 1)
      await applicantQuestions.clickNext()

      await applicantQuestions.submitFromReviewPage(programName)
    })

<<<<<<< HEAD
    it('with unanswered optional question submits successfully', async () => {
=======
    it('unanswered optional question submits successfully', async () => {
>>>>>>> f46b6f4a (Add checkbox browser test)
      await loginAsGuest(pageObject)
      await selectApplicantLanguage(pageObject, 'English')

      // Only answer first question. Leave second question blank.
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerCheckboxQuestion(['red'], 0)
      await applicantQuestions.clickNext()

      await applicantQuestions.submitFromReviewPage(programName)
    })

    it('with first invalid does not submit', async () => {
      await loginAsGuest(pageObject)
      await selectApplicantLanguage(pageObject, 'English')

      await applicantQuestions.applyProgram(programName)
      const checkboxError = '.cf-applicant-question-errors'
      // No validation errors on first page load.
      expect(await pageObject.isHidden(checkboxError)).toEqual(true)

      // Max of 2 answers allowed.
      await applicantQuestions.answerCheckboxQuestion(
        ['red', 'green', 'orange'],
        0,
      )
      await applicantQuestions.answerCheckboxQuestion(['beach'], 1)
      await applicantQuestions.clickNext()

      expect(await pageObject.isHidden(checkboxError)).toEqual(false)
    })

    it('with second invalid does not submit', async () => {
      await loginAsGuest(pageObject)
      await selectApplicantLanguage(pageObject, 'English')

      await applicantQuestions.applyProgram(programName)
      const checkboxError = '.cf-applicant-question-errors'
      // No validation errors on first page load.
      expect(await pageObject.isHidden(checkboxError)).toEqual(true)

      await applicantQuestions.answerCheckboxQuestion(['red'], 0)
      // Max of 2 answers allowed.
      await applicantQuestions.answerCheckboxQuestion(
        ['beach', 'mountains', 'city'],
        1,
      )
      await applicantQuestions.clickNext()

      expect(await pageObject.isHidden(checkboxError)).toEqual(false)
    })
  })
})
