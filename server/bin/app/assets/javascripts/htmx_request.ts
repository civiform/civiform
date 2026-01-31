// htmx does not provide Typescript definitions for its custom events, so we have to define them ourselves
// For more info about fields on an htmx request see https://htmx.org/events/#htmx:configRequest
export interface HtmxRequest {
  formData: FormData
}

// TypeScript interface for htmx:afterSwap event https://htmx.org/events/#htmx:afterSwap
export interface HtmxAfterSwapEvent extends CustomEvent {
  elt: HTMLElement
  xhr: XMLHttpRequest
  target: HTMLElement
  requestConfig: {
    verb: string
    path: string
    headers: Record<string, string>
    parameters: Record<string, string | number | boolean | string[]>
  }
  serverResponse: string
  successful: boolean
  swapStyle: string
  pathInfo: {
    requestPath: string
    finalRequestPath: string
    anchor: string
  }
}
