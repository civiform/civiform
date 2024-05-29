/** Responsible for controlling the sorting mechanism */
export function sortSelectorElements(sortSelectId: string,
    sortElementId: string,
    sortSublistId: string): void {
    const listToBeSorted = document.getElementById(
        sortSelectId,
    ) as HTMLSelectElement
    const sublists = document.querySelectorAll(sortSublistId)
    if (!listToBeSorted || !sublists) {
        return
    }

    sublists.forEach((sublist) => {
        const el: HTMLElement[] = Array.from(
            sublist.querySelectorAll(sortElementId),
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

