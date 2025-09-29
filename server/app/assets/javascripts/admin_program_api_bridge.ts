import {HtmxAfterSwapEvent} from './htmx_request'

/**
 * Client side event handlers for the program api bridge edit page only
 */
export class AdminProgramApiBridge {
  static init(event: HtmxAfterSwapEvent): void {
    // Only update if the target is the 'output' element in this swap
    if (event.target.id !== 'output') {
      return
    }

    // Remove existing listeners and bind to new ones after the swap
    // replaces the html.
    document
      .querySelectorAll<HTMLSelectElement>('[id^=question-id]')
      .forEach((dropdown: HTMLSelectElement) => {
        dropdown.removeEventListener('change', this.onQuestionDropdownChange)
        dropdown.addEventListener('change', this.onQuestionDropdownChange)
      })
  }

  /**
   * Event handler for the page question dropdowns
   */
  private static onQuestionDropdownChange = (event: Event): void => {
    const questionDropdown = event.target as HTMLSelectElement

    if (!questionDropdown) {
      return
    }

    const scalarDropdownId = questionDropdown.getAttribute(
      'data-scalar-target-id',
    )

    if (!scalarDropdownId) {
      return
    }

    const scalarDropdown = document.getElementById(
      scalarDropdownId,
    ) as HTMLSelectElement

    if (!scalarDropdown) {
      return
    }

    const questionScalarMap = window.app?.data?.bridge?.question_scalars

    if (!questionScalarMap?.[questionDropdown.value]) {
      return
    }

    const scalars: {value: string; display: string}[] =
      questionScalarMap[questionDropdown.value]

    // Clear existing options
    scalarDropdown.innerHTML = ''

    // Only show if there are multiple choices
    if (scalars.length > 1) {
      scalarDropdown.appendChild(this.createOption('', '-- Select option --'))
    }

    scalars.forEach((scalar: {value: string; display: string}) => {
      scalarDropdown.appendChild(
        this.createOption(scalar.value, scalar.display),
      )
    })
  }

  /**
   * Create and populate an option element
   */
  private static createOption(value: string, text: string): HTMLOptionElement {
    const optionElement: HTMLOptionElement = document.createElement('option')
    optionElement.value = value
    optionElement.textContent = text
    return optionElement
  }
}
