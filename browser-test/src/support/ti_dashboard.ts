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

  async updateClientEmailAddress(client: ClientInformation, newEmail: string) {
    await this.page
      .getByRole('row')
      .filter({hasText: client.emailAddress})
      .getByText('Edit')
      .click()
    await waitForPageJsLoad(this.page)
    await this.page.waitForSelector('h2:has-text("Edit Client")')
    await this.page.fill('#edit-email-input', newEmail)
    await this.page.click('text="Save"')
    await waitForPageJsLoad(this.page)
  }

  async updateClientTiNoteAndPhone(
    client: ClientInformation,
    tiNote: string,
    phone: string,
  ) {
    await this.page
      .getByRole('row')
      .filter({hasText: client.emailAddress})
      .getByText('Edit')
      .click()
    await waitForPageJsLoad(this.page)
    await this.page.waitForSelector('h2:has-text("Edit Client")')
    await this.page.fill('#edit-phone-number-input', phone)
    await this.page.fill('#edit-ti-note-input', tiNote)
    await this.page.click('text="Save"')
    await waitForPageJsLoad(this.page)
  }

  async updateClientDateOfBirth(client: ClientInformation, newDobDate: string) {
    await this.page
      .getByRole('row')
      .filter({hasText: client.emailAddress})
      .getByText('Edit')
      .click()
    await waitForPageJsLoad(this.page)
    await this.page.waitForSelector('h2:has-text("Edit Client")')
    await this.page.fill('#edit-date-of-birth-input', newDobDate)
    await this.page.click('text="Save"')
    await waitForPageJsLoad(this.page)
  }

  async expectClientContainsTiNoteAndPhone(
    client: ClientInformation,
    tiNote: string,
    phone: string,
  ) {
    await this.page
      .getByRole('row')
      .filter({hasText: client.emailAddress})
      .getByText('Edit')
      .click()
    await waitForPageJsLoad(this.page)
    await this.page.waitForSelector('h2:has-text("Edit Client")')
    const text = await this.page.innerHTML('#edit-ti')
    expect(text).toContain(phone)
    expect(text).toContain(tiNote)
  }

  async expectDashboardContainClient(client: ClientInformation) {
    const row = this.page.locator(
      `.cf-admin-question-table-row:has-text("${client.lastName}, ${client.firstName}")`,
    )
    const rowText = await row.innerText()
    expect(rowText).toContain(client.emailAddress)
    expect(rowText).toContain(client.dobDate)
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

  async searchByNameAndDateOfBirth(name: string, dobDate: string) {
    await this.page.fill('label:has-text("Search by Name")', name)
    await this.page.fill('label:has-text("Search Date Of Birth")', dobDate)
    await this.page.click('button:text("Search")')
  }

  async clickOnApplicantDashboard() {
    await this.page
      .locator('.cf-admin-question-table-row a:text("Applicant Dashboard")')
      .click()
    await waitForPageJsLoad(this.page)
  }

  async expectSuccessToast(successToastMessage: string) {
    const toastContainer = await this.page.innerHTML('#toast-container')

    expect(toastContainer).toContain('bg-emerald-200')
    expect(toastContainer).toContain(successToastMessage)
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
