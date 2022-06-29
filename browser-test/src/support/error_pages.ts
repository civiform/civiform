import {Page} from 'playwright'
import {waitForPageJsLoad} from './wait'
import {BASE_URL} from './'

export class NotFoundPage {
  public page!: Page

  constructor(page: Page) {
    this.page = page
  }

  async gotoNonExistentPage(page: Page) {
    return await page.goto(BASE_URL + '/dirjwlqickhdfguyqrm')
  }

  async loginAsGuest() {
    await this.page.click('#guestLogin')
    await waitForPageJsLoad(this.page)
  }

  async gotoMockNotFoundPage(page: Page) {
    return await page.goto(BASE_URL + '/dev/abc')
  }

  async checkPageHeaderEnUS() {
    expect(await this.page.innerText('h1')).toContain(
      'We were unable to find the page you tried to visit',
    )
  }

  async checkNotLoggedIn() {
    await this.page.waitForSelector('#guestLogin')
  }

  async checkIsGuest() {
    await this.page.waitForSelector('text=Logged in as Guest')
  }
}
