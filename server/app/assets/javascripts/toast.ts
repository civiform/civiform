/**
 * This class controls the visibility of toast messages.
 *
 * Functionality includes:
 *  - showing and hiding toast messages and the toast container.
 *  - dismiss a toast messages based on a user action or after a specified timeout.
 *  - permanently dismiss toast messags (using localStorage)
 */
import {assertNotNull} from './util'

export class ToastController {
  private static readonly CONTAINER_ID = 'toast-container'
  private static readonly MESSAGE_CLASS = 'cf-toast'
  private static readonly MESSAGE_DATA_CLASS = 'cf-toast-data'

  private static readonly INFO_SVG_PATH =
    'M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0' +
    ' 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z'
  private static readonly ERROR_SVG_PATH =
    'M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0' +
    ' 102 0V6a1 1 0 00-1-1z'
  private static readonly SUCCESS_SVG_PATH = 'M5 13l4 4L19 7'
  private static readonly WARNING_SVG_PATH =
    'M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742' +
    ' 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012' +
    ' 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z'

  constructor() {
    const toastContainer = document.createElement('div')
    toastContainer.setAttribute('id', ToastController.CONTAINER_ID)
    toastContainer.classList.add(
      'absolute',
      'hidden',
      'left-1/2',
      'top-0',
      'transform',
      '-translate-x-1/2',
      'z-20',
    )
    document.body.appendChild(toastContainer)

    ToastController.maybeShowToasts()
  }

  static showToastMessage(message: ToastMessage) {
    const inIgnoreList = localStorage.getItem(message.id + '-dismissed')
    if (message.canIgnore && inIgnoreList) {
      return
    }

    const toastMessage = document.createElement('div')
    toastMessage.setAttribute('id', message.id)
    toastMessage.setAttribute('ignorable', String(message.canIgnore))
    toastMessage.setAttribute('role', 'alert')
    toastMessage.setAttribute('aria-live', 'polite')
    toastMessage.classList.add(ToastController.MESSAGE_CLASS)
    toastMessage.classList.add(
      'bg-opacity-90',
      'duration-300',
      'flex',
      'flex-row',
      'max-w-md',
      'px-2',
      'py-2',
      'my-3',
      'relative',
      'rounded-sm',
      'shadow-lg',
      'transition-opacity',
      'transform',
      'text-gray-700',
    )

    if (message.type === 'alert') {
      toastMessage.classList.add('bg-gray-200', 'border-gray-300')
    } else if (message.type === 'error') {
      toastMessage.classList.add('bg-red-400', 'border-red-500')
    } else if (message.type === 'success') {
      toastMessage.classList.add('bg-emerald-200', 'border-emerald-300')
    } else if (message.type === 'warning') {
      toastMessage.classList.add('bg-amber-200', 'border-amber-300')
    }

    toastMessage.appendChild(ToastController.getToastIcon(message.type))

    // Add the content string.
    const contentContainer = document.createElement('span')
    contentContainer.textContent = message.content
    toastMessage.appendChild(contentContainer)

    // Maybe add dismiss button.
    if (message.canDismiss) {
      const dismissButton = document.createElement('div')
      dismissButton.setAttribute('id', message.id + '-dismiss')
      dismissButton.classList.add(
        'absolute',
        'font-bold',
        'pl-6',
        'opacity-40',
        'right-4',
        'top-2',
        'cursor-pointer',
        'hover:opacity-100',
      )
      dismissButton.textContent = 'x'
      dismissButton.addEventListener('click', ToastController.dismissClicked)
      toastMessage.appendChild(dismissButton)
      toastMessage.classList.add('pr-8')
    }

    const toastContainer = document.getElementById(ToastController.CONTAINER_ID)
    if (toastContainer) {
      toastContainer.appendChild(toastMessage)
      toastContainer.classList.remove('hidden')
      if (message.duration > 0) {
        setTimeout(
          ToastController.dismissToast,
          message.duration,
          message.id,
          /* dismissClicked = */ false,
        )
      }
    }
  }

