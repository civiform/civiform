import {addEventListenerToElements, assertNotNull} from './util'

/** Supports the CiviForm Admin system-wide settings page. */
class AdminSettingsController {
  private static SETTINGS_SIDE_NAV_ID = 'admin-settings-side-nav'

  constructor() {
    const sideNavEl = document.getElementById(
      AdminSettingsController.SETTINGS_SIDE_NAV_ID,
    )

    if (sideNavEl == null) {
      return
    }

    addEventListenerToElements(
      '#admin-settings-side-nav a',
      'click',
      (event: Event) => this.handleNavItemClicked(event),
    )
  }

  handleNavItemClicked(event: Event) {
    event.preventDefault()

    const link = event.target as HTMLAnchorElement
    const href = assertNotNull(link.href.split('#').pop())

    const targetSectionHeader = assertNotNull(
      document.getElementById(href),
      `No section header found with ID ${href}`,
    )
    const targetScrollHeight =
      window.pageYOffset +
      targetSectionHeader.getBoundingClientRect().top -
      this.getNavBottom()
    window.scrollTo(window.pageXOffset, targetScrollHeight)
  }

  getNavBottom(): number {
    return assertNotNull(document.querySelector('nav')).getBoundingClientRect()
      .bottom
  }
}

export function init() {
  new AdminSettingsController()
}
