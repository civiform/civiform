/**
 * Input formatting for PhoneQuestion question type
 * This file requires third party dependency- cleave.js for formatting the input as the user types.
 * Documentation for it can be found here: https://github.com/nosir/cleave.js/
 */

import Cleave from 'cleave.js'

function formatPhone(element: HTMLElement) {
  // Guard against attaching a second Cleave instance to an input that is already formatted. We
  // re-scan after HTMX swaps (see init), and a swapped container can also hold phone inputs that
  // were already formatted on page load; a second Cleave would double up its listeners.
  if (element.dataset.phoneFormatted) {
    return
  }
  element.dataset.phoneFormatted = 'true'
  new Cleave(element, {
    // Only accept digits as input
    numericOnly: true,
    // An Array value indicates the groups to format the input value. It will insert delimiters in between these groups.
    // it will take the index from the last block. ex.0 -> insert '(' at 0th spot, 3 -> insert ')' after 3 characters from 0th block
    blocks: [0, 3, 3, 4],
    // the delimiters at the specified blocks
    delimiters: ['(', ') ', '-'],
  })
}

function formatPhoneInputs(root: Document | HTMLElement) {
  // Match the input inside each phone question's `.cf-phone-number` wrapper. Selecting by wrapper
  // class (rather than the input's id prefix) also covers phone questions used as an enumerator's
  // initial question, whose inputs get a generic per-entity id instead of a `cf-phone-number-` one.
  root.querySelectorAll('.cf-phone-number input').forEach((el) => {
    formatPhone(el as HTMLElement)
  })
}

export function init() {
  formatPhoneInputs(document)

  const phoneElement = document.getElementById('phone-number-input')
  if (phoneElement) {
    formatPhone(phoneElement)
  }

  // A phone question can be an enumerator's initial question, whose entity rows are added after
  // load when HTMX swaps a new row onto the page. init() has already run by then, so also format
  // phone inputs in freshly swapped content (formatPhone skips any that are already formatted).
  document.addEventListener('htmx:afterSettle', (event) => {
    formatPhoneInputs(event.detail.target)
  })
}
