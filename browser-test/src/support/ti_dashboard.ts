import {expect} from '@playwright/test'
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
    await page.click('text="View and add clients"')
  }

  async goToProgramsPageForCurrentClient() {
    await this.page.click('text="CiviForm"')
  }

  async createClient(client: ClientInformation) {
    await this.page.getByRole('link', {name: 'Add new client'}).click()
    await waitForPageJsLoad(this.page)
    await this.page.fill('#email-input', client.emailAddress)
    await this.page.fill('#first-name-input', client.firstName)
    await this.page.fill('#middle-name-input', client.middleName)
    await this.page.fill('#last-name-input', client.lastName)
    await this.page.fill('#date-of-birth-input', client.dobDate)
    if (client.notes != undefined) {
      await this.page.fill('#ti-note-input', client.notes)
    }
    if (client.phoneNumber != undefined) {
      await this.page.fill('#phone-number-input', client.phoneNumber)
    }

    await this.page.getByRole('button', {name: 'Save'}).click()
    await waitForPageJsLoad(this.page)
    await this.gotoTIDashboardPage(this.page)
    await waitForPageJsLoad(this.page)
  }

  async createMultipleClients(nameBase: string, copies: number) {
    await this.page.getByRole('link', {name: 'Add new client'}).click()

    for (let i = 1; i <= copies; i++) {
      await waitForPageJsLoad(this.page)
      await this.page.fill('#email-input', nameBase + i + '@test.com')
      await this.page.fill('#first-name-input', nameBase + i)
      await this.page.fill('#last-name-input', 'last')
      await this.page.fill('#date-of-birth-input', '2021-05-10')
      await this.page.getByRole('button', {name: 'Save'}).click()
    }

    await waitForPageJsLoad(this.page)
    await this.gotoTIDashboardPage(this.page)
    await waitForPageJsLoad(this.page)
  }

  async updateClientEmailAddress(client: ClientInformation, newEmail: string) {
    await this.page
      .getByRole('listitem')
      .filter({hasText: client.emailAddress})
      .getByText('Edit')
      .click()
    await waitForPageJsLoad(this.page)
    await this.page.waitForSelector('h2:has-text("Edit client")')
    await this.page.fill('#email-input', newEmail)
    await this.page.click('text="Save"')
    await waitForPageJsLoad(this.page)
  }

  async updateClientTiNoteAndPhone(
    client: ClientInformation,
    tiNote: string,
    phone: string,
  ) {
    await this.page
      .getByRole('listitem')
      .filter({hasText: client.emailAddress})
      .getByText('Edit')
      .click()
    await waitForPageJsLoad(this.page)
    await this.page.waitForSelector('h2:has-text("Edit client")')
    await this.page.fill('#phone-number-input', phone)
    await this.page.fill('#ti-note-input', tiNote)
    await this.page.click('text="Save"')
    await waitForPageJsLoad(this.page)
  }

  async updateClientDateOfBirth(client: ClientInformation, newDobDate: string) {
    await this.page
      .getByRole('listitem')
      .filter({hasText: client.emailAddress})
      .getByText('Edit')
      .click()
    await waitForPageJsLoad(this.page)
    await this.page.waitForSelector('h2:has-text("Edit client")')

    // The success alert should not be present before the form is submitted
    await this.expectSuccessAlertNotPresent()

    await this.page.fill('#date-of-birth-input', newDobDate)
    await this.page.click('text="Save"')
    await this.expectSuccessAlertOnUpdate()
  }

  async expectSuccessAlertNotPresent() {
    const editElement = await this.page.innerHTML('main')
    expect(editElement).not.toContain('.usa-alert--success')
  }

  async expectEditFormContainsTiNoteAndPhone(client: ClientInformation) {
    await this.page
      .getByRole('listitem')
      .filter({hasText: client.emailAddress})
      .getByText('Edit')
      .click()
    await waitForPageJsLoad(this.page)
    await this.page.waitForSelector('h2:has-text("Edit client")')
    expect(await this.page.textContent('html')).toContain(client.notes)
  }

  async expectDashboardClientContainsTiNoteAndFormattedPhone(
    client: ClientInformation,
    phone: string,
  ) {
    const cardContainer = this.page.locator(
      `.usa-card__container:has-text("${client.lastName}, ${client.firstName}")`,
    )
    const cardText = await cardContainer.innerText()
    expect(cardText).toContain(client.notes)
    expect(cardText).toContain(phone) // This should be in (xxx) xxx-xxxx format.
  }

  async expectDashboardContainClient(client: ClientInformation) {
    const cardContainer = this.page.locator(
      `.usa-card__container:has-text("${client.lastName}, ${client.firstName}")`,
    )
    const cardText = await cardContainer.innerText()
    expect(cardText).toContain(client.emailAddress)
    expect(cardText).toContain(client.dobDate)
  }

  async expectDashboardNotContainClient(client: ClientInformation) {
    const tableInnerText = await this.page.innerText('.usa-card-group')
    expect(tableInnerText).not.toContain(client.emailAddress)
    expect(tableInnerText).not.toContain(client.firstName)
    expect(tableInnerText).not.toContain(client.lastName)
  }

  async expectClientContainsNumberOfApplications(num: string) {
    const cardContainer = this.page.locator('.usa-card__body')
    const cardText = await cardContainer.innerText()
    expect(cardText).toContain(
      `${num} application${num == '1' ? '' : 's'} submitted`,
    )
  }

  async expectClientContainsProgramNames(programs: string[]) {
    const cardContainer = this.page.locator('#card_applications')
    const programsText = await cardContainer.innerText()
    expect(programsText).toBe(programs.join(', '))
  }

  async searchByDateOfBirth(dobDay: string, dobMonth: string, dobYear: string) {
    await this.page.fill('label:has-text("Day")', dobDay)
    await this.page.selectOption('#date_of_birth_month', dobMonth)
    await this.page.fill('label:has-text("Year")', dobYear)
    await this.page.click('button:text("Search")')
  }

  async searchByNameAndDateOfBirth(
    name: string,
    dobDay: string,
    dobMonth: string,
    dobYear: string,
  ) {
    await this.page.fill('label:has-text("Search by name(s)")', name)
    await this.page.fill('label:has-text("Day")', dobDay)
    await this.page.selectOption('#date_of_birth_month', dobMonth)
    await this.page.fill('label:has-text("Year")', dobYear)
    await this.page.click('button:text("Search")')
  }

  async clickOnViewApplications() {
    await this.page
      .locator('.usa-card__container a:text("View applications")')
      .click()
    await waitForPageJsLoad(this.page)
  }

  async expectSuccessToast(successToastMessage: string) {
    const toastContainer = await this.page.innerHTML('#toast-container')

    expect(toastContainer).toContain('bg-emerald-200')
    expect(toastContainer).toContain(successToastMessage)
  }

  async expectSuccessAlertOnUpdate() {
    const alertContainer = await this.page.innerHTML('.usa-alert--success')

    expect(alertContainer).toContain(
      'Client info has been successfully updated.',
    )
  }
  async expectSuccessAlertOnAddNewClient() {
    const alertContainer = await this.page.innerHTML('.usa-alert--success')

    expect(alertContainer).toContain('New client successfully created.')
  }

  async expectIneligiblePage() {
    expect(await this.page.innerText('h2')).toContain(
      'your client may not qualify',
    )
  }

  async expectPageNumberButton(pageNum: string) {
    expect(await this.page.innerHTML('.usa-pagination__list')).toContain(
      `aria-label="Page${pageNum}"`,
    )
  }

  async expectPageNumberButtonNotPresent(pageNum: string) {
    expect(await this.page.innerHTML('.usa-pagination__list')).not.toContain(
      `aria-label="Page${pageNum}"`,
    )
  }

  async expectDateSearchError() {
    const errorDiv = await this.page.innerHTML('#memorable_date_error')
    expect(errorDiv).toContain('Error:')
  }

  async expectApplyingForBannerNotPresent() {
    const tiBanner = this.page.getByRole('banner').locator('.ti-banner')
    await expect(tiBanner).toBeHidden()
  }

  expectRedDateFieldOutline(
    missingMonth: boolean,
    missingDay: boolean,
    missingYear: boolean,
  ) {
    if (missingMonth) {
      expect(
        this.page.locator('#date_of_birth_month .usa-input--error'),
      ).not.toBeNull()
    }
    if (missingDay) {
      expect(
        this.page.locator('#date_of_birth_day .usa-input--error'),
      ).not.toBeNull()
    }
    if (missingYear) {
      expect(
        this.page.locator('#date_of_birth_year .usa-input--error'),
      ).not.toBeNull()
    }
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
  phoneNumber?: string
  notes?: string
}
