import {test, expect} from '../support/civiform_fixtures'
import {disableFeatureFlag, enableFeatureFlag, loginAsAdmin} from '../support'
import {Locator} from '@playwright/test'

const QUESTION_TYPES_EXCEPT_ENUMERATOR = [
  'Address',
  'Checkbox',
  'Currency',
  'Date',
  'Dropdown',
  'Email',
  'File Upload',
  'ID',
  'Name',
  'Number',
  'Phone Number',
  'Radio Button',
  'Static Text',
  'Text',
  'Yes/No',
]

const expectAllQuestionTypesExceptEnumerator = async (
  dropdownLocator: Locator,
) => {
  for (const questionType of QUESTION_TYPES_EXCEPT_ENUMERATOR) {
    await expect(
      dropdownLocator.getByText(questionType, {exact: true}),
    ).toBeVisible()
  }
}

test.describe('Admin question list on questions page with enumerator improvements disabled', () => {
  test.beforeEach(async ({page}) => {
    await disableFeatureFlag(page, 'enumerator_improvements_enabled')
  })

  test('displays list of available question types', async ({
    page,
    adminQuestions,
  }) => {
    await loginAsAdmin(page)
    await adminQuestions.gotoAdminQuestionsPage()

    await page.click('#create-question-button')
    const dropdownLocator = page.getByTestId('create-question-button-dropdown')

    await expectAllQuestionTypesExceptEnumerator(dropdownLocator)
    await expect(
      dropdownLocator.getByText('Enumerator', {exact: true}),
    ).toBeVisible()
  })
})

test.describe('Admin question list on questions page with enumerator improvements enabled', () => {
  test.beforeEach(async ({page}) => {
    await enableFeatureFlag(page, 'enumerator_improvements_enabled')
  })

  test('displays list of available question types', async ({
    page,
    adminQuestions,
  }) => {
    await loginAsAdmin(page)
    await adminQuestions.gotoAdminQuestionsPage()

    await page.click('#create-question-button')
    const dropdownLocator = page.getByTestId('create-question-button-dropdown')

    await expectAllQuestionTypesExceptEnumerator(dropdownLocator)
    await expect(
      dropdownLocator.getByText('Enumerator', {exact: true}),
    ).toBeVisible()
  })
})

test.describe('Admin question list on programs page with enumerator improvements disabled', () => {
  test.beforeEach(async ({page}) => {
    await disableFeatureFlag(page, 'enumerator_improvements_enabled')
  })

  test('displays list of available question types', async ({
    page,
    adminPrograms,
  }) => {
    await loginAsAdmin(page)
    const programName = 'Test program'
    await adminPrograms.addProgram(programName)
    await adminPrograms.editProgramBlock(programName)
    await page.getByRole('button', {name: 'Add a question'}).click()
    await page.click('#create-question-button')
    const dropdownLocator = page.getByTestId('create-question-button-dropdown')

    await expectAllQuestionTypesExceptEnumerator(dropdownLocator)
    await expect(
      dropdownLocator.getByText('Enumerator', {exact: true}),
    ).toBeVisible()
  })

  test('displays list of available question types with non-empty block', async ({
    page,
    adminPrograms,
    adminQuestions,
  }) => {
    await loginAsAdmin(page)
    // Create a question to add to the block
    await adminQuestions.addTextQuestion({questionName: 'text-question'})

    const programName = 'Test program'
    await adminPrograms.addProgram(programName)
    await adminPrograms.editProgramBlock(programName)
    // Add existing question to make block non-empty
    await adminPrograms.addQuestionFromQuestionBank('text-question')
    await page.getByRole('button', {name: 'Add a question'}).click()
    await page.click('#create-question-button')
    const dropdownLocator = page.getByTestId('create-question-button-dropdown')

    await expectAllQuestionTypesExceptEnumerator(dropdownLocator)
    await expect(
      dropdownLocator.getByText('Enumerator', {exact: true}),
    ).not.toBeAttached()
  })
})

test.describe('Admin question list on programs page with enumerator improvements enabled', () => {
  test.beforeEach(async ({page}) => {
    await enableFeatureFlag(page, 'enumerator_improvements_enabled')
  })

  test('displays list of available question types', async ({
    page,
    adminPrograms,
  }) => {
    await loginAsAdmin(page)
    const programName = 'Test program'
    await adminPrograms.addProgram(programName)
    await adminPrograms.editProgramBlock(programName)
    await page.getByRole('button', {name: 'Add a question'}).click()
    await page.click('#create-question-button')
    const dropdownLocator = page.getByTestId('create-question-button-dropdown')

    await expectAllQuestionTypesExceptEnumerator(dropdownLocator)
    await expect(
      dropdownLocator.getByText('Enumerator', {exact: true}),
    ).not.toBeAttached()
  })

  test('displays list of available question types with non-empty block', async ({
    page,
    adminPrograms,
    adminQuestions,
  }) => {
    await loginAsAdmin(page)
    // Create a question to add to the block
    await adminQuestions.addTextQuestion({questionName: 'text-question'})

    const programName = 'Test program'
    await adminPrograms.addProgram(programName)
    await adminPrograms.editProgramBlock(programName)
    // Add existing question to make block non-empty
    await adminPrograms.addQuestionFromQuestionBank('text-question')
    await page.getByRole('button', {name: 'Add a question'}).click()
    await page.click('#create-question-button')
    const dropdownLocator = page.getByTestId('create-question-button-dropdown')

    await expectAllQuestionTypesExceptEnumerator(dropdownLocator)
    await expect(
      dropdownLocator.getByText('Enumerator', {exact: true}),
    ).not.toBeAttached()
  })
})
