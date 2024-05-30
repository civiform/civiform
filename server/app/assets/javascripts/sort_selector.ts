/**
 * Responsible for controlling the sorting mechanism used by QuestionBankController and TrustedIntermediaryController.
 * Elements (tagged with elementSelector class) have data attributes with the value to be compared and will sort
 * list using the value. The data attribute is defined in QuestionSortOption.
 *
 * E.g. Sort by Name A-Z will look up the value for data attribute tiname-asc and sort the list by that value.
 *
 * @param listId id for finding the portion of the document that contains the sublistSelector and elementSelector
 * @param sublistSelector finds all of the elements containing to-be-sorted items. Can be multiple elements.
 * @param elementSelector individual elements to be sorted
 */
export function sortElementsByDataAttributes(
  listId: string,
  sublistSelector: string,
  elementSelector: string,
): void {
  const listToBeSorted = document.getElementById(listId) as HTMLSelectElement
  const sublists = document.querySelectorAll(sublistSelector)
  if (!listToBeSorted || !sublists) {
    return
  }

  sublists.forEach((sublist) => {
    const el: HTMLElement[] = Array.from(
      sublist.querySelectorAll(elementSelector),
    )

    const sortedElements = el.sort((elementA, elementB) => {
      // listToBeSorted values is expected to be of the format "<data_attribute_name>-<asc|desc>".
      // Attribute names and order suffix are defined in *SortOption.java.
      const [attrName, order] = listToBeSorted.value.split('-')
      // Get the data attribute whose name matches the selected sort option so that it can be used to compare the elements.
      const attrA: string | null = elementA.getAttribute('data-' + attrName)
      const attrB: string | null = elementB.getAttribute('data-' + attrName)
      if (!attrA || !attrB) {
        return 0
      }

      const compare = function (a: string, b: string): number {
        switch (attrName) {
          case 'lastmodified': {
            const dateA = new Date(a)
            const dateB = new Date(b)
            return dateA.getTime() - dateB.getTime()
          }
          default:
            // Default sort is a string sort.
            return a.localeCompare(b)
        }
      }
      return order == 'asc' ? compare(attrA, attrB) : compare(attrB, attrA)
    })

    sortedElements.forEach((q) => {
      sublist.appendChild(q)
    })
  })
}
