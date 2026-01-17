export enum CompatibilityLevel {
  V1 = 'v1',
}

export interface ProblemDetail {
  type: string
  title: string
  status: number
  detail: string
  instance?: string
}

export interface ValidationError {
  name: string
  message: string
}

export interface ValidationProblemDetail extends ProblemDetail {
  validation_errors: ValidationError[]
}

export interface HealthcheckResponse {
  timestamp: number
}

export interface Endpoint {
  compatibility_level: CompatibilityLevel
  description: string
  request_schema: Record<string, any>
  response_schema: Record<string, any>
}

export interface DiscoveryResponse {
  endpoints: Record<string, Endpoint>
}

export interface BridgeResponse {
  compatibility_level: CompatibilityLevel
  payload: Record<string, any>
}
