import {Page} from 'playwright'
import {waitForPageJsLoad} from './wait'
import {BASE_URL} from './'

export class NotFoundPage {
  public page!: Page

  constructor(page: Page) {
    this.page = page
  }

  async gotoNonExistentPage(page: Page) {
    return await page.goto(BASE_URL + '/ezbezzdebashiboozook')
  }

  async loginAsGuest(lang: String = 'en-US') {
    await this.page.click('#guestLogin')
    await waitForPageJsLoad(this.page)
    if (lang === 'es-US') {
      await this.page.click('text=Español')
    }

    await this.page.click('text=Submit')
  }

  async logout(lang: String = 'en-US') {
    if (lang === 'es-US') {
      await this.page.click('text=Cerrar Sesión')
    } else {
      await this.page.click('text=Logout')
    }
  }

  async checkPageHeaderEnUS() {
    expect(await this.page.innerText('h1')).toContain(
      'We were unable to find the page you tried to visit',
    )
  }

  async checkPageHeader(lang: String = 'en-US') {
    if (lang === 'es-US') {
      expect(await this.page.innerText('h1')).toContain(
        'No Pudimos encontrar la página que intentó visitar',
      )
    } else {
      expect(await this.page.innerText('h1')).toContain(
        'We were unable to find the page you tried to visit',
      )
    }
  }

  async checkIsGuest(lang: String = 'en-US') {
    if (lang === 'es-US') {
      await this.page.waitForSelector('text=Conectado como Guest')
    } else {
      await this.page.waitForSelector('text=Logged in as Guest')
    }
  }

  async checkNotLoggedIn() {
    await this.page.waitForSelector('#guestLogin')
  }
}
