import {expect} from './civiform_fixtures'
import {Page} from '@playwright/test'
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

  async enableSetting(settingName: string) {
    await this.page.getByTestId(`enable-${settingName}`).click()
  }

  async disableSetting(settingName: string) {
    await this.page.getByTestId(`disable-${settingName}`).click()
  }

  async expectEnabled(settingName: string) {
    await expect(
      this.page.getByTestId(`enable-${settingName}`).getByRole('radio'),
    ).toBeChecked()
  }

  async expectDisabled(settingName: string) {
    await expect(
      this.page.getByTestId(`disable-${settingName}`).getByRole('radio'),
    ).toBeChecked()
  }

  async setStringSetting(settingName: string, value: string) {
    await this.page
      .getByTestId(`string-${settingName}`)
      .locator('input')
      .fill(value)
  }

  async expectStringSetting(settingName: string, value: string) {
    await expect(
      this.page.getByTestId(`string-${settingName}`).locator('input'),
    ).toHaveValue(value)
  }

  async saveChanges(expectUpdated = true, expectError = false) {
    await this.page.click('button:text("Save changes")')

    const toastMessages = await this.page.innerText('#toast-container')

    if (expectUpdated) {
      expect(toastMessages).toContain('Settings updated')
    } else if (expectError) {
      expect(toastMessages).toContain(
        "Error: That update didn't look quite right. Please fix the errors in the form and try saving again.",
      )
    } else {
      expect(toastMessages).toContain('No changes to save')
    }
  }

  async expectColorContrastErrorVisible() {
    await expect(
      this.page.getByText(
        "This color doesn't have enough contrast to be legible with white text.",
      ),
    ).toBeVisible()
  }
}
