import {
  AdminPrograms,
  AdminQuestions,
  ApplicantQuestions,
  createBrowserContext,
  loginAsAdmin,
  loginAsGuest,
  logout,
  selectApplicantLanguage,
  validateAccessibility,
  validateScreenshot,
} from './support'

const NAME_FIRST = '.cf-name-first'
const NAME_LAST = '.cf-name-last'

describe('name applicant flow', () => {
  const ctx = createBrowserContext(/* clearDb= */ false)

  describe('single required name question', () => {
    let applicantQuestions: ApplicantQuestions
    const programName = 'test program for single name'

    beforeAll(async () => {
      await loginAsAdmin(ctx.page)
      const adminQuestions = new AdminQuestions(ctx.page)
      const adminPrograms = new AdminPrograms(ctx.page)
      applicantQuestions = new ApplicantQuestions(ctx.page)

      await adminQuestions.addNameQuestion({
        questionName: 'name-test-q',
      })
      await adminPrograms.addAndPublishProgramWithQuestions(
        ['name-test-q'],
        programName,
      )
      await logout(ctx.page)
    })

    it('validate screenshot', async () => {
      await loginAsGuest(ctx.page)
      await selectApplicantLanguage(ctx.page, 'English')

      await applicantQuestions.applyProgram(programName)

      await validateScreenshot(ctx.page, 'name')
    })

    it('validate screenshot with errors', async () => {
      await loginAsGuest(ctx.page)
      await selectApplicantLanguage(ctx.page, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.clickNext()

      await validateScreenshot(ctx.page, 'name-errors')
    })

    it('does not show errors initially', async () => {
      await loginAsGuest(ctx.page)
      await selectApplicantLanguage(ctx.page, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerNameQuestion('', '', '')
      let error = await ctx.page.$(`${NAME_FIRST}-error`)
      expect(await error?.isHidden()).toEqual(true)
      error = await ctx.page.$(`${NAME_LAST}-error`)
      expect(await error?.isHidden()).toEqual(true)
    })

    it('with valid name does submit', async () => {
      await loginAsGuest(ctx.page)
      await selectApplicantLanguage(ctx.page, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerNameQuestion('Tommy', 'Pickles', '')
      await applicantQuestions.clickNext()

      await applicantQuestions.submitFromReviewPage()
    })

    it('with empty name does not submit', async () => {
      await loginAsGuest(ctx.page)
      await selectApplicantLanguage(ctx.page, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerNameQuestion('', '', '')
      await applicantQuestions.clickNext()

      let error = await ctx.page.$(`${NAME_FIRST}-error`)
      expect(await error?.isHidden()).toEqual(false)
      error = await ctx.page.$(`${NAME_LAST}-error`)
      expect(await error?.isHidden()).toEqual(false)
    })
  })

  describe('multiple name questions', () => {
    let applicantQuestions: ApplicantQuestions
    const programName = 'test program for multiple names'

    beforeAll(async () => {
      await loginAsAdmin(ctx.page)
      const adminQuestions = new AdminQuestions(ctx.page)
      const adminPrograms = new AdminPrograms(ctx.page)
      applicantQuestions = new ApplicantQuestions(ctx.page)

      await adminQuestions.addNameQuestion({
        questionName: 'name-test-a-q',
      })
      await adminQuestions.addNameQuestion({
        questionName: 'name-test-b-q',
      })
      await adminPrograms.addAndPublishProgramWithQuestions(
        ['name-test-a-q', 'name-test-b-q'],
        programName,
      )

      await logout(ctx.page)
    })

    it('with valid name does submit', async () => {
      await loginAsGuest(ctx.page)
      await selectApplicantLanguage(ctx.page, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerNameQuestion('Tommy', 'Pickles', '', 0)
      await applicantQuestions.answerNameQuestion('Chuckie', 'Finster', '', 1)
      await applicantQuestions.clickNext()

      await applicantQuestions.submitFromReviewPage()
    })

    it('with first invalid does not submit', async () => {
      await loginAsGuest(ctx.page)
      await selectApplicantLanguage(ctx.page, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerNameQuestion('', '', '', 0)
      await applicantQuestions.answerNameQuestion('Chuckie', 'Finster', '', 1)
      await applicantQuestions.clickNext()

      // First question has errors.
      let error = await ctx.page.$(`${NAME_FIRST}-error >> nth=0`)
      expect(await error?.isHidden()).toEqual(false)
      error = await ctx.page.$(`${NAME_LAST}-error >> nth=0`)
      expect(await error?.isHidden()).toEqual(false)

      // Second question has no errors.
      error = await ctx.page.$(`${NAME_FIRST}-error >> nth=1`)
      expect(await error?.isHidden()).toEqual(true)
      error = await ctx.page.$(`${NAME_LAST}-error >> nth=1`)
      expect(await error?.isHidden()).toEqual(true)
    })

    it('with second invalid does not submit', async () => {
      await loginAsGuest(ctx.page)
      await selectApplicantLanguage(ctx.page, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerNameQuestion('Tommy', 'Pickles', '', 0)
      await applicantQuestions.answerNameQuestion('', '', '', 1)
      await applicantQuestions.clickNext()

      // First question has no errors.
      let error = await ctx.page.$(`${NAME_FIRST}-error >> nth=0`)
      expect(await error?.isHidden()).toEqual(true)
      error = await ctx.page.$(`${NAME_LAST}-error >> nth=0`)
      expect(await error?.isHidden()).toEqual(true)

      // Second question has errors.
      error = await ctx.page.$(`${NAME_FIRST}-error >> nth=1`)
      expect(await error?.isHidden()).toEqual(false)
      error = await ctx.page.$(`${NAME_LAST}-error >> nth=1`)
      expect(await error?.isHidden()).toEqual(false)
    })

    it('has no accessiblity violations', async () => {
      await loginAsGuest(ctx.page)
      await selectApplicantLanguage(ctx.page, 'English')

      await applicantQuestions.applyProgram(programName)

      await validateAccessibility(ctx.page)
    })
  })

  // One optional name followed by one required name.
  describe('optional name question', () => {
    let applicantQuestions: ApplicantQuestions
    const programName = 'test program for optional name'

    beforeAll(async () => {
      await loginAsAdmin(ctx.page)
      const adminQuestions = new AdminQuestions(ctx.page)
      const adminPrograms = new AdminPrograms(ctx.page)
      applicantQuestions = new ApplicantQuestions(ctx.page)

      await adminQuestions.addNameQuestion({
        questionName: 'name-test-optional-q',
      })
      await adminQuestions.addNameQuestion({
        questionName: 'name-test-required-q',
      })
      await adminPrograms.addProgram(programName)
      await adminPrograms.editProgramBlockWithOptional(
        programName,
        'Optional question block',
        ['name-test-required-q'],
        'name-test-optional-q',
      )
      await adminPrograms.gotoAdminProgramsPage()
      await adminPrograms.publishAllPrograms()

      await logout(ctx.page)
    })

    it('with valid required name does submit', async () => {
      await loginAsGuest(ctx.page)
      await selectApplicantLanguage(ctx.page, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerNameQuestion('Tommy', 'Pickles', '', 1)
      await applicantQuestions.clickNext()

      await applicantQuestions.submitFromReviewPage()
    })

    it('with invalid optional name does not submit', async () => {
      await loginAsGuest(ctx.page)
      await selectApplicantLanguage(ctx.page, 'English')

      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerNameQuestion('Tommy', '', '', 0)
      await applicantQuestions.answerNameQuestion('Tommy', 'Pickles', '', 1)
      await applicantQuestions.clickNext()

      // Optional question has an error.
      const error = await ctx.page.$(`${NAME_LAST}-error >> nth=0`)
      expect(await error?.isHidden()).toEqual(false)
    })

    describe('with invalid required name', () => {
      beforeEach(async () => {
        await loginAsGuest(ctx.page)
        await selectApplicantLanguage(ctx.page, 'English')

        await applicantQuestions.applyProgram(programName)
        await applicantQuestions.answerNameQuestion('', '', '', 1)
        await applicantQuestions.clickNext()
      })

      it('does not submit', async () => {
        // Second question has errors.
        let error = await ctx.page.$(`${NAME_FIRST}-error >> nth=1`)
        expect(await error?.isHidden()).toEqual(false)
        error = await ctx.page.$(`${NAME_LAST}-error >> nth=1`)
        expect(await error?.isHidden()).toEqual(false)
      })

      it('optional has no errors', async () => {
        // First question has no errors.
        let error = await ctx.page.$(`${NAME_FIRST}-error >> nth=0`)
        expect(await error?.isHidden()).toEqual(true)
        error = await ctx.page.$(`${NAME_LAST}-error >> nth=0`)
        expect(await error?.isHidden()).toEqual(true)
      })
    })
  })
})
