import {Page} from 'playwright'
import {BASE_URL} from './config'
import {TestContext} from './index'

export class NotFoundPage {
  public ctx: TestContext

  constructor(ctx: TestContext) {
    this.ctx = ctx
    ctx.browserErrorWatcher.ignoreErrorsFromUrl(/.*\/ezbezzdebashiboozook/)
  }

  async gotoNonExistentPage(page: Page) {
    return await page.goto(BASE_URL + '/ezbezzdebashiboozook')
  }

  async checkPageHeader(lang = 'en-US') {
    if (lang === 'es-US') {
      expect(await this.ctx.page.innerText('h1')).toContain(
        'No Pudimos encontrar la página que intentó visitar',
      )
    } else {
      expect(await this.ctx.page.innerText('h1')).toContain(
        'We were unable to find the page you tried to visit',
      )
    }
  }
}
