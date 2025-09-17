import {Page} from '@playwright/test'
import {test, expect} from '../../support/civiform_fixtures'
import {
  AdminPrograms,
  AdminQuestions,
  enableFeatureFlag,
  loginAsAdmin,
  logout,
  validateAccessibility,
  validateScreenshot,
} from '../../support'

test.describe('address applicant flow', {tag: ['@northstar']}, () => {
  test.beforeEach(async ({page}) => {
    await enableFeatureFlag(page, 'north_star_applicant_ui')
  })

  test.describe('single required address question', () => {
    const programName = 'Test program for single address'

    test.beforeEach(async ({page, adminQuestions, adminPrograms}) => {
      await setUpProgramWithSingleAddressQuestion(
        page,
        adminQuestions,
        adminPrograms,
        programName,
      )
    })

    test('with valid address does submit', async ({
      page,
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(
        programName,
        /* northStarEnabled= */ true,
      )

      await test.step('Validate page', async () => {
        await expectQuestionHasNoErrors(page, 0)

        await validateScreenshot(
          page.getByTestId('questionRoot'),
          'address-north-star',
          /* fullPage= */ false,
          /* mobileScreenshot= */ false,
        )
        await validateAccessibility(page)
      })

      await test.step('Verify user can submit', async () => {
        await applicantQuestions.answerAddressQuestion(
          '1234 St',
          'Unit B',
          'Sim',
          'WA',
          '54321',
        )

        await applicantQuestions.clickContinue()
        await applicantQuestions.submitFromReviewPage(
          /* northStarEnabled= */ true,
        )
        await applicantQuestions.expectConfirmationPage(
          /* northStarEnabled= */ true,
        )
      })
    })

    test('with empty address does not submit', async ({
      page,
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(
        programName,
        /* northStarEnabled= */ true,
      )
      // Answers intentionally left blank
      await applicantQuestions.clickContinue()

      await test.step('Validate error state', async () => {
        await expectQuestionHasGroupErrors(page, 0)

        await validateScreenshot(
          page.getByTestId('questionRoot'),
          'address-errors-north-star',
          /* fullPage= */ false,
          /* mobileScreenshot= */ false,
        )

        await validateAccessibility(page)
      })
    })

    test('with invalid address does not submit', async ({
      page,
      applicantQuestions,
    }) => {
      await test.step('Fill out form', async () => {
        await applicantQuestions.applyProgram(
          programName,
          /* northStarEnabled= */ true,
        )
        await applicantQuestions.answerAddressQuestion(
          '1234 St',
          'Unit B',
          'Sim',
          'WA',
          'notazipcode',
        )
        await applicantQuestions.clickContinue()
      })

      await test.step('Confirm aria-invalid applies only to the invalid field', async () => {
        const addressStreet1 = page.getByRole('textbox', {name: 'Address'})
        const addressStreet2 = page.getByRole('textbox', {
          name: 'Apartment, suite, etc. (',
        })
        const addressCity = page.getByRole('textbox', {name: 'City'})
        const addressState = page.getByLabel('State *')
        const addressZip = page.getByRole('textbox', {name: 'ZIP Code'})
        await expect(addressStreet1).toHaveAttribute('aria-invalid', 'false')
        await expect(addressStreet2).toHaveAttribute('aria-invalid', 'false')
        await expect(addressCity).toHaveAttribute('aria-invalid', 'false')
        await expect(addressState).toHaveAttribute('aria-invalid', 'false')
        await expect(addressZip).toHaveAttribute('aria-invalid', 'true')
      })

      await test.step('Confirm address zip error is visible', async () => {
        const error = page.locator('.cf-address-zip-error')
        await expect(error).toBeVisible()
        await validateAccessibility(page)
      })
    })

    test('with partially complete address does not submit', async ({
      page,
      applicantQuestions,
    }) => {
      await test.step('Partially fill out form', async () => {
        await applicantQuestions.applyProgram(
          programName,
          /* northStarEnabled= */ true,
        )
        await applicantQuestions.answerAddressQuestion('', '', '', '', '43568')
        await applicantQuestions.clickContinue()
      })

      await expectQuestionHasFieldErrorsForAllExceptZip(page, 0)
      await validateAccessibility(page)
    })
  })

  test.describe('multiple address questions', () => {
    const programName = 'Test program for multiple addresses'

    test.beforeEach(async ({page, adminPrograms, adminQuestions}) => {
      await loginAsAdmin(page)

      await adminQuestions.addAddressQuestion({
        questionName: 'address-test-a-q',
        description: '',
        questionText: 'Question A',
      })
      await adminQuestions.addAddressQuestion({
        questionName: 'address-test-b-q',
        description: '',
        questionText: 'Question B',
      })
      await adminPrograms.addAndPublishProgramWithQuestions(
        ['address-test-a-q', 'address-test-b-q'],
        programName,
      )

      await logout(page)
    })

    test('with valid addresses does submit', async ({applicantQuestions}) => {
      await test.step('Fill out form', async () => {
        await applicantQuestions.applyProgram(
          programName,
          /* northStarEnabled= */ true,
        )
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
      })

      await test.step('Verify user can submit', async () => {
        await applicantQuestions.clickContinue()

        await applicantQuestions.submitFromReviewPage(
          /* northStarEnabled= */ true,
        )
        await applicantQuestions.expectConfirmationPage(
          /* northStarEnabled= */ true,
        )
      })
    })

    test('with first invalid does not submit', async ({
      page,
      applicantQuestions,
    }) => {
      await test.step('Fill out form', async () => {
        await applicantQuestions.applyProgram(
          programName,
          /* northStarEnabled= */ true,
        )
        await applicantQuestions.answerAddressQuestion('', '', '', '', '', 0)
        await applicantQuestions.answerAddressQuestion(
          '1234 St',
          'Unit B',
          'Sim',
          'WA',
          '54321',
          1,
        )
        await applicantQuestions.clickContinue()
      })

      // Expect first question has errors since it's not answered
      await expectQuestionHasGroupErrors(page, 0)

      // Expect second question does NOT have errors since it has a valid answer
      await expectQuestionHasNoErrors(page, 1)
    })

    test('with second invalid does not submit', async ({
      page,
      applicantQuestions,
    }) => {
      await test.step('Fill out form', async () => {
        await applicantQuestions.applyProgram(
          programName,
          /* northStarEnabled= */ true,
        )
        await applicantQuestions.answerAddressQuestion(
          '1234 St',
          'Unit B',
          'Sim',
          'WA',
          '54321',
          0,
        )
        await applicantQuestions.answerAddressQuestion('', '', '', '', '', 1)
        await applicantQuestions.clickContinue()
      })

      // Expect the application did NOT advance to the summary page
      await expect(page.getByText('1 of 2')).toBeVisible() // Page 1 of 2
      await expect(page.getByText('Screen 1')).toBeVisible()
      await expect(page.getByText('Question A')).toBeVisible()
      await expect(
        page.locator('[data-testid="questionRoot"]').nth(0),
      ).toBeVisible()
      await expect(page.getByText('Question B')).toBeVisible()
      await expect(
        page.locator('[data-testid="questionRoot"]').nth(1),
      ).toBeVisible()

      // First question has no errors because it has a valid answer
      await expectQuestionHasNoErrors(page, 0)

      // Second question has errors because it is not answered
      await expectQuestionHasGroupErrors(page, 1)
    })

    test('has no accessibility violations', async ({
      page,
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(
        programName,
        /* northStarEnabled= */ true,
      )

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
        questionText: 'Optional Question',
      })
      await adminQuestions.addAddressQuestion({
        questionName: 'address-test-required-q',
        questionText: 'Required Question',
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

    test('with valid required address does submit', async ({
      applicantQuestions,
    }) => {
      await test.step('Fill out form', async () => {
        await applicantQuestions.applyProgram(
          programName,
          /* northStarEnabled= */ true,
        )
        await applicantQuestions.answerAddressQuestion(
          '1234 St',
          'Unit B',
          'Sim',
          'WA',
          '54321',
          1,
        )
      })

      await test.step('Verify user can submit', async () => {
        await applicantQuestions.clickContinue()

        await applicantQuestions.submitFromReviewPage(
          /* northStarEnabled= */ true,
        )
        await applicantQuestions.expectConfirmationPage(
          /* northStarEnabled= */ true,
        )
      })
    })

    test('with invalid optional address does not submit', async ({
      page,
      applicantQuestions,
    }) => {
      await test.step('Fill out form', async () => {
        await applicantQuestions.applyProgram(
          programName,
          /* northStarEnabled= */ true,
        )
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
        await applicantQuestions.clickContinue()
      })

      // Expect the first question has errors except on line 1
      const questionRoot = page.locator('[data-testid="questionRoot"]').first()
      await expect(
        questionRoot.getByText(
          'Error: Please enter valid street name and number.',
        ),
      ).toBeHidden()
      await expect(
        questionRoot.getByText('Error: Please enter city.'),
      ).toBeVisible()
      await expect(
        questionRoot.getByText('Error: Please enter state.'),
      ).toBeVisible()
      await expect(
        questionRoot.getByText('Error: Please enter valid 5-digit ZIP code.'),
      ).toBeVisible()
    })

    test('invalid required address', async ({page, applicantQuestions}) => {
      await applicantQuestions.applyProgram(
        programName,
        /* northStarEnabled= */ true,
      )

      // Intentionally do not answer any questions
      await applicantQuestions.clickContinue()

      // First (optional) question has no errors
      await expectQuestionHasNoErrors(page, 0)

      // Second (required) question has errors since it's not answered
      await expectQuestionHasGroupErrors(page, 1)
    })
  })

  async function setUpProgramWithSingleAddressQuestion(
    page: Page,
    adminQuestions: AdminQuestions,
    adminPrograms: AdminPrograms,
    programName: string,
  ) {
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

  // index: The index of the question of this type on the page. For example, on a page with 2 address
  // questions, the top question is index 0. The second question is index 1.
  async function expectQuestionHasGroupErrors(page: Page, index = 0) {
    const questionRoot = page.locator('[data-testid="questionRoot"]').nth(index)
    await expect(
      questionRoot.getByText('Error: This question is required.'),
    ).toBeVisible()

    // When all fields are missing, individual field errors should not be shown.
    await expect(
      questionRoot.getByText(
        'Error: Please enter valid street name and number.',
      ),
    ).toBeHidden()
    await expect(
      questionRoot.getByText('Error: Please enter city.'),
    ).toBeHidden()
    await expect(
      questionRoot.getByText('Error: Please enter state.'),
    ).toBeHidden()
    await expect(
      questionRoot.getByText('Error: Please enter valid 5-digit ZIP code.'),
    ).toBeHidden()
  }

  async function expectQuestionHasFieldErrorsForAllExceptZip(
    page: Page,
    index = 0,
  ) {
    const questionRoot = page.locator('[data-testid="questionRoot"]').nth(index)
    // When only some fields are missing, individual field errors should not be shown.
    await expect(
      questionRoot.getByText(
        'Error: Please enter valid street name and number.',
      ),
    ).toBeVisible()
    await expect(
      questionRoot.getByText('Error: Please enter city.'),
    ).toBeVisible()
    await expect(
      questionRoot.getByText('Error: Please enter state.'),
    ).toBeVisible()
    // There is a red border on the left because of cf-question-field-with-error
    await expect(questionRoot.locator('.cf-address-street-1')).toHaveClass(
      'cf-address-street-1 cf-applicant-question-field cf-question-field-with-error',
    )
    // The input box has a red border because of usa-input--error
    await expect(
      questionRoot.locator('.cf-address-street-1 input'),
    ).toHaveClass('usa-input cf-input-large usa-input--error')
  }

  // index: The index of the question of this type on the page. For example, on a page with 2 address
  // questions, the top question is index 0. The second question is index 1.
  async function expectQuestionHasNoErrors(page: Page, index = 0) {
    const questionRoot = page.locator('[data-testid="questionRoot"]').nth(index)
    await expect(
      questionRoot.getByText(
        'Error: Please enter valid street name and number.',
      ),
    ).toBeHidden()
    await expect(
      questionRoot.getByText('Error: Please enter city.'),
    ).toBeHidden()
    await expect(
      questionRoot.getByText('Error: Please enter state.'),
    ).toBeHidden()
    await expect(
      questionRoot.getByText('Error: Please enter valid 5-digit ZIP code.'),
    ).toBeHidden()
  }
})
