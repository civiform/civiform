import {Page} from '@playwright/test'
import {test, expect} from '../../support/civiform_fixtures'
import {
  AdminQuestions,
  AdminPrograms,
  disableFeatureFlag,
  enableFeatureFlag,
  loginAsAdmin,
  logout,
  validateAccessibility,
  validateScreenshot,
} from '../../support'

const NAME_FIRST = '.cf-name-first'
const NAME_LAST = '.cf-name-last'

test.describe('name applicant flow', () => {
  test.describe('single required name question', () => {
    const programName = 'Test program for single name'

    test.beforeEach(async ({page, adminQuestions, adminPrograms}) => {
      await disableFeatureFlag(page, 'show_not_production_banner_enabled')

      await setUpSingleRequiredQuestion(
        programName,
        page,
        adminQuestions,
        adminPrograms,
      )
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
      await expect(page.locator(`${NAME_FIRST}-error`)).toBeHidden()
      await expect(page.locator(`${NAME_LAST}-error`)).toBeHidden()
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

      await expect(page.locator(`${NAME_FIRST}-error`)).toBeVisible()
      await expect(page.locator(`${NAME_LAST}-error`)).toBeVisible()
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
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerNameQuestion('Tommy', 'Pickles', '', '', 0)
      await applicantQuestions.answerNameQuestion(
        'Chuckie',
        'Finster',
        '',
        '',
        1,
      )
      await applicantQuestions.clickNext()

      await applicantQuestions.submitFromReviewPage()
    })

    test('with first invalid does not submit', async ({
      page,
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerNameQuestion('', '', '', '', 0)
      await applicantQuestions.answerNameQuestion(
        'Chuckie',
        'Finster',
        '',
        '',
        1,
      )
      await applicantQuestions.clickNext()

      // First question has errors.
      await expect(page.locator(`${NAME_FIRST}-error`).nth(0)).toBeVisible()
      await expect(page.locator(`${NAME_LAST}-error`).nth(0)).toBeVisible()

      // Second question has no errors.
      await expect(page.locator(`${NAME_FIRST}-error`).nth(1)).toBeHidden()
      await expect(page.locator(`${NAME_LAST}-error`).nth(1)).toBeHidden()
    })

    test('with second invalid does not submit', async ({
      page,
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerNameQuestion('Tommy', 'Pickles', '', '', 0)
      await applicantQuestions.answerNameQuestion('', '', '', '', 1)
      await applicantQuestions.clickNext()

      // First question has no errors.
      await expect(page.locator(`${NAME_FIRST}-error`).nth(0)).toBeHidden()
      await expect(page.locator(`${NAME_LAST}-error`).nth(0)).toBeHidden()

      // Second question has errors.
      await expect(page.locator(`${NAME_FIRST}-error`).nth(1)).toBeVisible()
      await expect(page.locator(`${NAME_LAST}-error`).nth(1)).toBeVisible()
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
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerNameQuestion('Tommy', 'Pickles', '', '', 1)
      await applicantQuestions.clickNext()

      await applicantQuestions.submitFromReviewPage()
    })

    test('with invalid optional name does not submit', async ({
      page,
      applicantQuestions,
    }) => {
      await applicantQuestions.applyProgram(programName)
      await applicantQuestions.answerNameQuestion('Tommy', '', '', '', 0)
      await applicantQuestions.answerNameQuestion('Tommy', 'Pickles', '', '', 1)
      await applicantQuestions.clickNext()

      // Optional question has an error.
      await expect(page.locator(`${NAME_LAST}-error`).nth(0)).toBeVisible()
    })

    test.describe('with invalid required name', () => {
      test.beforeEach(async ({applicantQuestions}) => {
        await applicantQuestions.applyProgram(programName)
        await applicantQuestions.answerNameQuestion('', '', '', '', 1)
        await applicantQuestions.clickNext()
      })

      test('does not submit', async ({page}) => {
        // Second question has errors.
        await expect(page.locator(`${NAME_FIRST}-error`).nth(1)).toBeVisible()
        await expect(page.locator(`${NAME_LAST}-error`).nth(1)).toBeVisible()
      })

      test('optional has no errors', async ({page}) => {
        // First question has no errors.
        await expect(page.locator(`${NAME_FIRST}-error`).nth(0)).toBeHidden()
        await expect(page.locator(`${NAME_LAST}-error`).nth(0)).toBeHidden()
      })
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
        await applicantQuestions.applyProgram(programName)

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
        await applicantQuestions.applyProgram(programName)

        await test.step('anwers name question with suffix', async () => {
          await applicantQuestions.answerNameQuestion(
            'Lilly',
            'Singh',
            'Saini',
            'I',
          )
          await applicantQuestions.clickNext()

          await expect(page.getByText('Lilly Saini Singh I')).toBeVisible()
          await applicantQuestions.submitFromReviewPage()
        })
      })

      test('without suffix the application does submit as well', async ({
        page,
        applicantQuestions,
      }) => {
        await applicantQuestions.applyProgram(programName)

        await test.step('anwers name question with suffix', async () => {
          await applicantQuestions.answerNameQuestion('Ann', 'Gates', 'Quiroz')
          await applicantQuestions.clickNext()

          await expect(page.getByText('Ann Quiroz Gates')).toBeVisible()
          await applicantQuestions.submitFromReviewPage()
        })
      })
    },
  )

  test.describe(
    'single required name question with north star flag enabled',
    {tag: ['@northstar']},
    () => {
      const programName = 'Test program for single name'

      test.beforeEach(async ({page, adminQuestions, adminPrograms}) => {
        await setUpSingleRequiredQuestion(
          programName,
          page,
          adminQuestions,
          adminPrograms,
        )
        await enableFeatureFlag(page, 'north_star_applicant_ui')
      })

      test('validate screenshot with north star flag enabled', async ({
        page,
        applicantQuestions,
      }) => {
        await applicantQuestions.applyProgram(programName)

        await test.step('Screenshot without errors', async () => {
          await validateScreenshot(
            page.getByTestId('questionRoot'),
            'name-north-star',
            /* fullPage= */ false,
            /* mobileScreenshot= */ true,
          )
        })

        await test.step('Screenshot with errors', async () => {
          await applicantQuestions.clickContinue()
          await validateScreenshot(
            page.getByTestId('questionRoot'),
            'name-errors-north-star',
            /* fullPage= */ false,
            /* mobileScreenshot= */ true,
          )
        })
      })

      test('validate name suffix field with north star flag and name suffix dropdown flag enabled', async ({
        page,
        applicantQuestions,
      }) => {
        await enableFeatureFlag(page, 'name_suffix_dropdown_enabled')

        await test.step('name suffix field available to use', async () => {
          await applicantQuestions.applyProgram(programName)
          await expect(page.getByLabel('Suffix')).toBeVisible()
          await expect(page.getByLabel('Suffix')).toHaveValue('')
        })

        await test.step('selects an option in name suffix dropdown', async () => {
          await applicantQuestions.answerDropdownQuestion('III')
          await expect(page.getByLabel('Suffix')).toHaveValue('III')
        })
      })

      test('has no accessiblity violations', async ({
        page,
        applicantQuestions,
      }) => {
        await applicantQuestions.applyProgram(programName)

        await validateAccessibility(page)
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
})
