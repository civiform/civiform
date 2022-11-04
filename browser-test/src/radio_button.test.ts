import {
  createTestContext,
  loginAsAdmin,
  loginAsGuest,
  logout,
  selectApplicantLanguage,
  validateAccessibility,
  validateScreenshot,
} from './support'

describe('Radio button question for applicant flow', () => {
  const ctx = createTestContext(/* clearDb= */ false)

  describe('single radio button question', () => {
    const programName = 'test-program-for-single-radio-button'

    beforeAll(async () => {
      const {page, adminQuestions, adminPrograms} = ctx
      // As admin, create program with radio button question.
      await loginAsAdmin(page)

      await adminQuestions.addRadioButtonQuestion({
        questionName: 'ice-cream-radio-q',
        options: ['matcha', 'strawberry', 'vanilla'],
      })
      await adminPrograms.addAndPublishProgramWithQuestions(
        ['ice-cream-radio-q'],
        programName,
      )

      await logout(page)
    })

    it('Updates options in preview', async () => {
      const {page, adminQuestions} = ctx
      await loginAsAdmin(page)

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
      const {page, applicantQuestions} = ctx
      await loginAsGuest(page)
      await selectApplicantLanguage(page, 'English')

      await applicantQuestions.applyProgram(programName)

      await validateScreenshot(page, 'radio-button')
    })

    it('validate screenshot with errors', async () => {
      const {page, applicantQuestions} = ctx
      await loginAsGuest(page)
      await selectApplicantLanguage(page, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.clickNext()

      await validateScreenshot(page, 'radio-button-errors')
    })

    it('with selection submits successfully', async () => {
      const {page, applicantQuestions} = ctx
      await loginAsGuest(page)
      await selectApplicantLanguage(page, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerRadioButtonQuestion('matcha')
      await applicantQuestions.clickNext()

      await applicantQuestions.submitFromReviewPage()
    })

    it('with empty selection does not submit', async () => {
      const {page, applicantQuestions} = ctx
      await loginAsGuest(page)
      await selectApplicantLanguage(page, 'English')

      await applicantQuestions.applyProgram(programName)

      // Click next without inputting anything
      await applicantQuestions.clickNext()

      const radioButtonId = '.cf-question-radio'
      expect(await page.innerText(radioButtonId)).toContain(
        'This question is required.',
      )
    })
  })

  describe('multiple radio button questions', () => {
    const programName = 'test-program-for-multiple-radio-button-qs'

    beforeAll(async () => {
      const {page, adminQuestions, adminPrograms} = ctx
      await loginAsAdmin(page)

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
      await adminPrograms.publishAllPrograms()

      await logout(page)
    })

    it('with both selections submits successfully', async () => {
      const {page, applicantQuestions} = ctx
      await loginAsGuest(page)
      await selectApplicantLanguage(page, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerRadioButtonQuestion('matcha')
      await applicantQuestions.answerRadioButtonQuestion('mountains')
      await applicantQuestions.clickNext()

      await applicantQuestions.submitFromReviewPage()
    })

    it('with unanswered optional question submits successfully', async () => {
      const {page, applicantQuestions} = ctx
      await loginAsGuest(page)
      await selectApplicantLanguage(page, 'English')

      // Only answer second question. First is optional.
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerRadioButtonQuestion('matcha')
      await applicantQuestions.clickNext()

      await applicantQuestions.submitFromReviewPage()
    })

    it('has no accessiblity violations', async () => {
      const {page, applicantQuestions} = ctx
      await loginAsGuest(page)
      await selectApplicantLanguage(page, 'English')

      await applicantQuestions.applyProgram(programName)

      await validateAccessibility(page)
    })
  })
})
