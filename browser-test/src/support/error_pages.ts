import {expect} from '@playwright/test'
import {Page} from 'playwright'
import {BASE_URL} from './config'
import { BrowserErrorWatcher } from './browser_error_watcher'

export class NotFoundPage {
  private static NON_EXISTENT_PATH = 'ezbezzdebashiboozook'
  public page!: Page

  constructor(page: Page) {
    this.page = page
    const browserErrorWatcher = new BrowserErrorWatcher(page)
    browserErrorWatcher.ignoreErrorsFromUrl(
      new RegExp(NotFoundPage.NON_EXISTENT_PATH),
    )
  }

  async gotoNonExistentPage(page: Page) {
    return await page.goto(BASE_URL + '/' + NotFoundPage.NON_EXISTENT_PATH)
  }

  async checkPageHeader(lang = 'en-US') {
    if (lang === 'es-US') {
      await expect(this.page.locator('h1')).toHaveText('No Pudimos encontrar la página que intentó visitar')
    } else {
      await expect(this.page.locator('h1')).toHaveText('We were unable to find the page you tried to visit')
    }
  }
}
