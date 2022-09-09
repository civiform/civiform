import {Page} from 'playwright'
import {BASE_URL} from './config'

export class NotFoundPage {
  public page!: Page

  constructor(page: Page) {
    this.page = page
  }

  async gotoNonExistentPage(page: Page) {
    return await page.goto(BASE_URL + '/ezbezzdebashiboozook')
  }

  async checkPageHeader(lang = 'en-US') {
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
}
