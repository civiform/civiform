import {test, expect} from '../../fixtures/custom_fixture'
import {
  AdminPrograms,
  AdminQuestions,
  ApplicantQuestions,
  loginAsAdmin,
  logout,
  validateAccessibility,
  validateScreenshot,
} from '../../support'

test.describe('address applicant flow', {tag: ['@migrated']}, () => {
  test.describe('single required address question', () => {
    const programName = 'Test program for single address'

    test.beforeEach(async ({page, helpers}) => {
      const adminQuestions = new AdminQuestions(page)
      const adminPrograms = new AdminPrograms(page)

      await helpers.loginAsAdmin()

      await adminQuestions.addAddressQuestion({
        questionName: 'address-test-q',
      })
      await adminPrograms.addAndPublishProgramWithQuestions(
        ['address-test-q'],
        programName,
      )

      await helpers.logout()
    })

    test('validate screenshot', async ({page}) => {
      const applicantQuestions = new ApplicantQuestions(page)
      await applicantQuestions.applyProgram(programName)

      await validateScreenshot(page, 'address')
    })

    test('validate screenshot with errors', async ({page, applicantQuestions} ) => {
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.clickNext()

      await validateScreenshot(page, 'address-errors')
    })

    test('does not show errors initially', async ({page, applicantQuestions} ) => {
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerAddressQuestion(
        '1234 St',
        'Unit B',
        'Sim',
        'WA',
        '54321',
      )
      let error = page.locator('.cf-address-street-1-error')
      await expect(error).toBeHidden()
      error = page.locator('.cf-address-city-error')
      await expect(error).toBeHidden()
      error = page.locator('.cf-address-state-error')
      await expect(error).toBeHidden()
      error = page.locator('.cf-address-zip-error')
      await expect(error).toBeHidden()
    })

    test('with valid address does submit', async ({applicantQuestions}) => {
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerAddressQuestion(
        '1234 St',
        'Unit B',
        'Sim',
        'WA',
        '54321',
      )
      await applicantQuestions.clickNext()

      await applicantQuestions.submitFromReviewPage()
    })

    test('with empty address does not submit', async ({page, applicantQuestions}) => {
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerAddressQuestion('', '', '', '', '')
      await applicantQuestions.clickNext()

      let error = page.locator('.cf-address-street-1-error')
      await expect(error).toBeVisible()
      error = page.locator('.cf-address-city-error')
      await expect(error).toBeVisible()
      error = page.locator('.cf-address-state-error')
      await expect(error).toBeVisible()
      error = page.locator('.cf-address-zip-error')
      await expect(error).toBeVisible()
    })

    test('with invalid address does not submit', async ({page, applicantQuestions}) => {
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerAddressQuestion(
        '1234 St',
        'Unit B',
        'Sim',
        'WA',
        'notazipcode',
      )
      await applicantQuestions.clickNext()

      const error = page.locator('.cf-address-zip-error')
      await expect(error).toBeVisible()
    })
  })

  test.describe('multiple address questions', () => {
    const programName = 'Test program for multiple addresses'

    test.beforeEach(async ({page, adminPrograms, adminQuestions}) => {
      await loginAsAdmin(page)

      await adminQuestions.addAddressQuestion({
        questionName: 'address-test-a-q',
      })
      await adminQuestions.addAddressQuestion({
        questionName: 'address-test-b-q',
      })
      await adminPrograms.addAndPublishProgramWithQuestions(
        ['address-test-a-q', 'address-test-b-q'],
        programName,
      )

      await logout(page)
    })

    test('with valid addresses does submit', async ({applicantQuestions}) => {
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerAddressQuestion(
        '1234 St',
        'Unit B',
        'Sim',
        'WA',
        '54321',
        0,
      )
      await applicantQuestions.answerAddressQuestion(
        '1234 St',
        'Unit B',
        'Sim',
        'WA',
        '54321',
        1,
      )
      await applicantQuestions.clickNext()

      await applicantQuestions.submitFromReviewPage()
    })

    test('with first invalid does not submit', async ({page, applicantQuestions}) => {
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerAddressQuestion('', '', '', '', '', 0)
      await applicantQuestions.answerAddressQuestion(
        '1234 St',
        'Unit B',
        'Sim',
        'WA',
        '54321',
        1,
      )
      await applicantQuestions.clickNext()

      // First question has errors.
      let error = page.locator('.cf-address-street-1-error >> nth=0')
      await expect(error).toBeVisible()
      error = page.locator('.cf-address-city-error >> nth=0')
      await expect(error).toBeVisible()
      error = page.locator('.cf-address-state-error >> nth=0')
      await expect(error).toBeVisible()
      error = page.locator('.cf-address-zip-error >> nth=0')
      await expect(error).toBeVisible()

      // Second question has no errors.
      error = page.locator('.cf-address-street-1-error >> nth=1')
      await expect(error).toBeHidden()
      error = page.locator('.cf-address-city-error >> nth=1')
      await expect(error).toBeHidden()
      error = page.locator('.cf-address-state-error >> nth=1')
      await expect(error).toBeHidden()
      error = page.locator('.cf-address-zip-error >> nth=1')
      await expect(error).toBeHidden()
    })

    test('with second invalid does not submit', async ({page, applicantQuestions}) => {
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerAddressQuestion(
        '1234 St',
        'Unit B',
        'Sim',
        'WA',
        '54321',
        0,
      )
      await applicantQuestions.answerAddressQuestion('', '', '', '', '', 1)
      await applicantQuestions.clickNext()

      // First question has no errors.
      let error = page.locator('.cf-address-street-1-error >> nth=0')
      await expect(error).toBeHidden()
      error = page.locator('.cf-address-city-error >> nth=0')
      await expect(error).toBeHidden()
      error = page.locator('.cf-address-state-error >> nth=0')
      await expect(error).toBeHidden()
      error = page.locator('.cf-address-zip-error >> nth=0')
      await expect(error).toBeHidden()

      // Second question has errors.
      error = page.locator('.cf-address-street-1-error >> nth=1')
      await expect(error).toBeVisible()
      error = page.locator('.cf-address-city-error >> nth=1')
      await expect(error).toBeVisible()
      error = page.locator('.cf-address-state-error >> nth=1')
      await expect(error).toBeVisible()
      error = page.locator('.cf-address-zip-error >> nth=1')
      await expect(error).toBeVisible()
    })

    test('has no accessibility violations', async ({page, applicantQuestions}) => {
      await applicantQuestions.applyProgram(programName)

      await validateAccessibility(page)
    })
  })

  // One optional address followed by one required address.
  test.describe('optional address question', () => {
    const programName = 'Test program for optional address'

    test.beforeEach(async ({page, adminPrograms, adminQuestions}) => {
      await loginAsAdmin(page)

      await adminQuestions.addAddressQuestion({
        questionName: 'address-test-optional-q',
      })
      await adminQuestions.addAddressQuestion({
        questionName: 'address-test-required-q',
      })
      await adminPrograms.addProgram(programName)
      await adminPrograms.editProgramBlockWithOptional(
        programName,
        'Optional question block',
        ['address-test-required-q'],
        'address-test-optional-q',
      )
      await adminPrograms.publishAllDrafts()

      await logout(page)
    })

    test('with valid required address does submit', async ({applicantQuestions}) => {
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerAddressQuestion(
        '1234 St',
        'Unit B',
        'Sim',
        'WA',
        '54321',
        1,
      )
      await applicantQuestions.clickNext()

      await applicantQuestions.submitFromReviewPage()
    })

    test('with invalid optional address does not submit', async ({page, applicantQuestions}) => {
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerAddressQuestion(
        '1234 St',
        '',
        '',
        '',
        '',
        0,
      )
      await applicantQuestions.answerAddressQuestion(
        '1234 St',
        'Unit B',
        'Sim',
        'WA',
        '54321',
        1,
      )
      await applicantQuestions.clickNext()

      // First question has errors.
      let error = page.locator('.cf-address-city-error >> nth=0')
      await expect(error).toBeVisible()
      error = page.locator('.cf-address-state-error >> nth=0')
      await expect(error).toBeVisible()
      error = page.locator('.cf-address-zip-error >> nth=0')
      await expect(error).toBeVisible()
    })

    test.describe('with invalid required address', () => {
      test.beforeEach(async ({applicantQuestions}) => {
        await applicantQuestions.applyProgram(programName)
        await applicantQuestions.answerAddressQuestion('', '', '', '', '', 1)
        await applicantQuestions.clickNext()
      })

      test('does not submit', async ({page}) => {
        // Second question has errors.
        let error = page.locator('.cf-address-street-1-error >> nth=1')
        await expect(error).toBeVisible()
        error = page.locator('.cf-address-city-error >> nth=1')
        await expect(error).toBeVisible()
        error = page.locator('.cf-address-state-error >> nth=1')
        await expect(error).toBeVisible()
        error = page.locator('.cf-address-zip-error >> nth=1')
        await expect(error).toBeVisible()
      })

      test('optional has no errors', async ({page}) => {
        // First question has no errors.
        let error = page.locator('.cf-address-street-1-error >> nth=0')
        await expect(error).toBeHidden()
        error = page.locator('.cf-address-city-error >> nth=0')
        await expect(error).toBeHidden()
        error = page.locator('.cf-address-state-error >> nth=0')
        await expect(error).toBeHidden()
        error = page.locator('.cf-address-zip-error >> nth=0')
        await expect(error).toBeHidden()
      })
    })
  })
})
