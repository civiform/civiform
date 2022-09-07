/**
 * ElementSorter provides utilities for sorting a group of objects. If a placeholder element is
 * provided, it will be hidden after the elements have been sorted.
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

window.addEventListener('load', () => {
  const programListParent = document.querySelector(
    '.cf-admin-program-card-list',
  ) as HTMLElement | null
  if (programListParent) {
    ElementSorter.sortElementsOnLoad({
      parent: programListParent,
      compareFunc: lastUpdatedAndNameComparator,
    })
  }

  const questionListParent = document.querySelector(
    '.cf-admin-question-list',
  ) as HTMLElement | null
  if (questionListParent) {
    ElementSorter.sortElementsOnLoad({
      parent: questionListParent,
      compareFunc: lastUpdatedAndNameComparator,
    })
  }

  // Question bank.
  const questionBankParent = document.getElementById('question-bank-questions')
  if (questionBankParent) {
    ElementSorter.sortElementsOnLoad({
      parent: questionBankParent,
      compareFunc: lastUpdatedAndNameComparator,
    })
  }
})
