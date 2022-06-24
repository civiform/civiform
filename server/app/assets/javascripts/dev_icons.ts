window.addEventListener('load', () => {
  Array.from(document.querySelectorAll('tr')).forEach((rowEl) => {
    const svgEl = rowEl.querySelector('svg')
    if (!svgEl) {
      return
    }
    const bbox = svgEl.getBBox()
    rowEl.querySelector('.icon-width')!.textContent = `${bbox.x + bbox.width}`
    rowEl.querySelector('.icon-height')!.textContent = `${bbox.y + bbox.height}`
  })
})
