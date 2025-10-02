import {
  CF_LOCATIONS_LIST_CONTAINER,
  CF_LOCATION_HIDDEN,
  DATA_MAP_ID,
  getVisibleCheckboxes,
  mapQuerySelector,
  queryLocationCheckboxes,
} from './map_util'

const ITEMS_PER_PAGE = 6
const MAX_VISIBLE_PAGE_BUTTONS = 3
const DATA_CURRENT_PAGE_ATTRIBUTE = 'data-current-page'
const CF_PAGINATION_BUTTON_TEMPLATE_SELECTOR = '.cf-pagination-button-template'
const CF_PAGINATION_OVERFLOW_TEMPLATE_SELECTOR = '.cf-pagination-overflow-template'

interface PaginationState {
  currentPage: number
  totalPages: number
  totalItems: number
  visibleItems: number
}

export const initPagination = (mapId: string): void => {
  updatePagination(mapId)
  setupPaginationEventListeners(mapId)
}

export const updatePagination = (mapId: string): void => {
  const state = getPaginationState(mapId)
  renderPaginationButtons(mapId, state)
  updateVisibleLocations(mapId, state)
  updatePaginationButtonStates(mapId, state)
}

const getPaginationState = (mapId: string): PaginationState => {
  const locationsContainer = mapQuerySelector(
    mapId,
    CF_LOCATIONS_LIST_CONTAINER,
  ) as HTMLElement | null

  if (!locationsContainer) {
    return {currentPage: 1, totalPages: 1, totalItems: 0, visibleItems: 0}
  }

  const totalItems = queryLocationCheckboxes(mapId).length
  const visibleItems = getVisibleCheckboxes(mapId).length
  const totalPages = Math.max(1, Math.ceil(visibleItems / ITEMS_PER_PAGE))

  const paginationNav = getPaginationNavComponent(mapId)
  // Get current page from pagination nav data attribute, default to 1
  const currentPage = Math.min(
    parseInt(paginationNav?.getAttribute(DATA_CURRENT_PAGE_ATTRIBUTE) || '1', 10),
    totalPages,
  )

  return {
    currentPage,
    totalPages,
    totalItems,
    visibleItems,
  }
}

const updateVisibleLocations = (
  mapId: string,
  state: PaginationState,
): void => {
  const locationsContainer = mapQuerySelector(
    mapId,
    CF_LOCATIONS_LIST_CONTAINER,
  ) as HTMLElement | null

  if (!locationsContainer) return

  const allCheckboxes = Array.from(queryLocationCheckboxes(mapId))
  const visibleCheckboxes = getVisibleCheckboxes(mapId)

  const startIndex = (state.currentPage - 1) * ITEMS_PER_PAGE
  const endIndex = startIndex + ITEMS_PER_PAGE

  // Hide all checkboxes first, but preserve the CF_LOCATION_HIDDEN class for filtered items
  allCheckboxes.forEach((checkbox) => {
    const checkboxElement = checkbox as HTMLElement
    checkboxElement.style.display = 'none'
  })

  // Show only the checkboxes for the current page
  visibleCheckboxes.slice(startIndex, endIndex).forEach((checkbox) => {
    const checkboxElement = checkbox as HTMLElement
    checkboxElement.style.display = ''
  })
}

const renderPaginationButtons = (
  mapId: string,
  state: PaginationState,
): void => {
  const paginationNav = getPaginationNavComponent(mapId)
  if (!paginationNav) return

  const paginationList = paginationNav.querySelector('.usa-pagination__list')
  if (!paginationList) return

  // Update current page data attribute
  paginationNav.setAttribute(DATA_CURRENT_PAGE_ATTRIBUTE, state.currentPage.toString())

  // Clear existing page buttons (keep prev/next arrows)
  const existingPageButtons = paginationList.querySelectorAll('.cf-pagination-item')
  existingPageButtons.forEach((button) => button.remove())

  if (state.totalPages === 1) {
    // Don't show any page buttons if there's only one page
    return
  }

  // Calculate which page numbers to show
  const pageNumbers = calculateVisiblePages(
    state.currentPage,
    state.totalPages,
  )

  // Insert page buttons before the "next" arrow
  const nextArrow = paginationList.querySelector(
    '.usa-pagination__arrow:last-child',
  )

  pageNumbers.forEach((pageNum) => {
    const buttonElement =
      pageNum === '...'
        ? createOverflowElement(mapId)
        : createPageButton(mapId, pageNum as number, state.currentPage)

    if (nextArrow) {
      paginationList.insertBefore(buttonElement, nextArrow)
    } else {
      paginationList.appendChild(buttonElement)
    }
  })
}

