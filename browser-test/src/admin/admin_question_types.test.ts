import {test, expect} from '../support/civiform_fixtures'
import {loginAsAdmin} from '../support'

test.describe('Admin question list on questions page', () => {
  test('displays list of available question types', async ({
    page,
    adminQuestions,
  }) => {
    await loginAsAdmin(page)
    await adminQuestions.gotoAdminQuestionsPage()
    await page.click('#create-question-button')
    const dropdownLocator = page.getByTestId('create-question-button-dropdown')

    await expect(
      dropdownLocator.getByText('Address', {exact: true}),
    ).toBeVisible()
    await expect(
      dropdownLocator.getByText('Checkbox', {exact: true}),
    ).toBeVisible()
    await expect(
      dropdownLocator.getByText('Currency', {exact: true}),
    ).toBeVisible()
    await expect(dropdownLocator.getByText('Date', {exact: true})).toBeVisible()
    await expect(
      dropdownLocator.getByText('Dropdown', {exact: true}),
    ).toBeVisible()
    await expect(
      dropdownLocator.getByText('Email', {exact: true}),
    ).toBeVisible()
    await expect(
      dropdownLocator.getByText('Enumerator', {exact: true}),
    ).toBeVisible()
    await expect(
      dropdownLocator.getByText('File Upload', {exact: true}),
    ).toBeVisible()
    await expect(dropdownLocator.getByText('ID', {exact: true})).toBeVisible()
    await expect(dropdownLocator.getByText('Name', {exact: true})).toBeVisible()
    await expect(
      dropdownLocator.getByText('Number', {exact: true}),
    ).toBeVisible()
    await expect(
      dropdownLocator.getByText('Phone Number', {exact: true}),
    ).toBeVisible()
    await expect(
      dropdownLocator.getByText('Radio Button', {exact: true}),
    ).toBeVisible()
    await expect(
      dropdownLocator.getByText('Static Text', {exact: true}),
    ).toBeVisible()
    await expect(dropdownLocator.getByText('Text', {exact: true})).toBeVisible()
    await expect(
      dropdownLocator.getByText('Yes/No', {exact: true}),
    ).toBeVisible()
  })
})

test.describe('Admin question list on programs page', () => {
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

    await expect(
      dropdownLocator.getByText('Address', {exact: true}),
    ).toBeVisible()
    await expect(
      dropdownLocator.getByText('Checkbox', {exact: true}),
    ).toBeVisible()
    await expect(
      dropdownLocator.getByText('Currency', {exact: true}),
    ).toBeVisible()
    await expect(dropdownLocator.getByText('Date', {exact: true})).toBeVisible()
    await expect(
      dropdownLocator.getByText('Dropdown', {exact: true}),
    ).toBeVisible()
    await expect(
      dropdownLocator.getByText('Email', {exact: true}),
    ).toBeVisible()
    await expect(
      dropdownLocator.getByText('Enumerator', {exact: true}),
    ).toBeVisible()
    await expect(
      dropdownLocator.getByText('File Upload', {exact: true}),
    ).toBeVisible()
    await expect(dropdownLocator.getByText('ID', {exact: true})).toBeVisible()
    await expect(dropdownLocator.getByText('Name', {exact: true})).toBeVisible()
    await expect(
      dropdownLocator.getByText('Number', {exact: true}),
    ).toBeVisible()
    await expect(
      dropdownLocator.getByText('Phone Number', {exact: true}),
    ).toBeVisible()
    await expect(
      dropdownLocator.getByText('Radio Button', {exact: true}),
    ).toBeVisible()
    await expect(
      dropdownLocator.getByText('Static Text', {exact: true}),
    ).toBeVisible()
    await expect(dropdownLocator.getByText('Text', {exact: true})).toBeVisible()
    await expect(
      dropdownLocator.getByText('Yes/No', {exact: true}),
    ).toBeVisible()
  })
})
