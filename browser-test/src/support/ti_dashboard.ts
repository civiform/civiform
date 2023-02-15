import {Page} from 'playwright'
import {waitForPageJsLoad} from './wait'

/*
 * This class is to test Civiform in the TI path
 * It implements functionality to create a new client,Test if the given client is present/not present in the dashboard
 * It can also update a Client's Data of Birth to a new one
 * It also have a ClientInformation Interface to easily store clients
 * It requires the tests to be logged as a TI
 */
export class TIDashboard {
  public page!: Page

  constructor(page: Page) {
    this.page = page
  }

  async gotoTIDashboardPage(page: Page) {
    await page.click('text="View and Add Clients"')
  }

  async goToProgramsPageForCurrentClient() {
    await this.page.click('text="CiviForm"')
  }

  async createClient(client: ClientInformation) {
    await this.page.fill('label:has-text("Email Address")', client.emailAddress)
    await this.page.fill('label:has-text("First Name")', client.firstName)
    await this.page.fill('label:has-text("Middle Name")', client.middleName)
    await this.page.fill('label:has-text("Last Name")', client.lastName)
    await this.page.fill('label:has-text("Date Of Birth")', client.dobDate)
    await this.page.click('text="Add"')
  }

  async updateClientDateOfBirth(client: ClientInformation, newDobDate: string) {
    await this.page.locator('id=date-of-birth-update').fill(newDobDate)
    await this.page.click('text="Update DOB"')
  }

  async expectDashboardContainClient(client: ClientInformation) {
    const row = this.page.locator(
      `.cf-admin-question-table-row:has-text("${client.emailAddress}")`,
    )
    const rowText = await row.innerText()
    expect(rowText).toContain(client.firstName)
    expect(rowText).toContain(client.lastName)
    // date of birth rendered as <input> rather than plain text.
    expect(await row.locator('input[name="dob"]').inputValue()).toEqual(
      client.dobDate,
    )
  }

  async expectDashboardNotContainClient(client: ClientInformation) {
    const tableInnerText = await this.page.innerText('table')
    expect(tableInnerText).not.toContain(client.emailAddress)
    expect(tableInnerText).not.toContain(client.firstName)
    expect(tableInnerText).not.toContain(client.lastName)
  }

  async searchByDateOfBirth(dobDate: string) {
    await this.page.fill('label:has-text("Search Date Of Birth")', dobDate)
    await this.page.click('button:text("Search")')
  }

  async clickOnApplicantDashboard() {
    await this.page
      .locator('.cf-admin-question-table-row a:text("Applicant Dashboard")')
      .click()
    await waitForPageJsLoad(this.page)
  }

  async expectIneligiblePage() {
    expect(await this.page.innerText('h2')).toContain(
      'your client may not qualify',
    )
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
