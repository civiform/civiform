import {test, expect} from '../../fixtures/custom_fixture'
import {
  loginAsAdmin,
  logout,
  validateAccessibility,
  validateScreenshot,
} from '../../support'

const NAME_FIRST = '.cf-name-first'
const NAME_LAST = '.cf-name-last'

test.describe('name applicant flow', {tag: ['@migrated']}, () => {
  test.describe('single required name question', () => {
    const programName = 'Test program for single name'

    test.beforeEach(async ({page, adminQuestions, adminPrograms}) => {
      // beforeAll
      await loginAsAdmin(page)

      await adminQuestions.addNameQuestion({
        questionName: 'name-test-q',
      })
      await adminPrograms.addAndPublishProgramWithQuestions(
        ['name-test-q'],
        programName,
      )
      await logout(page)

      // beforeEach
    })

    test('validate screenshot', async ({page, applicantQuestions}) => {
      await applicantQuestions.applyProgram(programName)

      await validateScreenshot(page, 'name')
    })

    test('validate screenshot with errors', async ({
      page,
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.clickNext()

      await validateScreenshot(page, 'name-errors')
    })

    test('does not show errors initially', async ({
      page,
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerNameQuestion('', '', '')
      let error = await page.$(`${NAME_FIRST}-error`)
      expect(await error?.isHidden()).toEqual(true)
      error = await page.$(`${NAME_LAST}-error`)
      expect(await error?.isHidden()).toEqual(true)
    })

    test('with valid name does submit', async ({applicantQuestions}) => {
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerNameQuestion('Tommy', 'Pickles', '')
      await applicantQuestions.clickNext()

      await applicantQuestions.submitFromReviewPage()
    })

    test('with empty name does not submit', async ({
      page,
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerNameQuestion('', '', '')
      await applicantQuestions.clickNext()

      let error = await page.$(`${NAME_FIRST}-error`)
      expect(await error?.isHidden()).toEqual(false)
      error = await page.$(`${NAME_LAST}-error`)
      expect(await error?.isHidden()).toEqual(false)
    })
  })

  test.describe('multiple name questions', () => {
    const programName = 'Test program for multiple names'

    test.beforeEach(async ({page, adminQuestions, adminPrograms}) => {
      // beforeAll
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

      // beforeEach
    })

    test('with valid name does submit', async ({applicantQuestions}) => {
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerNameQuestion('Tommy', 'Pickles', '', 0)
      await applicantQuestions.answerNameQuestion('Chuckie', 'Finster', '', 1)
      await applicantQuestions.clickNext()

      await applicantQuestions.submitFromReviewPage()
    })

    test('with first invalid does not submit', async ({
      page,
      applicantQuestions,
    }) => {
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

    test('with second invalid does not submit', async ({
      page,
      applicantQuestions,
    }) => {
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

    test('has no accessiblity violations', async ({
      page,
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(programName)

      await validateAccessibility(page)
    })
  })

  // One optional name followed by one required name.
  test.describe('optional name question', () => {
    const programName = 'Test program for optional name'

    test.beforeEach(async ({page, adminQuestions, adminPrograms}) => {
      // beforeAll
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
      await adminPrograms.publishAllDrafts()

      await logout(page)

      // beforeEach
    })

    test('with valid required name does submit', async ({
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerNameQuestion('Tommy', 'Pickles', '', 1)
      await applicantQuestions.clickNext()

      await applicantQuestions.submitFromReviewPage()
    })

    test('with invalid optional name does not submit', async ({
      page,
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerNameQuestion('Tommy', '', '', 0)
      await applicantQuestions.answerNameQuestion('Tommy', 'Pickles', '', 1)
      await applicantQuestions.clickNext()

      // Optional question has an error.
      const error = await page.$(`${NAME_LAST}-error >> nth=0`)
      expect(await error?.isHidden()).toEqual(false)
    })

    test.describe('with invalid required name', () => {
      test.beforeEach(async ({applicantQuestions}) => {
        await applicantQuestions.applyProgram(programName)
        await applicantQuestions.answerNameQuestion('', '', '', 1)
        await applicantQuestions.clickNext()
      })

      test('does not submit', async ({page}) => {
        // Second question has errors.
        let error = await page.$(`${NAME_FIRST}-error >> nth=1`)
        expect(await error?.isHidden()).toEqual(false)
        error = await page.$(`${NAME_LAST}-error >> nth=1`)
        expect(await error?.isHidden()).toEqual(false)
      })

      test('optional has no errors', async ({page}) => {
        // First question has no errors.
        let error = await page.$(`${NAME_FIRST}-error >> nth=0`)
        expect(await error?.isHidden()).toEqual(true)
        error = await page.$(`${NAME_LAST}-error >> nth=0`)
        expect(await error?.isHidden()).toEqual(true)
      })
    })
  })
})
