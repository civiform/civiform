
/**
 * Adds event listener to all elements on a page that match given selector.
 * This function doesn't handle elements added dynamically after the function was invoked.
 * @param {string} selector CSS selector that will be used to retrieve list of elements.
 * @param {string} event Browser event. For example 'click'
 * @param {Function} listener Listener that will be registered on all matching elements.
 */
export function addEventListenerToElements(
  selector: string,
  event: string,
  listener: (e: Event) => void,
) {
  Array.from(document.querySelectorAll(selector)).forEach((el) =>
    el.addEventListener(event, listener),
  )
}
