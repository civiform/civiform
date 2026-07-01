/**
 * Behavior for the program category filter on the applicant program index page.
 *
 * The filter form (`#ns-category-filter-form`) is HTMX-driven: selecting
 * categories and pressing "Apply selections" (`#filter-submit`) issues the
 * `hx-get` and swaps the results in place, so there is no form-submission logic
 * here. This class handles responsive checkbox styling and resetting the
 * checkboxes when the "Clear selections" button is pressed.
 */
import {addEventListenerToElements} from '@/util'

export class CategoryFilter {
  /** Kick everything off. Call once after the DOM is ready. */
  init() {
    addEventListener('resize', () => this.handleMediaQueryChange())
    this.handleMediaQueryChange()

    /* Uncheck all program filter checkboxes when the clear filters button is clicked */
    addEventListenerToElements('#clear-filters', 'click', () => {
      const checkboxes = document.querySelectorAll('[id*="ns-check-category"]')

      checkboxes.forEach((checkbox) => {
        const checkboxInput = checkbox as HTMLInputElement
        checkboxInput.checked = false
      })
    })
  }

  /**
   * Add USWDS checkbox tile CSS class to program filter checkboxes for tablet
   * and mobile.
   */
  private handleMediaQueryChange() {
    const mediaQuery = window.matchMedia('(max-width: 63.9em)')
    const checkboxes = document.querySelectorAll('[id*="ns-check-category-"]')

    if (mediaQuery.matches) {
      checkboxes.forEach((checkbox) => {
        checkbox.classList.add('usa-checkbox__input--tile')
      })
    } else {
      checkboxes.forEach((checkbox) => {
        checkbox.classList.remove('usa-checkbox__input--tile')
      })
    }
  }
}
