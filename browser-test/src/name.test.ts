import {
  createTestContext,
  loginAsAdmin,
  logout,
  validateAccessibility,
  validateScreenshot,
} from './support'

const NAME_FIRST = '.cf-name-first'
const NAME_LAST = '.cf-name-last'

describe('name applicant flow', () => {
  const ctx = createTestContext(/* clearDb= */ false)

  describe('single required name question', () => {
    const programName = 'Test program for single name'

    beforeAll(async () => {
      const {page, adminQuestions, adminPrograms} = ctx
      await loginAsAdmin(page)

      await adminQuestions.addNameQuestion({
        questionName: 'name-test-q',
      })
      await adminPrograms.addAndPublishProgramWithQuestions(
        ['name-test-q'],
        programName,
      )
      await logout(page)
    })

    it('validate screenshot', async () => {
      const {page, applicantQuestions} = ctx
      await applicantQuestions.applyProgram(programName)

      await validateScreenshot(page, 'name')
    })

    it('validate screenshot with errors', async () => {
      const {page, applicantQuestions} = ctx
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.clickNext()

      await validateScreenshot(page, 'name-errors')
    })

    it('does not show errors initially', async () => {
      const {page, applicantQuestions} = ctx
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerNameQuestion('', '', '')
      let error = await page.$(`${NAME_FIRST}-error`)
      expect(await error?.isHidden()).toEqual(true)
      error = await page.$(`${NAME_LAST}-error`)
      expect(await error?.isHidden()).toEqual(true)
    })

    it('with valid name does submit', async () => {
      const {applicantQuestions} = ctx
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerNameQuestion('Tommy', 'Pickles', '')
      await applicantQuestions.clickNext()

      await applicantQuestions.submitFromReviewPage()
    })

    it('with empty name does not submit', async () => {
      const {page, applicantQuestions} = ctx
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerNameQuestion('', '', '')
      await applicantQuestions.clickNext()

      let error = await page.$(`${NAME_FIRST}-error`)
      expect(await error?.isHidden()).toEqual(false)
      error = await page.$(`${NAME_LAST}-error`)
      expect(await error?.isHidden()).toEqual(false)
    })
  })

  describe('multiple name questions', () => {
    const programName = 'Test program for multiple names'

    beforeAll(async () => {
      const {page, adminQuestions, adminPrograms} = ctx
      await loginAsAdmin(page)

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

      await logout(page)
    })

    it('with valid name does submit', async () => {
      const {applicantQuestions} = ctx
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerNameQuestion('Tommy', 'Pickles', '', 0)
      await applicantQuestions.answerNameQuestion('Chuckie', 'Finster', '', 1)
      await applicantQuestions.clickNext()

      await applicantQuestions.submitFromReviewPage()
    })

    it('with first invalid does not submit', async () => {
      const {page, applicantQuestions} = ctx
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerNameQuestion('', '', '', 0)
      await applicantQuestions.answerNameQuestion('Chuckie', 'Finster', '', 1)
      await applicantQuestions.clickNext()

      // First question has errors.
      let error = await page.$(`${NAME_FIRST}-error >> nth=0`)
      expect(await error?.isHidden()).toEqual(false)
      error = await page.$(`${NAME_LAST}-error >> nth=0`)
      expect(await error?.isHidden()).toEqual(false)

      // Second question has no errors.
      error = await page.$(`${NAME_FIRST}-error >> nth=1`)
      expect(await error?.isHidden()).toEqual(true)
      error = await page.$(`${NAME_LAST}-error >> nth=1`)
      expect(await error?.isHidden()).toEqual(true)
    })

    it('with second invalid does not submit', async () => {
      const {page, applicantQuestions} = ctx
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerNameQuestion('Tommy', 'Pickles', '', 0)
      await applicantQuestions.answerNameQuestion('', '', '', 1)
      await applicantQuestions.clickNext()

      // First question has no errors.
      let error = await page.$(`${NAME_FIRST}-error >> nth=0`)
      expect(await error?.isHidden()).toEqual(true)
      error = await page.$(`${NAME_LAST}-error >> nth=0`)
      expect(await error?.isHidden()).toEqual(true)

      // Second question has errors.
      error = await page.$(`${NAME_FIRST}-error >> nth=1`)
      expect(await error?.isHidden()).toEqual(false)
      error = await page.$(`${NAME_LAST}-error >> nth=1`)
      expect(await error?.isHidden()).toEqual(false)
    })

    it('has no accessiblity violations', async () => {
      const {page, applicantQuestions} = ctx
      await applicantQuestions.applyProgram(programName)

      await validateAccessibility(page)
    })
  })

  // One optional name followed by one required name.
  describe('optional name question', () => {
    const programName = 'Test program for optional name'

    beforeAll(async () => {
      const {page, adminQuestions, adminPrograms} = ctx
      await loginAsAdmin(page)

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
      await adminPrograms.publishAllPrograms()

      await logout(page)
    })

    it('with valid required name does submit', async () => {
      const {applicantQuestions} = ctx
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerNameQuestion('Tommy', 'Pickles', '', 1)
      await applicantQuestions.clickNext()

      await applicantQuestions.submitFromReviewPage()
    })

    it('with invalid optional name does not submit', async () => {
      const {page, applicantQuestions} = ctx
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerNameQuestion('Tommy', '', '', 0)
      await applicantQuestions.answerNameQuestion('Tommy', 'Pickles', '', 1)
      await applicantQuestions.clickNext()

      // Optional question has an error.
      const error = await page.$(`${NAME_LAST}-error >> nth=0`)
      expect(await error?.isHidden()).toEqual(false)
    })

    describe('with invalid required name', () => {
      beforeEach(async () => {
        const {applicantQuestions} = ctx
        await applicantQuestions.applyProgram(programName)
        await applicantQuestions.answerNameQuestion('', '', '', 1)
        await applicantQuestions.clickNext()
      })

      it('does not submit', async () => {
        const {page} = ctx
        // Second question has errors.
        let error = await page.$(`${NAME_FIRST}-error >> nth=1`)
        expect(await error?.isHidden()).toEqual(false)
        error = await page.$(`${NAME_LAST}-error >> nth=1`)
        expect(await error?.isHidden()).toEqual(false)
      })

      it('optional has no errors', async () => {
        const {page} = ctx
        // First question has no errors.
        let error = await page.$(`${NAME_FIRST}-error >> nth=0`)
        expect(await error?.isHidden()).toEqual(true)
        error = await page.$(`${NAME_LAST}-error >> nth=0`)
        expect(await error?.isHidden()).toEqual(true)
      })
    })
  })
})
