// This file is here to help webpack correctly inject HTMX >= v2.0
import htmx from 'htmx.org'

// @ts-expect-error Extra noise because of linting rules thinking this is wrong, but it is not.
window.htmx = htmx

export default htmx
