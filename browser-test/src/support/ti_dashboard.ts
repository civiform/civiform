import {Page} from 'playwright'

export class TiDashboard {
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
asyn checkUpdatedDateOfBirth(client: ClientInformation,newDob : string)
{
    const tableInnerText = await this.page.innerText('table')
    expect(tableInnerText).toContain(this.convertToMMDDYYYY(newDob))
    expect(tableInnerText).toContain(client.emailAddress)
    expect(tableInnerText).toContain(client.firstName)
    expect(tableInnerText).toContain(client.lastName)

}
  async checkInnerTableForClientInformation(client: ClientInformation) {
    const tableInnerText = await this.page.innerText('table')
    expect(tableInnerText).toContain(this.convertToMMDDYYYY(client.dobDate))
    expect(tableInnerText).toContain(client.emailAddress)
    expect(tableInnerText).toContain(client.firstName)
    expect(tableInnerText).toContain(client.lastName)
  }
  async checkInnerTableNotToContainClient(client: ClientInformation) {
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
    //Input format :  '2021-10-10' YYYY-MM-DD O/p- MM-DD-YYYY
    const arr = dobDate.split('-')
    return arr[1] + '-' + arr[2] + '-' + arr[0]
  }
  async updateClientDateOfBirth(client : ClientInformation,newDobDate : string)
  {
    await this.page.fill('label:has-text("Date Of Birth")', dobDate)
    await this.page.click('text ="Add DOB"')
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
