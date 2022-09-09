import {ElementHandle, Frame, Page} from 'playwright'

/**
 * Civiform attaches JS event handlers after pages load, so after any action
 * that loads a new page, browser tests should call this function to wait
 * for pages to be fully operational and ready to test.
 */
export const waitForPageJsLoad = async (page: Page | Frame | null) => {
  if (page == null) {
    throw new Error('waitForPageJsLoad received null!')
  }

  await page.waitForLoadState('load')

  // Wait for main.ts and modal.ts to signal that they're done initializing
  await page.waitForSelector('body[data-load-main="true"]')
  await page.waitForSelector('body[data-load-modal="true"]')

  const hasWaitFor = await page.isVisible('[data-wait-for-scripts]')
  if (hasWaitFor) {
    const waitForScriptsRaw = await page.getAttribute(
      '[data-wait-for-scripts]',
      'data-wait-for-scripts',
    )
    if (!waitForScriptsRaw) {
      throw new Error('Found element but could not retrieve attribute')
    }

    const waitForScriptPromises = Array.from(waitForScriptsRaw.split(',')).map(
      (script) => {
        return page.waitForSelector(`[data-${script}="true"]`)
      },
    )
    await Promise.all(waitForScriptPromises)
  }
}

/**
 * Click on the button to trigger a modal.ts dialog and wait for it to appear.
 * @param modalId ID of the modal dialog without the leading #
 */
export const clickAndWaitForModal = async (page: Page, modalId: string) => {
  await page.click(`#${modalId}-button`)
  await page.waitForSelector(`#${modalId}:not(.hidden)`)
}

/**
 * Waits for any modal to be displayed.
 */
export const waitForAnyModal = async (
  page: Page | Frame,
): Promise<ElementHandle<HTMLElement>> => {
  return (await page.waitForSelector(
    '.cf-modal:not(.hidden)',
  )) as unknown as ElementHandle<HTMLElement>
}

/**
 * Dismisses an open modal.
 */
export const dismissModal = async (page: Page | Frame) => {
  await page.click('.cf-modal:not(.hidden) .cf-modal-close')
}
