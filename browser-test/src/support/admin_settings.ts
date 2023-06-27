import {Page} from 'playwright'
import {BASE_URL} from './config'

export class AdminSettings {
  public page!: Page

  constructor(page: Page) {
    this.page = page
  }

  async gotoAdminSettings() {
    await this.page.goto(BASE_URL + `/admin/settings`)
    await this.page.waitForSelector('h1:has-text("Settings")')
  }

  async enableSetting(settingName: string) {
    await this.page.getByTestId(`enable-${settingName}`).click()
  }

  async disableSetting(settingName: string) {
    await this.page.getByTestId(`disable-${settingName}`).click()
  }

  async expectEnabled(settingName: string) {
    expect(
      await this.page
        .getByTestId(`enable-${settingName}`)
        .getByRole('radio')
        .isChecked(),
    ).toBe(true)
  }

  async expectDisabled(settingName: string) {
    expect(
      await this.page
        .getByTestId(`disable-${settingName}`)
        .getByRole('radio')
        .isChecked(),
    ).toBe(true)
  }

  async saveChanges(expectUpdated = true) {
    await this.page.click('button:text("Save changes")')

    const toastMessages = await this.page.innerText('#toast-container')

    if (expectUpdated) {
      expect(toastMessages).toContain('Settings updated')
    } else {
      expect(toastMessages).toContain('No changes to save')
    }
  }
}
