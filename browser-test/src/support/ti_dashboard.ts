import {Page} from 'playwright'
//This class is to test Civiform's TI to add new clients and search functionality
//It requires the tests to be logged as a TI
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
    await this.page.click('text ="Add"')
  }
  async checkUpdatedDateOfBirth(client: ClientInformation, newDob: string) {
    expect(`.cf-admin-question-table-row:has-text("${this.convertToMMDDYYYY(newDob)}")`)
    expect(`.cf-admin-question-table-row:has-text("${client.emailAddress}")`)
    expect(`.cf-admin-question-table-row:has-text("${client.firstName}")`)
    expect(`.cf-admin-question-table-row:has-text("${client.lastName}")`)
  }
  async checkInnerTableForClientInformation(client: ClientInformation) {
    expect(`.cf-admin-question-table-row:has-text("${this.convertToMMDDYYYY(client.dobDate)}")`)
    expect(`.cf-admin-question-table-row:has-text("${client.emailAddress}")`)
    expect(`.cf-admin-question-table-row:has-text("${client.firstName}")`)
    expect(`.cf-admin-question-table-row:has-text("${client.lastName}")`)
  }
  async checkInnerTableNotToContainClient(client: ClientInformation) {
    expect(`.cf-admin-question-table-row:has-text("${this.convertToMMDDYYYY(client.dobDate)}")`).toBe(false)
    expect(`.cf-admin-question-table-row:has-text("${client.emailAddress}")`).toBe(false)
    expect(`.cf-admin-question-table-row:has-text("${client.firstName}")`).toBe(false)
    expect(`.cf-admin-question-table-row:has-text("${client.lastName}")`).toBe(false)
  }
  async searchByDateOfBirth(dobDate: string) {
    await this.page.fill('label:has-text("Search Date Of Birth")', dobDate)
    await this.page.click('button:text("Search")')
  }
  convertToMMDDYYYY(dobDate: string): string {
    //Input format :  '2021-10-10' YYYY-MM-DD O/p- MM-DD-YYYY
    const [year, month, day] = dobDate.split('-')
    return `${month}-${day}-${year}`
  }
  async updateClientDateOfBirth(client: ClientInformation, newDobDate: string) {
    await this.page.locator('id=date-of-birth-update').fill('newDobDate');
    await this.page.click('text ="add DOB"')
  }
}
//This class helps to test the TrustesIntermediary dashboard changes
//We should be logged as a TI to use this class
export interface ClientInformation {
  emailAddress: string
  firstName: string
  middleName: string
  lastName: string
  dobDate: string
}
