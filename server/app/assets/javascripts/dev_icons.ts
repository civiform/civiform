const devIconsScriptEl = document.currentScript

/** This handler is responsible for annotating each rendered SVG icon
 * with its size as calculated by the browser.  */
window.addEventListener('load', () => {
  Array.from(document.querySelectorAll('tr')).forEach((rowEl) => {
    const svgEl = rowEl.querySelector('svg')
    if (!svgEl) {
      return
    }
    const iconWidthEl = rowEl.querySelector('.icon-width')
    const iconHeightEl = rowEl.querySelector('.icon-height')
    const bbox = svgEl.getBBox()
    // when calculating width/height multiple offset (x and y) values by 2 as
    // icons usually centered and there is some empty space on all sides of
    // the icon.
    if (iconWidthEl != null) {
      iconWidthEl.textContent = `${2 * bbox.x + bbox.width}`
    }
    if (iconHeightEl != null) {
      iconHeightEl.textContent = `${2 * bbox.y + bbox.height}`
    }
  })

  // Advertise for browser tests that initialization is done.
  if (devIconsScriptEl) {
    devIconsScriptEl.setAttribute('data-has-loaded', 'true')
  }
})
