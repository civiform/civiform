import {Page} from 'playwright'

/*
 * This class is to test Civiform in the TI path
 * It impements functionality to create a new client,Test if the given client is present in the dashboard
 * It also have a ClientInformation Interface to easily store clients
 * It requires the tests to be logged as a TI
 */
export class TIDashboard {
  public page!: Page

  constructor(page: Page) {
    this.page = page
  }

  async gotoTIDashboardPage(page: Page) {
    await page.click('text="Trusted intermediary dashboard"')
  }

  async createClient(client: ClientInformation) {
    await this.page.fill('label:has-text("Email Address")', client.emailAddress)
    await this.page.fill('label:has-text("First Name")', client.firstName)
    await this.page.fill('label:has-text("Middle Name")', client.middleName)
    await this.page.fill('label:has-text("Last Name")', client.lastName)
    await this.page.fill('label:has-text("Date Of Birth")', client.dobDate)
    await this.page.click('text="Add"')
  }

  async expectDashboardContainClient(client: ClientInformation) {
    expect(
      `.cf-admin-question-table-row:has-text("${this.convertToMMDDYYYY(
        client.dobDate,
      )}")`,
    )
    expect(`.cf-admin-question-table-row:has-text("${client.emailAddress}")`)
    expect(`.cf-admin-question-table-row:has-text("${client.firstName}")`)
    expect(`.cf-admin-question-table-row:has-text("${client.lastName}")`)
  }

  async expectDashboardNotContainClient(client: ClientInformation) {
    const tableInnerText = await this.page.innerText('table')
    expect(tableInnerText).not.toContain(this.convertToMMDDYYYY(client.dobDate))
    expect(tableInnerText).not.toContain(client.emailAddress)
    expect(tableInnerText).not.toContain(client.firstName)
    expect(tableInnerText).not.toContain(client.lastName)
  }

  async searchByDateOfBirth(dobDate: string) {
    await this.page.fill('label:has-text("Search Date Of Birth")', dobDate)
    await this.page.click('button:text("Search")')
  }

  convertToMMDDYYYY(dobDate: string): string {
    const [year, month, day] = dobDate.split('-')
    return `${month}-${day}-${year}`
  }
}

/*
 * This class helps to test the TrustesIntermediary dashboard changes
 * We should be logged as a TI to use this class
 */
export interface ClientInformation {
  emailAddress: string
  firstName: string
  middleName: string
  lastName: string
  dobDate: string
}
