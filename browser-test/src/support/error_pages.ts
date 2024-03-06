import {expect} from '@playwright/test'
import {Page} from 'playwright'
import {BASE_URL} from './config'
import {TestContext} from './index'

export class NotFoundPage {
  private static NON_EXISTENT_PATH = 'ezbezzdebashiboozook'
  public ctx: TestContext

  constructor(ctx: TestContext) {
    this.ctx = ctx
    ctx.browserErrorWatcher.ignoreErrorsFromUrl(
      new RegExp(NotFoundPage.NON_EXISTENT_PATH),
    )
  }

  async gotoNonExistentPage(page: Page) {
    return await page.goto(BASE_URL + '/' + NotFoundPage.NON_EXISTENT_PATH)
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
