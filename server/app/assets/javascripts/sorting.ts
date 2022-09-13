/**
 * ElementSorter provides utilities for sorting a group of objects.
 */
class ElementSorter {
  private static sortableElementSelector = '.cf-sortable-element'

  static sortElementsOnLoad({
    parent,
    compareFunc,
  }: {
    parent: HTMLElement
    compareFunc: (first: Element, second: Element) => number
  }) {
    const elements = Array.from(
      parent.querySelectorAll(ElementSorter.sortableElementSelector),
    )
    try {
      ElementSorter.sortCards(parent, elements, compareFunc)
    } finally {
      // Make sure to always show the element, even
      // if there was an error while sorting.
      parent.classList.remove('invisible')
    }
  }

  private static sortCards(
    parent: HTMLElement,
    elements: Array<Element>,
    compareFunc: (a: Element, b: Element) => number,
  ) {
    elements.sort(compareFunc)
    elements.forEach((el) => {
      // Note: Calling appendChild on this element will reuse
      // the existing element since it's already in the DOM.
      // This means that any attached event handlers will remain.
      parent.appendChild(el)
    })
  }
}

const lastUpdatedAndNameComparatorObject = (el: Element) => {
  const lastUpdatedMillisString =
    el.getAttribute('data-last-updated-millis') || ''
  const lastUpdatedMillis = Number(lastUpdatedMillisString)
  return {
    name: el.getAttribute('data-name') || '',
    lastUpdatedMillis,
  }
}

const lastUpdatedAndNameComparator = (first: Element, second: Element) => {
  const firstComparator = lastUpdatedAndNameComparatorObject(first)
  const secondComparator = lastUpdatedAndNameComparatorObject(second)

  return (
    secondComparator.lastUpdatedMillis - firstComparator.lastUpdatedMillis ||
    firstComparator.name.localeCompare(secondComparator.name)
  )
}

const sortingScriptEl = document.currentScript

window.addEventListener('load', () => {
  for (const parentSelector of [
    '.cf-admin-program-card-list',
    '.cf-admin-question-list',
    '#question-bank-questions',
  ]) {
    const parent = document.querySelector(parentSelector) as HTMLElement | null
    if (parent) {
      ElementSorter.sortElementsOnLoad({
        parent,
        compareFunc: lastUpdatedAndNameComparator,
      })
    }
  }

  // Advertise for browser tests that initialization is done.
  if (sortingScriptEl) {
    sortingScriptEl.setAttribute('data-has-loaded', 'true')
  }
})
