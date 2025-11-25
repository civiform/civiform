import {
  CF_LOCATIONS_LIST_CONTAINER,
  DATA_MAP_ID,
  getVisibleCheckboxes,
  localizeString,
  mapQuerySelector,
  queryLocationCheckboxes,
  CF_PAGINATION_HIDDEN,
  getMessages,
} from './map_util'

const ITEMS_PER_PAGE = 6
const MAX_VISIBLE_PAGE_BUTTONS_MOBILE = 3
const MAX_VISIBLE_PAGE_BUTTONS_DESKTOP = 1
const DATA_CURRENT_PAGE_ATTRIBUTE = 'data-current-page'
const CF_PAGINATION_BUTTON_TEMPLATE_SELECTOR = '.cf-pagination-button-template'
const CF_PAGINATION_OVERFLOW_TEMPLATE_SELECTOR =
  '.cf-pagination-overflow-template'
const CF_PAGINATION_ITEM_SELECTOR = '.cf-pagination-item'
const CF_PAGINATION_LIST_SELECTOR = '.cf-pagination-list'
const CF_PAGINATION_STATUS_SELECTOR = '.cf-pagination-status'
const USA_CURRENT_CLASS = 'usa-current'
export const CF_MAP_QUESTION_PAGINATION_BUTTON =
  'cf-map-question-pagination-button'
export const CF_MAP_QUESTION_PAGINATION_NEXT_BUTTON =
  'cf-map-question-pagination-next-button'
export const CF_MAP_QUESTION_PAGINATION_PREVIOUS_BUTTON =
  'cf-map-question-pagination-previous-button'
export const DATA_PAGE_ATTRIBUTE = 'data-page'

interface PaginationState {
  currentPage: number
  totalPages: number
  totalItems: number
  visibleItems: number
}

export const initPagination = (mapId: string): void => {
  const paginationNav = getPaginationNavComponent(mapId)
  if (!paginationNav) return
  updatePagination(mapId, paginationNav)
  window.addEventListener(
    'resize',
    function () {
      updatePagination(mapId, paginationNav)
    },
    true,
  )
}

export const updatePagination = (
  mapId: string,
  paginationNav: Element,
): void => {
  const state = getPaginationState(mapId, paginationNav)
  renderPaginationButtons(mapId, state, paginationNav)
  updateVisibleLocations(mapId, state)
  updatePaginationButtonStates(state, paginationNav)
}

export const resetPagination = (mapId: string): void => {
  const paginationNav = getPaginationNavComponent(mapId)
  if (!paginationNav) return
  paginationNav.setAttribute(DATA_CURRENT_PAGE_ATTRIBUTE, '1')
  updatePagination(mapId, paginationNav)
}

