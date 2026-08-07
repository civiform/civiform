import {assertNotNull} from '@/util'

declare const SwaggerUIBundle: {
  (config: {
    url: string
    dom_id: string
    validatorUrl: null
    presets: unknown[]
    layout: string
    plugins: unknown[]
    supportedSubmitMethods: string[]
    responseInterceptor: (response: SwaggerResponse) => SwaggerResponse
  }): void
  presets: {
    apis: unknown
  }
}
declare const SwaggerUIStandalonePreset: unknown

interface SwaggerResponse {
  status: number
  [key: string]: unknown
}

function init(): void {
  // The swagger-ui library does not expose a way specify the theme as light or dark. They
  // added native dark mode at some point and it is set by the users browser/os settings
  // and looking at the prefers-color-scheme option in their css. Because the entire site
  // does not have a dark mode it makes the rendered dark mode version of swagger-ui look
  // very out of place. There was no other way to force it into light mode other than
  // overriding `window.matchMedia`. Give this is a low touch page and the override is
  // only on this page it is a suitable solution until the library provides a direct
  // option to configure it.
  const _matchMedia = window.matchMedia.bind(window)
  window.matchMedia = function (query) {
    if (query && query.includes('prefers-color-scheme')) {
      return {
        matches: false,
        media: query,
        onchange: null,
        addListener: () => {},
        removeListener: () => {},
        addEventListener: () => {},
        removeEventListener: () => {},
        dispatchEvent: () => false,
      }
    }
    return _matchMedia(query)
  }

  SwaggerUIBundle({
    url: document.querySelector<HTMLInputElement>('#api-url')?.value || '',
    dom_id: '#swagger-ui',
    validatorUrl: null,
    presets: [SwaggerUIBundle.presets.apis, SwaggerUIStandalonePreset],
    layout: 'StandaloneLayout',
    supportedSubmitMethods: [],
    plugins: [
      () => ({
        wrapComponents: {
          authorizeBtn: () => () => null,
          authorizeOperationBtn: () => () => null,
        },
      }),
    ],
    responseInterceptor: (response: SwaggerResponse) => {
      if (response.status === 404) {
        const uiError = assertNotNull(
          document.querySelector('.swagger-ui.swagger-container'),
        )
        uiError.setAttribute('data-testid', 'ui-error')

        const wrapper = document.createElement('div')
        wrapper.style.cssText =
          'display: flex; justify-content: center; margin: 4rem; font-size: 1.6rem;'
        wrapper.textContent = 'A program with this status could not be found.'

        uiError.replaceChildren(wrapper)
      }
      return response
    },
  })

  const submitForm = () =>
    document.querySelector<HTMLFormElement>('#selectionForm')?.submit()

  document.getElementById('programSlug')?.addEventListener('change', submitForm)
  document.getElementById('stage')?.addEventListener('change', submitForm)
  document
    .getElementById('openApiVersion')
    ?.addEventListener('change', submitForm)
}

if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', init)
} else {
  init()
}