const calculateVisiblePages = (
  currentPage: number,
  totalPages: number,
): (number | string)[] => {
  if (totalPages <= MAX_VISIBLE_PAGE_BUTTONS) {
    // Show all pages if we have fewer than max
    return Array.from({length: totalPages}, (_, i) => i + 1)
  }

  const pages: (number | string)[] = []
  const halfVisible = Math.floor(MAX_VISIBLE_PAGE_BUTTONS / 2)

  let startPage = Math.max(1, currentPage - halfVisible)
  let endPage = Math.min(totalPages, startPage + MAX_VISIBLE_PAGE_BUTTONS - 1)

  // Adjust start if we're near the end
  if (endPage - startPage + 1 < MAX_VISIBLE_PAGE_BUTTONS) {
    startPage = Math.max(1, endPage - MAX_VISIBLE_PAGE_BUTTONS + 1)
  }

  // Add overflow before if needed
  if (startPage > 1) {
    pages.push('...')
  }

  // Add visible page numbers
  for (let i = startPage; i <= endPage; i++) {
    pages.push(i)
  }

  // Add overflow after if needed
  if (endPage < totalPages) {
    pages.push('...')
  }

  return pages
}

const createPageButton = (
  mapId: string,
  pageNumber: number,
  currentPage: number,
): HTMLLIElement => {
  const template = getPaginationButtonTemplate(mapId)
  const li = template.cloneNode(true) as HTMLLIElement

  const link = li.querySelector('.usa-pagination__button')!
  link.textContent = pageNumber.toString()
  link.setAttribute('aria-label', `Page ${pageNumber}`)
  link.setAttribute('data-page', pageNumber.toString())
  link.setAttribute(DATA_MAP_ID, mapId)

  if (pageNumber === currentPage) {
    link.classList.add('usa-current')
    link.setAttribute('aria-current', 'page')
  } else {
    link.classList.remove('usa-current')
    link.removeAttribute('aria-current')
  }

  return li
}

const createOverflowElement = (mapId: string): HTMLLIElement => {
  const template = getPaginationOverflowTemplate(mapId)
  return template.cloneNode(true) as HTMLLIElement
}

const getPaginationButtonTemplate = (mapId: string): HTMLLIElement => {
  const template = document.querySelector(
    `${CF_PAGINATION_BUTTON_TEMPLATE_SELECTOR}[${DATA_MAP_ID}="${mapId}"]`,
  ) as HTMLTemplateElement
  return template.content.querySelector('.usa-pagination__page-no')!
}

const getPaginationOverflowTemplate = (mapId: string): HTMLLIElement => {
  const template = document.querySelector(
    `${CF_PAGINATION_OVERFLOW_TEMPLATE_SELECTOR}[${DATA_MAP_ID}="${mapId}"]`,
  ) as HTMLTemplateElement
  return template.content.querySelector('.usa-pagination__overflow')!
}

const updatePaginationButtonStates = (
  mapId: string,
  state: PaginationState,
): void => {
  const paginationNav = getPaginationNavComponent(mapId)
  if (!paginationNav) return

  const prevButton = paginationNav.querySelector(
    '.usa-pagination__previous-page',
  ) as HTMLElement
  const nextButton = paginationNav.querySelector(
    '.usa-pagination__next-page',
  ) as HTMLElement

  // Hide/show previous button
  if (prevButton) {
    const prevLi = prevButton.closest('li')
    if (prevLi) {
      prevLi.style.display = state.currentPage === 1 ? 'none' : ''
    }
  }

  // Hide/show next button
  if (nextButton) {
    const nextLi = nextButton.closest('li')
    if (nextLi) {
      nextLi.style.display =
        state.currentPage === state.totalPages ? 'none' : ''
    }
  }
}

const setupPaginationEventListeners = (mapId: string): void => {
  const paginationNav = getPaginationNavComponent(mapId)
  if (!paginationNav) return

  // Delegate click events for pagination buttons
  paginationNav.addEventListener('click', (e) => {
    const target = e.target as HTMLElement
    if (!target) return

    // Handle page number clicks
    if (target.classList.contains('usa-pagination__button')) {
      const page = parseInt(target.getAttribute('data-page') || '1', 10)
      goToPage(mapId, page)
    }

    // Handle previous button
    if (target.classList.contains('usa-pagination__previous-page') ||
        target.closest('.usa-pagination__previous-page')) {
      const state = getPaginationState(mapId)
      if (state.currentPage > 1) {
        goToPage(mapId, state.currentPage - 1)
      }
    }

    // Handle next button
    if (target.classList.contains('usa-pagination__next-page') ||
        target.closest('.usa-pagination__next-page')) {
      const state = getPaginationState(mapId)
      if (state.currentPage < state.totalPages) {
        goToPage(mapId, state.currentPage + 1)
      }
    }
  })
}

const goToPage = (mapId: string, page: number): void => {
  const paginationNav = getPaginationNavComponent(mapId)
  if (!paginationNav) return

  paginationNav.setAttribute(DATA_CURRENT_PAGE_ATTRIBUTE, page.toString())
  updatePagination(mapId)

  // Scroll to top of locations list
  const locationsContainer = mapQuerySelector(
    mapId,
    CF_LOCATIONS_LIST_CONTAINER,
  ) as HTMLElement | null
  if (locationsContainer) {
    locationsContainer.scrollTop = 0
  }
}

const getPaginationNavComponent = (mapId: string) => {
  return document.querySelector(
    `.cf-pagination[data-map-id="${mapId}"]`,
  )
}