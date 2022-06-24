window.addEventListener('load', () => {
  Array.from(document.querySelectorAll('tr')).forEach((rowEl) => {
    const svgEl = rowEl.querySelector('svg')
    if (!svgEl) {
      return
    }
    const bbox = svgEl.getBBox()
    rowEl.querySelectorAll('p')[0].textContent = `${bbox.x + bbox.width}`
    rowEl.querySelectorAll('p')[1].textContent = `${bbox.y + bbox.height}`
  })
})
