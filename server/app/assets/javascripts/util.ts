import MarkdownIt = require('markdown-it')

/** @fileoverview Collection of generic util functions used throughout the
 * codebase.
 */

/**
 * Adds event listener to all elements on a page that match given selector.
 * This function doesn't handle elements added dynamically after the function
 * was invoked.
 *
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
 * See TypeScirpt best practices for recommendations for when to use
 * assertNotNull vs non-null operator `!`:
 * https://docs.civiform.us/contributor-guide/developer-guide/development-standards
 *
 * @param value
 * @param extraInfo Additional info to add to the error if provided value is
 *     null. In most cases it's not needed as stack trace is enough to identify
 *     the location of the error.
 */
export function assertNotNull<T>(
  value: T | null | undefined,
  extraInfo = '',
): T {
  if (value == null) {
    const extra = extraInfo !== '' ? `Extra info: ${extraInfo}` : ''
    throw new Error(`Provided value is ${String(value)}. ${extra}`)
  }
  return value
}

// Setup for formatText function
const parser = new DOMParser()
const md = new MarkdownIt({
  html: true,
  linkify: true,
  breaks: true,
})

/**
 * Parses text with markdown into HTML with some additional styles applied
 *
 * @param {string} text The text to parse into HTML.
 */
export function formatText(text: string): Element {
  // Preserve line breaks before parsing the text
  text = text.split('\n').join('<br>')

  let parsedHtml = md.render(text)
  // Format lists
  parsedHtml = parsedHtml.split('<ul>').join('<ul class="list-disc mx-8">')
  parsedHtml = parsedHtml.split('<ol>').join('<ol class="list-decimal mx-8">')
  // Format links
  parsedHtml = parsedHtml
    .split('href')
    .join(
      'class="text-blue-600 hover:text-blue-500 underline" target="_blank" href',
    )
  // Change h1 to h2 (per accessibility standards, there should only ever be one H1 per page)
  parsedHtml = parsedHtml.split('<h1>').join('<h2>')
  parsedHtml = parsedHtml.split('</h1>').join('</h2>')
  // parsedHtml = parsedHtml.replace('<body>', "")
  // parsedHtml = parsedHtml.replace('</body>', "")

  console.log("parsedHtml", parsedHtml)

  const html = parser.parseFromString(parsedHtml, 'text/html')
  console.log("html", html)
  console.log("html.body", html.body)
  return html.body
}
