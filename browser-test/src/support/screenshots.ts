import {Page, Locator} from 'playwright'
import {test, expect} from './civiform_fixtures'
import {DISABLE_SCREENSHOTS} from './config'
import * as path from 'path'

import {normalizeElements} from './helpers'

/**
 * Settings for taking screenshots
 */
type ScreenshotSettings = {
  /**
   * When true, takes a screenshot of the full scrollable page, instead of the currently visible viewport. Defaults to
   * `true`.
   */
  fullPage?: boolean
  /** When true, also takes screenshots at screen widths for mobile (320px) and tablet/medium (800px) */
  mobileScreenshot?: boolean
  /**
   * Specify locators that should be masked when the screenshot is taken. Masked elements will be overlaid with a pink
   * box `#FF00FF` (customized by
   * [`maskColor`](https://playwright.dev/docs/api/class-pageassertions#page-assertions-to-have-screenshot-1-option-mask-color))
   * that completely covers its bounding box. The mask is also applied to invisible elements, see
   * [Matching only visible elements](https://playwright.dev/docs/locators#matching-only-visible-elements) to disable that.
   */
  mask?: Array<Locator>

  /**
   * An acceptable ratio of pixels that are different to the total amount of pixels, between `0` and `1`. Default is
   * configurable with `TestConfig.expect`. Unset by default.
   */
  maxDiffPixelRatio?: number
}

/**
 * Saves a screenshot to a file such as
 * browser-test/image_snapshots/test_file_name/{screenshotFileName}-snap.png.
 * If the screenshot already exists, compare the new screenshot with the
 * existing screenshot, and save a pixel diff instead if the two don't match.
 * @param fileName Must use dash-separated-case for consistency.
 */
export const validateScreenshot = async (
  element: Page | Locator,
  fileName: string,
  settings?: ScreenshotSettings,
) => {
  // Do not make image snapshots when running locally
  if (DISABLE_SCREENSHOTS) {
    return
  }

  await test.step(
    'Validate screenshot',
    async () => {
      let fullPage = settings?.fullPage

      if (fullPage === undefined) {
        fullPage = true
      }

      const page = 'page' in element ? element.page() : element
      // Normalize all variable content so that the screenshot is stable.
      await normalizeElements(page)
      // Also process any sub frames.
      for (const frame of page.frames()) {
        await normalizeElements(frame)
      }

      if (fullPage) {
        // Some tests take screenshots while scroll position in the middle. That
        // affects header which is position fixed and on final full-page screenshots
        // overlaps part of the page.
        await page.evaluate(() => {
          window.scrollTo(0, 0)
        })
      }

      expect(fileName).toMatch(/^[a-z0-9-]+$/)

      // Full/desktop width
      await softAssertScreenshot(element, `${fileName}`, fullPage, settings)

      // If we add additional breakpoints the browser-test/src/reporters/file_placement_reporter.ts
      // needs to be updated to handle the additional options.
      if (settings?.mobileScreenshot) {
        const existingWidth = page.viewportSize()?.width || 1280
        const height = page.viewportSize()?.height || 720
        // Mobile width
        await page.setViewportSize({width: 320, height})
        await softAssertScreenshot(
          element,
          `${fileName}-mobile`,
          fullPage,
          settings,
        )

        // Medium width
        await page.setViewportSize({width: 800, height})
        await softAssertScreenshot(
          element,
          `${fileName}-medium`,
          fullPage,
          settings,
        )

        // Reset back to original width
        await page.setViewportSize({width: existingWidth, height})
      }

      // Do a hard assert that we have no errors. This allows us to do soft asserts on the
      // different sized images.
      expect(test.info().errors).toHaveLength(0)
    },
    {
      box: true,
    },
  )
}

const softAssertScreenshot = async (
  element: Page | Locator,
  fileName: string,
  fullPage?: boolean,
  settings?: ScreenshotSettings,
) => {
  const testFileName = path
    .basename(test.info().file)
    .replace('.test.ts', '_test')

  await expect
    .soft(element)
    .toHaveScreenshot([testFileName, fileName + '.png'], {
      fullPage: fullPage,
      mask: settings?.mask,
      maxDiffPixelRatio: settings?.maxDiffPixelRatio,
    })
}
