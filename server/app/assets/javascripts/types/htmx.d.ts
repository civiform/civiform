// TypeScript definitions for htmx custom events
//
// Reference: https://htmx.org/events/

export interface HtmxRequestConfig {
  verb: string
  path: string
  headers: Record<string, string>
  parameters: Record<string, string | number | boolean | string[]>
}

export interface HtmxEventDetail {
  elt: HTMLElement
}

interface HtmxRequestEventDetail extends HtmxEventDetail {
  xhr: XMLHttpRequest
  target: HTMLElement
  requestConfig: HtmxRequestConfig
}

// https://htmx.org/events/#htmx:beforeRequest
export type HtmxBeforeRequestDetail = HtmxRequestEventDetail

// https://htmx.org/events/#htmx:afterSettle
export type HtmxAfterSettleDetail = HtmxRequestEventDetail

// https://htmx.org/events/#htmx:responseError
export type HtmxResponseErrorDetail = HtmxRequestEventDetail

// https://htmx.org/events/#htmx:configRequest
export interface HtmxConfigRequestDetail extends HtmxEventDetail {
  formData: FormData
  parameters: Record<string, unknown>
  headers: Record<string, string>
  target: HTMLElement
  verb: string
  path: string
}

// https://htmx.org/events/#htmx:afterRequest
export interface HtmxAfterRequestDetail extends HtmxRequestEventDetail {
  successful: boolean
  failed: boolean
}

// https://htmx.org/events/#htmx:afterSwap
export interface HtmxAfterSwapDetail extends HtmxRequestEventDetail {
  serverResponse: string
  successful: boolean
  swapStyle: string
  pathInfo: {
    requestPath: string
    finalRequestPath: string
    anchor: string
  }
}

export type HtmxEvent<T> = CustomEvent<T>

export type HtmxConfigRequestEvent = HtmxEvent<HtmxConfigRequestDetail>
export type HtmxBeforeRequestEvent = HtmxEvent<HtmxBeforeRequestDetail>
export type HtmxAfterRequestEvent = HtmxEvent<HtmxAfterRequestDetail>
export type HtmxAfterSwapEvent = HtmxEvent<HtmxAfterSwapDetail>
export type HtmxAfterSettleEvent = HtmxEvent<HtmxAfterSettleDetail>
export type HtmxResponseErrorEvent = HtmxEvent<HtmxResponseErrorDetail>

declare global {
  interface GlobalEventHandlersEventMap {
    'htmx:configRequest': HtmxConfigRequestEvent
    'htmx:beforeRequest': HtmxBeforeRequestEvent
    'htmx:afterRequest': HtmxAfterRequestEvent
    'htmx:afterSwap': HtmxAfterSwapEvent
    'htmx:afterSettle': HtmxAfterSettleEvent
    'htmx:responseError': HtmxResponseErrorEvent
  }
}
