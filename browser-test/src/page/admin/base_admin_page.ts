import {Page} from '@playwright/test'
import {BasePage} from '../base_page'

/**
 * Represents an admin page and related global components
 */
export class BaseAdminPage extends BasePage {
  constructor(page: Page) {
    super(page)
  }

  /**
   * Clicks the primary navigation link to goto that page
   */
  async clickPrimaryNavLink(name: string) {
    const primaryNav = this.page.getByRole('navigation', {
      name: 'Primary navigation',
    })

    await primaryNav.getByRole('link', {name: name}).click()
  }

  /**
   * Clicks the primary navigation link within the specified popup menu
   */
  async clickPrimaryNavSubMenuLink(menuName: string, linkName: string) {
    const primaryNav = this.page.getByRole('navigation', {
      name: 'Primary navigation',
    })

    await primaryNav.getByRole('button', {name: menuName}).click()
    await primaryNav.getByRole('link', {name: linkName}).click()
  }
}
