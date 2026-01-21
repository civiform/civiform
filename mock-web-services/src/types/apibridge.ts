export type CompatibilityLevel = 'v1'

export type ProblemDetail = {
  type: string
  title: string
  status: number
  detail: string
  instance?: string
}

export type ValidationProblemDetail = ProblemDetail & {
  validation_errors: {
    name: string
    message: string
  }[]
}

export type HealthcheckResponse = {
  timestamp: number
}

export type DiscoveryResponse = {
  endpoints: Record<
    string,
    {
      compatibility_level: CompatibilityLevel
      description: string
      request_schema: Record<string, any>
      response_schema: Record<string, any>
    }
  >
}

export type BridgeResponse = {
  compatibility_level: CompatibilityLevel
  payload: Record<string, any>
}
