import {test, expect} from '@playwright/test'
import {
  createTestContext,
  enableFeatureFlag,
  loginAsAdmin,
  logout,
  validateAccessibility,
  validateScreenshot,
} from '../../support'

test.describe('address applicant flow', () => {
  const ctx = createTestContext(/* clearDb= */ false)

  test.describe('single required address question', () => {
    const programName = 'Test program for single address'

    test.beforeAll(async () => {
      await setUpProgramWithSingleAddressQuestion(programName)
    })

    test('validate screenshot', async () => {
      const {page, applicantQuestions} = ctx
      await applicantQuestions.applyProgram(programName)

      await validateScreenshot(page, 'address')
    })

    test('validate screenshot with errors', async () => {
      const {page, applicantQuestions} = ctx
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.clickNext()

      await validateScreenshot(page, 'address-errors')
    })

    test('does not show errors initially', async () => {
      const {page, applicantQuestions} = ctx
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

    test('with valid address does submit', async () => {
      const {applicantQuestions} = ctx
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

    test('with empty address does not submit', async () => {
      const {page, applicantQuestions} = ctx
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

    test('with invalid address does not submit', async () => {
      const {page, applicantQuestions} = ctx
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

    test.beforeAll(async () => {
      const {page, adminPrograms, adminQuestions} = ctx
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

    test('with valid addresses does submit', async () => {
      const {applicantQuestions} = ctx
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

    test('with first invalid does not submit', async () => {
      const {page, applicantQuestions} = ctx
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

    test('with second invalid does not submit', async () => {
      const {page, applicantQuestions} = ctx
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

    test('has no accessibility violations', async () => {
      const {page, applicantQuestions} = ctx
      await applicantQuestions.applyProgram(programName)

      await validateAccessibility(page)
    })
  })

  // One optional address followed by one required address.
  test.describe('optional address question', () => {
    const programName = 'Test program for optional address'

    test.beforeAll(async () => {
      const {page, adminPrograms, adminQuestions} = ctx
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

    test('with valid required address does submit', async () => {
      const {applicantQuestions} = ctx
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

    test('with invalid optional address does not submit', async () => {
      const {page, applicantQuestions} = ctx
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
      test.beforeEach(async () => {
        const {applicantQuestions} = ctx
        await applicantQuestions.applyProgram(programName)
        await applicantQuestions.answerAddressQuestion('', '', '', '', '', 1)
        await applicantQuestions.clickNext()
      })

      test('does not submit', async () => {
        const {page} = ctx
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

      test('optional has no errors', async () => {
        const {page} = ctx
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

  test.describe('single required address question with north star flag enabled', () => {
    const programName = 'Test program for single address'

    test.beforeAll(async () => {
      await setUpProgramWithSingleAddressQuestion(programName)
    })

    test.beforeEach(async () => {
      const {page} = ctx
      await enableFeatureFlag(page, 'north_star_applicant_ui')
    })

    test('validate screenshot', {tag: ['@northstar']}, async () => {
      const {page, applicantQuestions} = ctx
      await applicantQuestions.applyProgram(programName)

      await test.step('Screenshot without errors', async () => {
        await validateScreenshot(
          page,
          'address-north-star',
          /* fullPage= */ true,
          /* mobileScreenshot= */ true,
        )
      })

      await test.step('Screenshot with errors', async () => {
        await applicantQuestions.clickContinue()
        await validateScreenshot(
          page,
          'address-errors-north-star',
          /* fullPage= */ true,
          /* mobileScreenshot= */ true,
        )
      })
    })
  })

  async function setUpProgramWithSingleAddressQuestion(programName: string) {
    const {page, adminQuestions, adminPrograms} = ctx
    await loginAsAdmin(page)

    await adminQuestions.addAddressQuestion({
      questionName: 'address-test-q',
    })
    await adminPrograms.addAndPublishProgramWithQuestions(
      ['address-test-q'],
      programName,
    )

    await logout(page)
  }
})
