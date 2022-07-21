import {Page} from 'playwright'

export class TiDashboard {
  public page!: Page

  constructor(page: Page) {
    this.page = page
  }

  async gotoTIDashboardPage(page: Page) {
    await page.click('text="Trusted intermediary dashboard"')
  }

  async fillFormForNewClients(client: ClientInformation) {
    await this.page.fill('label:has-text("Email Address")', client.emailAddress)
    await this.page.fill('label:has-text("First Name")', client.firstName)
    await this.page.fill('label:has-text("Middle Name")', client.middleName)
    await this.page.fill('label:has-text("Last Name")', client.lastName)
    await this.page.fill('label:has-text("Date Of Birth")', client.dobDate)
    await this.page.click('text ="Add"')
  }

  async checkInnerTableForClientInformation(client: ClientInformation) {
    const tableInnerText = await this.page.innerText('table')
    expect(tableInnerText).toContain(this.convertToMMDDYYYY(client.dobDate))
    expect(tableInnerText).toContain(client.emailAddress)
    expect(tableInnerText).toContain(client.firstName)
    expect(tableInnerText).toContain(client.lastName)
  }
  async checkInnterTableNotToConatain(client: ClientInformation) {
    const tableInnerText = await this.page.innerText('table')
    expect(tableInnerText).not.toContain(this.convertToMMDDYYYY(client.dobDate))
    expect(tableInnerText).not.toContain(client.emailAddress)
    expect(tableInnerText).not.toContain(client.firstName)
    expect(tableInnerText).not.toContain(client.lastName)
  }
  async searchByDob(dobDate: string) {
    await this.page.fill('label:has-text("Search Date Of Birth")', dobDate)
    await this.page.click('button:text("Search")')
  }
  convertToMMDDYYYY(dobDate: string): string {
    //Input format :  '2021-10-10' YYYY-MM-DD O/p- MM-DD-YYYY
    const arr = dobDate.split('-')
    return arr[1] + '-' + arr[2] + '-' + arr[0]
  }
}

export interface ClientInformation {
  emailAddress: string
  firstName: string
  middleName: string
  lastName: string
  dobDate: string
}
