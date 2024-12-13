import {Page} from '@playwright/test'
import {test, expect} from '../../support/civiform_fixtures'
import {
  AdminQuestions,
  AdminPrograms,
  enableFeatureFlag,
  loginAsAdmin,
  logout,
  validateAccessibility,
  validateScreenshot,
} from '../../support'

test.describe('name applicant flow', {tag: ['@northstar']}, () => {
  test.beforeEach(async ({page}) => {
    await enableFeatureFlag(page, 'north_star_applicant_ui')
  })

  test.describe('single required name question', () => {
    const programName = 'Test program for single name'

    test.beforeEach(async ({page, adminQuestions, adminPrograms}) => {
      await setUpSingleRequiredQuestion(
        programName,
        page,
        adminQuestions,
        adminPrograms,
      )
    })

    test('validate screenshot with north star flag enabled', async ({
      page,
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(
        programName,
        /* northStarEnabled= */ true,
      )

      await test.step('Screenshot without errors', async () => {
        await validateScreenshot(
          page.getByTestId('questionRoot'),
          'name-north-star',
          /* fullPage= */ false,
          /* mobileScreenshot= */ false,
        )
      })

      await test.step('Screenshot with errors', async () => {
        await applicantQuestions.clickContinue()
        await validateScreenshot(
          page.getByTestId('questionRoot'),
          'name-errors-north-star',
          /* fullPage= */ false,
          /* mobileScreenshot= */ false,
        )
      })
    })

    test('has no accessiblity violations', async ({
      page,
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(
        programName,
        /* northStarEnabled= */ true,
      )

      await validateAccessibility(page)
    })

    test('does not show errors initially', async ({
      page,
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(
        programName,
        /* northStarEnabled= */ true,
      )
      await applicantQuestions.answerNameQuestion('', '', '')

      await expectQuestionHasNoErrors(page)
    })

    test('with valid name does submit', async ({applicantQuestions}) => {
      await applicantQuestions.applyProgram(
        programName,
        /* northStarEnabled= */ true,
      )
      await applicantQuestions.answerNameQuestion('Tommy', 'Pickles', '')
      await applicantQuestions.clickContinue()

      await applicantQuestions.submitFromReviewPage(
        /* northStarEnabled= */ true,
      )
    })

    test('with empty name does not submit', async ({
      page,
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(
        programName,
        /* northStarEnabled= */ true,
      )
      await applicantQuestions.answerNameQuestion('', '', '')
      await applicantQuestions.clickContinue()

      await expectQuestionHasErrors(page)
    })
  })

  test.describe('multiple name questions', () => {
    const programName = 'Test program for multiple names'

    test.beforeEach(async ({page, adminQuestions, adminPrograms}) => {
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

    test('with valid name does submit', async ({applicantQuestions}) => {
      await applicantQuestions.applyProgram(
        programName,
        /* northStarEnabled= */ true,
      )
      await applicantQuestions.answerNameQuestion('Tommy', 'Pickles', '', '', 0)
      await applicantQuestions.answerNameQuestion(
        'Chuckie',
        'Finster',
        '',
        '',
        1,
      )
      await applicantQuestions.clickContinue()

      await applicantQuestions.submitFromReviewPage(
        /* northStarEnabled= */ true,
      )
    })

    test('with first invalid does not submit', async ({
      page,
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(
        programName,
        /* northStarEnabled= */ true,
      )
      await applicantQuestions.answerNameQuestion('', '', '', '', 0)
      await applicantQuestions.answerNameQuestion(
        'Chuckie',
        'Finster',
        '',
        '',
        1,
      )
      await applicantQuestions.clickContinue()

      // First question has errors.
      await expectQuestionHasErrors(page, 0)

      // Second question has no errors.
      await expectQuestionHasNoErrors(page, 1)
    })

    test('with second invalid does not submit', async ({
      page,
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(
        programName,
        /* northStarEnabled= */ true,
      )
      await applicantQuestions.answerNameQuestion('Tommy', 'Pickles', '', '', 0)
      await applicantQuestions.answerNameQuestion('', '', '', '', 1)
      await applicantQuestions.clickContinue()

      // First question has no errors.
      await expectQuestionHasNoErrors(page, 0)

      // Second question has errors.
      await expectQuestionHasErrors(page, 1)
    })

    test('has no accessiblity violations', async ({
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

  // One optional name followed by one required name.
  test.describe('optional name question', () => {
    const programName = 'Test program for optional name'

    test.beforeEach(async ({page, adminQuestions, adminPrograms}) => {
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
    })

    test('with valid required name does submit', async ({
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(
        programName,
        /* northStarEnabled= */ true,
      )
      await applicantQuestions.answerNameQuestion('Tommy', 'Pickles', '', '', 1)
      await applicantQuestions.clickContinue()

      await applicantQuestions.submitFromReviewPage(
        /* northStarEnabled= */ true,
      )
    })

    test('with invalid optional name does not submit', async ({
      page,
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(
        programName,
        /* northStarEnabled= */ true,
      )
      await applicantQuestions.answerNameQuestion('Tommy', '', '', '', 0)
      await applicantQuestions.answerNameQuestion('Tommy', 'Pickles', '', '', 1)
      await applicantQuestions.clickContinue()

      // Optional question has an error.
      const questionRoot = page.locator('[data-testid="questionRoot"]').nth(0)
      await expect(
        questionRoot.getByText('Error: Please enter your last name.'),
      ).toBeVisible()
    })

    test('with invalid required name does not submit', async ({
      page,
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(
        programName,
        /* northStarEnabled= */ true,
      )
      await applicantQuestions.answerNameQuestion('', '', '', '', 1)
      await applicantQuestions.clickContinue()

      // First (optional) question has no errors.
      await expectQuestionHasNoErrors(page, 0)

      // Second (required) question has errors
      await expectQuestionHasErrors(page, 1)
    })
  })

  test.describe(
    'name question with name suffix flag enabled',
    {tag: ['@in-development']},
    () => {
      const programName = 'Test program for name suffix'

      test.beforeEach(async ({page, adminQuestions, adminPrograms}) => {
        await setUpSingleRequiredQuestion(
          programName,
          page,
          adminQuestions,
          adminPrograms,
        )
        await enableFeatureFlag(page, 'name_suffix_dropdown_enabled')
      })

      test('name questions with suffix field being available to use', async ({
        page,
        applicantQuestions,
      }) => {
        await applicantQuestions.applyProgram(
          programName,
          /* northStarEnabled= */ true,
        )

        await test.step('name question has suffix field available and no default value selected', async () => {
          await expect(page.getByLabel('Suffix')).toBeVisible()
          await expect(page.getByLabel('Suffix')).toHaveValue('')
        })

        await test.step('selects an option in name suffix dropdown', async () => {
          await applicantQuestions.answerDropdownQuestion('II')
          await expect(page.getByLabel('Suffix')).toBeVisible()
          await expect(page.getByLabel('Suffix')).toHaveValue('II')
        })
      })

      test('with suffix the application does submit', async ({
        page,
        applicantQuestions,
      }) => {
        await applicantQuestions.applyProgram(
          programName,
          /* northStarEnabled= */ true,
        )

        await test.step('anwers name question with suffix', async () => {
          await applicantQuestions.answerNameQuestion(
            'Lilly',
            'Singh',
            'Saini',
            'I',
          )
          await applicantQuestions.clickContinue()

          await expect(page.getByText('Lilly Saini Singh I')).toBeVisible()
          await applicantQuestions.submitFromReviewPage(
            /* northStarEnabled= */ true,
          )
        })
      })

      test('without suffix the application does submit as well', async ({
        page,
        applicantQuestions,
      }) => {
        await applicantQuestions.applyProgram(
          programName,
          /* northStarEnabled= */ true,
        )

        await test.step('anwers name question with suffix', async () => {
          await applicantQuestions.answerNameQuestion('Ann', 'Gates', 'Quiroz')
          await applicantQuestions.clickContinue()

          await expect(page.getByText('Ann Quiroz Gates')).toBeVisible()
          await applicantQuestions.submitFromReviewPage(
            /* northStarEnabled= */ true,
          )
        })
      })
    },
  )

  async function setUpSingleRequiredQuestion(
    programName: string,
    page: Page,
    adminQuestions: AdminQuestions,
    adminPrograms: AdminPrograms,
  ) {
    await loginAsAdmin(page)

    await adminQuestions.addNameQuestion({
      questionName: 'name-test-q',
    })
    await adminPrograms.addAndPublishProgramWithQuestions(
      ['name-test-q'],
      programName,
    )
    await logout(page)
  }

  async function expectQuestionHasErrors(page: Page, index = 0) {
    const questionRoot = page.locator('[data-testid="questionRoot"]').nth(index)
    await expect(
      questionRoot.getByText('Error: This question is required.'),
    ).toBeVisible()
    await expect(
      questionRoot.getByText('Error: Please enter your first name.'),
    ).toBeVisible()
    await expect(
      questionRoot.getByText('Error: Please enter your last name.'),
    ).toBeVisible()
  }

  async function expectQuestionHasNoErrors(page: Page, index = 0) {
    const questionRoot = page.locator('[data-testid="questionRoot"]').nth(index)
    await expect(
      questionRoot.getByText('Error: This question is required.'),
    ).toBeHidden()
    await expect(
      questionRoot.getByText('Error: Please enter your first name.'),
    ).toBeHidden()
    await expect(
      questionRoot.getByText('Error: Please enter your last name.'),
    ).toBeHidden()
  }
})