  private static getToastIcon(type: string): Element {
    const svgContainer = document.createElement('div')
    svgContainer.classList.add('flex-none', 'pr-2')

    const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg')
    svg.setAttribute('fill', 'currentColor')
    svg.setAttribute('fill-rule', 'evenodd')
    svg.setAttribute('viewBox', '0 0 20 20')
    svg.setAttribute('xmlns', 'http://www.w3.org/2000/svg')
    svg.classList.add('inline-block', 'h-6', 'w-6')
    svgContainer.appendChild(svg)

    const svgPath = document.createElementNS(
      'http://www.w3.org/2000/svg',
      'path',
    )
    svg.appendChild(svgPath)

    if (type === 'alert') {
      svgPath.setAttribute('d', ToastController.INFO_SVG_PATH)
    } else if (type === 'error') {
      svgPath.setAttribute('d', ToastController.ERROR_SVG_PATH)
    } else if (type === 'success') {
      svg.setAttribute('fill', 'none')
      svg.setAttribute('stroke', 'currentColor')
      svg.setAttribute('stroke-width', '2')
      svgPath.setAttribute('d', ToastController.SUCCESS_SVG_PATH)
    } else if (type === 'warning') {
      svgPath.setAttribute('d', ToastController.WARNING_SVG_PATH)
    }
    return svgContainer
  }

  /** If a toast message is present, create it and add it to the container. */
  private static maybeShowToasts() {
    const messages = Array.from(
      document.querySelectorAll('.' + ToastController.MESSAGE_DATA_CLASS),
    )
    messages.forEach((element) => {
      const message: ToastMessage = {
        id: element.id,
        canDismiss: element.getAttribute('canDismiss') === 'true',
        canIgnore: element.getAttribute('canIgnore') === 'true',
        content: assertNotNull(element.textContent),
        duration: Number(element.getAttribute('toastDuration')),
        type: element.getAttribute('toastType')!.toLowerCase(),
      }
      element.remove()
      ToastController.showToastMessage(message)
    })
  }

  /**
   *  Hide warning message and throw an indicator in local storage to not show.
   *  @param {Event} event The event that triggered this action.
   *  */
  private static dismissClicked(event: Event) {
    const target = event.target as Element
    const toast = target.closest('.' + ToastController.MESSAGE_CLASS)
    if (toast && toast.id) {
      const toastId = toast.id
      ToastController.dismissToast(toastId, /* dismissClicked = */ true)
    }
  }

  /**
   * If toastMessage has a storageId and was dismissed by the user (not a timeout)
   * then we add the id to localStorage so that it isn't displayed again.
   * @param {string} toastId The html id of the toast message
   * @param {boolean} dismissClicked Whether to add indicator in local storage to not show
   *  */
  private static dismissToast(toastId: string, dismissClicked: boolean) {
    const toastMessage = document.getElementById(toastId)
    if (toastMessage) {
      if (dismissClicked && toastMessage.getAttribute('ignorable')) {
        localStorage.setItem(toastId + '-dismissed', 'true')
      }

      // Dismiss the toast with the given id.
      toastMessage.classList.add('opacity-0')
      ToastController.cleanupToast(toastId)
    }
  }

  /**
   * Removes a toast from the DOM and hides the toast container if it is now empty.
   * @param {string} toastId The html id of the toast message
   * */
  private static cleanupToast(toastId: string) {
    const toastMessage = document.getElementById(toastId)
    if (toastMessage) {
      // Remove toast message.
      toastMessage.remove()
    }

    /** Hide toast container if there are no toast messages active. */
    const toastContainer = document.getElementById(ToastController.CONTAINER_ID)
    if (toastContainer && toastContainer.children.length == 0) {
      toastContainer.classList.add('hidden')
    }
  }
}

type ToastMessage = {
  id: string
  canDismiss: boolean
  canIgnore: boolean
  content: string
  duration: number
  type: string
}

export function init() {
  new ToastController()
}