export const getPaginationState = (
  mapId: string,
  paginationNav: Element,
): PaginationState => {
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

  const currentPage = Math.min(
    parseInt(
      paginationNav?.getAttribute(DATA_CURRENT_PAGE_ATTRIBUTE) || '1',
      10,
    ),
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
  const allCheckboxes = Array.from(queryLocationCheckboxes(mapId))
  const visibleCheckboxes = getVisibleCheckboxes(mapId)

  const startIndex = (state.currentPage - 1) * ITEMS_PER_PAGE
  const endIndex = startIndex + ITEMS_PER_PAGE

  // Hide all checkboxes first, but preserve the CF_FILTER_HIDDEN class for filtered items
  allCheckboxes.forEach((checkbox) =>
    checkbox.classList.add(CF_PAGINATION_HIDDEN),
  )

  // Show only the checkboxes for the current page
  visibleCheckboxes
    .slice(startIndex, endIndex)
    .forEach((checkbox) => checkbox.classList.remove(CF_PAGINATION_HIDDEN))
}

const renderPaginationButtons = (
  mapId: string,
  state: PaginationState,
  paginationNav: Element,
): void => {
  const paginationList = paginationNav.querySelector(
    CF_PAGINATION_LIST_SELECTOR,
  )
  if (!paginationList) return

  // Update current page data attribute
  paginationNav.setAttribute(
    DATA_CURRENT_PAGE_ATTRIBUTE,
    state.currentPage.toString(),
  )

  // Clear existing page buttons (keep prev/next arrows)
  const existingPageButtons = paginationList.querySelectorAll(
    CF_PAGINATION_ITEM_SELECTOR,
  )
  existingPageButtons.forEach((button) => button.remove())

  if (state.totalPages === 1) {
    // Don't show any page buttons if there's only one page
    return
  }

  const pageNumbers = calculateVisiblePages(state.currentPage, state.totalPages)

  const nextArrow = paginationList.querySelector(
    `.${CF_MAP_QUESTION_PAGINATION_NEXT_BUTTON}`,
  )

  pageNumbers.forEach((pageNum) => {
    const paginationButtonElement =
      pageNum === '...'
        ? createOverflowElement(mapId)
        : createPageButton(mapId, pageNum as number, state.currentPage)

    if (nextArrow) {
      paginationList.insertBefore(paginationButtonElement, nextArrow)
    } else {
      paginationList.appendChild(paginationButtonElement)
    }
  })
}

const calculateVisiblePages = (
  currentPage: number,
  totalPages: number,
): (number | string)[] => {
  const maxVisiblePageButtons =
    window.innerWidth < 640
      ? MAX_VISIBLE_PAGE_BUTTONS_MOBILE
      : MAX_VISIBLE_PAGE_BUTTONS_DESKTOP
  // Show all pages if we have fewer than max + first/last
  if (totalPages <= maxVisiblePageButtons + 2) {
    return Array.from({length: totalPages}, (_, i) => i + 1)
  }

  const pages: (number | string)[] = []
  const halfVisible = Math.floor(maxVisiblePageButtons / 2)

  let startPage = Math.max(2, currentPage - halfVisible)
  const endPage = Math.min(
    totalPages - 1,
    startPage + maxVisiblePageButtons - 1,
  )

  // Adjust start if we're near the end
  if (endPage - startPage + 1 < maxVisiblePageButtons) {
    startPage = Math.max(2, endPage - maxVisiblePageButtons + 1)
  }

  // Always show first page
  pages.push(1)

  // Add overflow before if needed
  if (startPage > 2) {
    pages.push('...')
  }

  // Add visible page numbers (excluding first and last)
  for (let i = startPage; i <= endPage; i++) {
    pages.push(i)
  }

  // Add overflow after if needed
  if (endPage < totalPages - 1) {
    pages.push('...')
  }

  // Always show last page
  pages.push(totalPages)

  return pages
}

const createPageButton = (
  mapId: string,
  pageNumber: number,
  currentPage: number,
): HTMLLIElement => {
  const template = getPaginationButtonTemplate(mapId)
  const li = template.cloneNode(true) as HTMLLIElement

  const link = li.querySelector(`.${CF_MAP_QUESTION_PAGINATION_BUTTON}`)!
  link.textContent = pageNumber.toString()
  link.setAttribute(
    'aria-label',
    localizeString(getMessages().goToPage, [pageNumber.toString()]),
  )
  link.setAttribute(DATA_PAGE_ATTRIBUTE, pageNumber.toString())
  link.setAttribute(DATA_MAP_ID, mapId)

  if (pageNumber === currentPage) {
    link.classList.add(USA_CURRENT_CLASS)
    link.setAttribute('aria-current', 'page')
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
  return template.content.querySelector(CF_PAGINATION_ITEM_SELECTOR)!
}

const getPaginationOverflowTemplate = (mapId: string): HTMLLIElement => {
  const template = document.querySelector(
    `${CF_PAGINATION_OVERFLOW_TEMPLATE_SELECTOR}[${DATA_MAP_ID}="${mapId}"]`,
  ) as HTMLTemplateElement
  return template.content.querySelector(CF_PAGINATION_ITEM_SELECTOR)!
}

const updatePaginationButtonStates = (
  state: PaginationState,
  paginationNav: Element,
): void => {
  const prevButton = paginationNav.querySelector(
    `.${CF_MAP_QUESTION_PAGINATION_PREVIOUS_BUTTON}`,
  ) as HTMLElement
  const nextButton = paginationNav.querySelector(
    `.${CF_MAP_QUESTION_PAGINATION_NEXT_BUTTON}`,
  ) as HTMLElement

  // Hide/show previous button
  if (prevButton) {
    prevButton.style.display = state.currentPage === 1 ? 'none' : ''
  }

  // Hide/show next button
  if (nextButton) {
    nextButton.style.display =
      state.currentPage === state.totalPages ? 'none' : ''
  }
}

const updatePaginationStatus = (
  mapId: string,
  paginationNav: Element,
): void => {
  const updatedState = getPaginationState(mapId, paginationNav)
  const statusElement = paginationNav.querySelector(
    CF_PAGINATION_STATUS_SELECTOR,
  ) as HTMLElement
  if (statusElement) {
    statusElement.textContent = localizeString(getMessages().paginationStatus, [
      updatedState.currentPage.toString(),
      updatedState.totalPages.toString(),
    ])
    // Clear the text after announcement to prevent navigation to it
    setTimeout(() => {
      statusElement.textContent = ''
    }, 1000)
  }
}

export const goToPage = (mapId: string, page: number): void => {
  const paginationNav = getPaginationNavComponent(mapId)
  if (!paginationNav) return

  const state = getPaginationState(mapId, paginationNav)
  const validPage = Math.max(1, Math.min(page, state.totalPages))

  paginationNav.setAttribute(DATA_CURRENT_PAGE_ATTRIBUTE, validPage.toString())
  updatePagination(mapId, paginationNav)
  updatePaginationStatus(mapId, paginationNav)
}

export const getPaginationNavComponent = (mapId: string) => {
  return document.querySelector(`.cf-pagination[data-map-id="${mapId}"]`)
}
