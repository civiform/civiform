/** @fileoverview Collection of generic util functions used throughout the
 * codebase.
 */

/**
 * Adds event listener to all elements on a page that match given selector.
 * This function doesn't handle elements added dynamically after the function was invoked.
 * @param selector CSS selector that will be used to retrieve list of elements.
 * @param event Browser event. For example 'click'
 * @param listener Listener that will be registered on all matching elements.
 */
export function addEventListenerToElements<K extends keyof HTMLElementEventMap>(
  selector: string,
  event: K,
  listener: (ev: HTMLElementEventMap[K]) => void,
) {
  Array.from(document.querySelectorAll<HTMLElement>(selector)).forEach((el) =>
    el.addEventListener(event, listener),
  )
}

/**
 * Asserts that passed value is not null. This method is used when working with
 * various APIs that often return nullable values and often developers know
 * that value is not-null. For example using document.querySelector on elements
 * that 100% should be there. This function helps to assert that value is not
 * null or fail quickly if those expectations are false.
 *
 * This is preferred way of asserting values over using assertion `value!` as
 * latter doesn't throw error immediately and instead value can be dereferenced later in the code flow making root cause of the error harder to track.
 *
 * @param value
 * @param extraInfo Additional info to add to the error if provided value is null. In most cases it's not needed as stack trace is enough to identify the location of the error.
 */
export function assert<T>(value: T | null | undefined, extraInfo = ''): T {
  if (value == null) {
    const extra = extraInfo !== '' ? ' Extra info: ' + extraInfo : ''
    throw new Error(`Provided value is ${String(value)}.${extra}`)
  }
  return value
}
