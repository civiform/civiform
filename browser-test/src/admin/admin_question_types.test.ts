import {test, expect} from '../support/civiform_fixtures'
import {loginAsAdmin} from '../support'

test.describe('Admin question list on questions page', () => {
  test('displays list of available question types', async ({
    page,
    adminQuestions,
  }) => {
    await loginAsAdmin(page)
    await adminQuestions.gotoAdminQuestionsPage()

    await page.getByRole('button', {name: 'Create new question'}).click()

    const menuLocator = page.getByRole('menu', {name: 'New Question Options'})

    await expect(
      menuLocator.getByRole('menuitem', {name: 'Address'}),
    ).toBeVisible()
    await expect(
      menuLocator.getByRole('menuitem', {name: 'Checkbox'}),
    ).toBeVisible()
    await expect(
      menuLocator.getByRole('menuitem', {name: 'Currency'}),
    ).toBeVisible()
    await expect(
      menuLocator.getByRole('menuitem', {name: 'Date'}),
    ).toBeVisible()
    await expect(
      menuLocator.getByRole('menuitem', {name: 'Dropdown'}),
    ).toBeVisible()
    await expect(
      menuLocator.getByRole('menuitem', {name: 'Email'}),
    ).toBeVisible()
    await expect(
      menuLocator.getByRole('menuitem', {name: 'Enumerator'}),
    ).toBeVisible()
    await expect(
      menuLocator.getByRole('menuitem', {name: 'File Upload'}),
    ).toBeVisible()
    await expect(menuLocator.getByRole('menuitem', {name: 'ID'})).toBeVisible()
    await expect(
      menuLocator.getByRole('menuitem', {name: 'Name'}),
    ).toBeVisible()
    await expect(
      menuLocator.getByRole('menuitem', {name: 'Number', exact: true}),
    ).toBeVisible()
    await expect(
      menuLocator.getByRole('menuitem', {name: 'Phone Number'}),
    ).toBeVisible()
    await expect(
      menuLocator.getByRole('menuitem', {name: 'Radio Button'}),
    ).toBeVisible()
    await expect(
      menuLocator.getByRole('menuitem', {name: 'Static Text'}),
    ).toBeVisible()
    await expect(
      menuLocator.getByRole('menuitem', {name: 'Text', exact: true}),
    ).toBeVisible()
    await expect(
      menuLocator.getByRole('menuitem', {name: 'Yes/No'}),
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

    const menuLocator = page.getByRole('menu', {name: 'New Question Options'})

    await expect(
      menuLocator.getByRole('menuitem', {name: 'Address'}),
    ).toBeVisible()
    await expect(
      menuLocator.getByRole('menuitem', {name: 'Checkbox'}),
    ).toBeVisible()
    await expect(
      menuLocator.getByRole('menuitem', {name: 'Currency'}),
    ).toBeVisible()
    await expect(
      menuLocator.getByRole('menuitem', {name: 'Date'}),
    ).toBeVisible()
    await expect(
      menuLocator.getByRole('menuitem', {name: 'Dropdown'}),
    ).toBeVisible()
    await expect(
      menuLocator.getByRole('menuitem', {name: 'Email'}),
    ).toBeVisible()
    await expect(
      menuLocator.getByRole('menuitem', {name: 'Enumerator'}),
    ).toBeVisible()
    await expect(
      menuLocator.getByRole('menuitem', {name: 'File Upload'}),
    ).toBeVisible()
    await expect(menuLocator.getByRole('menuitem', {name: 'ID'})).toBeVisible()
    await expect(
      menuLocator.getByRole('menuitem', {name: 'Name'}),
    ).toBeVisible()
    await expect(
      menuLocator.getByRole('menuitem', {name: 'Number', exact: true}),
    ).toBeVisible()
    await expect(
      menuLocator.getByRole('menuitem', {name: 'Phone Number'}),
    ).toBeVisible()
    await expect(
      menuLocator.getByRole('menuitem', {name: 'Radio Button'}),
    ).toBeVisible()
    await expect(
      menuLocator.getByRole('menuitem', {name: 'Static Text'}),
    ).toBeVisible()
    await expect(
      menuLocator.getByRole('menuitem', {name: 'Text', exact: true}),
    ).toBeVisible()
    await expect(
      menuLocator.getByRole('menuitem', {name: 'Yes/No'}),
    ).toBeVisible()
  })
})
