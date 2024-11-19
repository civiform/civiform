// htmx does not provide Typescript definitions for its custom events, so we have to define them ourselves
// For more info about fields on an htmx request see https://htmx.org/events/#htmx:configRequest
export interface HtmxRequest {
  formData: FormData
}
