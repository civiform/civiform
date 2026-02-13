import {expect} from './civiform_fixtures'
import {Locator, Page} from '@playwright/test'
import {BASE_URL} from './config'

export class AdminSettings {
  public page!: Page

  constructor(page: Page) {
    this.page = page
  }

  async gotoAdminSettings() {
    await this.page.goto(BASE_URL + `/admin/settings`)
    await expect(
      this.page.getByRole('heading', {name: 'Settings', exact: true}),
    ).toBeVisible()
  }

  getBooleanSettingLocator(
    settingName: string,
    value: 'True' | 'False',
  ): Locator {
    return this.page.getByRole('group', {name: settingName}).getByText(value)
  }

  async enableSetting(settingName: string) {
    await this.getBooleanSettingLocator(settingName, 'True').click()
  }

  async disableSetting(settingName: string) {
    await this.getBooleanSettingLocator(settingName, 'False').click()
  }

  async expectEnabled(settingName: string) {
    await expect(
      this.getBooleanSettingLocator(settingName, 'True'),
    ).toBeChecked()
  }

  async expectDisabled(settingName: string) {
    await expect(
      this.getBooleanSettingLocator(settingName, 'False'),
    ).toBeChecked()
  }

  async setStringSetting(settingName: string, value: string) {
    await this.page.getByTestId(`string-${settingName}`).fill(value)
  }

  async expectStringSetting(settingName: string, value: string) {
    await expect(
      this.page.getByTestId(`string-${settingName}`).locator('input'),
    ).toHaveValue(value)
  }

  async saveChanges(expectUpdated = true, expectError = false) {
    await this.page.getByRole('button', {name: 'Save changes'}).click()

    // TODO GWEN
    // const toastMessages = await this.page.innerText('#toast-container')

    // if (expectUpdated) {
    //   expect(toastMessages).toContain('Settings updated')
    // } else if (expectError) {
    //   expect(toastMessages).toContain("Error: That update didn't look quite right. Please fix the errors in the form and try saving again.")
    // } else {
    //   expect(toastMessages).toContain('No changes to save')
    // }
  }

  async expectColorContrastErrorVisible() {
    await expect(
      this.page.getByText(
        "This color doesn't have enough contrast to be legible with white text.",
      ),
    ).toBeVisible()
  }
}
