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
    if (iconWidthEl != null) {
      iconWidthEl.textContent = `${bbox.x + bbox.width}`
    }
    if (iconHeightEl != null) {
      iconHeightEl.textContent = `${bbox.y + bbox.height}`
    }
  })
})
