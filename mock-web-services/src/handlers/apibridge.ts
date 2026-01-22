import {http, HttpResponse} from 'msw';
import {
  HealthcheckResponse,
  DiscoveryResponse,
  BridgeResponse,
  ProblemDetail,
  ValidationProblemDetail,
} from '@/types/apibridge.js';

// Sample data (from Python sample_data.py)
const requestSchema = {
  $schema: 'https://json-schema.org/draft/2020-12/schema',
  $id: 'https://civiform.us/schemas/request.json',
  title: 'Response title',
  description: 'Request schema description',
  type: 'object',
  properties: {
    accountNumber: {
      type: 'number',
      title: 'Account Number',
      description: 'Account Number',
    },
    zipCode: {
      type: 'string',
      title: 'ZIP Code',
      description: 'ZIP Code description',
    },
  },
  required: ['accountNumber', 'zipCode'],
  additionalProperties: false,
};

const responseSchema = {
  $schema: 'https://json-schema.org/draft/2020-12/schema',
  $id: 'https://civiform.us/schemas/response-schema.json',
  title: 'Response title',
  description: 'Response schema description',
  type: 'object',
  properties: {
    accountNumber: {
      type: 'number',
      title: 'Account Number',
      description: 'Account Number',
    },
    isValid: {
      type: 'boolean',
      title: 'Is Valid',
      description: 'Has valid account',
    },
  },
  required: ['accountNumber', 'isValid'],
  additionalProperties: false,
};

function createProblemDetail(
    status: number,
    requestPath: string,
): ProblemDetail {
  return {
    type: `https://localhost.localdomain/type/${status}`,
    title: `title-${status}`,
    status,
    detail: `detail-${status}`,
    instance: requestPath,
  };
}

function createValidationProblemDetail(
    requestPath: string,
): ValidationProblemDetail {
  return {
    type: 'https://localhost.localdomain/type/422',
    title: 'title-422',
    status: 422,
    detail: 'detail-422',
    instance: requestPath,
    validation_errors: [
      {name: 'accountNumber', message: 'Invalid account number'},
      {name: 'zipCode', message: 'Invalid zip code'},
    ],
  };
}

function unhandled(requestPath: string): ProblemDetail {
  return {
    type: 'https://localhost.localdomain/type/500',
    title: 'unhandled',
    status: 500,
    detail: 'Error in the mock bridge. not a simulated error',
    instance: requestPath,
  };
}

/**
 * GET /api-bridge/health-check
 *
 * API Bridge - Healthcheck endpoint handler
 */
export const getHealthCheck = http.get(
    '*/api-bridge/health-check',
    async ({request}) => {
      try {
        const url = new URL(request.url);
        const responseCode = parseInt(
            request.headers.get('emulate-response-code') || '200',
            10,
        );

        if ([400, 401, 500].includes(responseCode)) {
          const response = createProblemDetail(responseCode, url.pathname);
          return HttpResponse.json(response, {status: responseCode});
        }

        if (responseCode === 429) {
          const response = createProblemDetail(429, url.pathname);
          return HttpResponse.json(response, {
            status: 429,
            headers: {'Retry-After': '1'},
          });
        }

        const response: HealthcheckResponse = {
          timestamp: Math.floor(Date.now() / 1000) + 1000,
        };
        return HttpResponse.json(response, {status: responseCode});
      } catch {
        const url = new URL(request.url);
        const problemDetail = unhandled(url.pathname);
        return HttpResponse.json(problemDetail, {status: 500});
      }
    },
);

/**
 * GET /api-bridge/discovery
 *
 * API Bridge - Discovery endpoint handler
 */
export const getDiscovery = http.get(
    '*/api-bridge/discovery',
    async ({request}) => {
      try {
        const url = new URL(request.url);
        const responseCode = parseInt(
            request.headers.get('emulate-response-code') || '200',
            10,
        );

        if ([400, 401, 500].includes(responseCode)) {
          const response = createProblemDetail(responseCode, url.pathname);
          return HttpResponse.json(response, {status: responseCode});
        }

        if (responseCode === 429) {
          const response = createProblemDetail(429, url.pathname);
          return HttpResponse.json(response, {
            status: 429,
            headers: {'Retry-After': '1'},
          });
        }

        const response: DiscoveryResponse = {
          endpoints: {
            '/bridge/success': {
              compatibility_level: 'v1',
              description: 'Sample endpoint that is successful',
              request_schema: requestSchema,
              response_schema: responseSchema,
            },
          },
        };

        return HttpResponse.json(response, {status: responseCode});
      } catch {
        const url = new URL(request.url);
        const problemDetail = unhandled(url.pathname);
        return HttpResponse.json(problemDetail, {status: 500});
      }
    },
);

/**
 * POST /api-bridge/bridge/:slug
 *
 * :slug - string
 *
 * API Bridge - Bridge endpoint handler
 */
export const postBridge = http.post(
    '*/api-bridge/bridge/:slug',
    async ({request}) => {
      try {
        const url = new URL(request.url);
        const data = (await request.json()) as any;

        const responseCode = parseInt(
            request.headers.get('emulate-response-code') || '200',
            10,
        );

        if ([400, 401, 404, 500].includes(responseCode)) {
          const response = createProblemDetail(responseCode, url.pathname);
          return HttpResponse.json(response, {status: responseCode});
        }

        if (responseCode === 422) {
          const response = createValidationProblemDetail(url.pathname);
          return HttpResponse.json(response, {status: 422});
        }

        if (responseCode === 429) {
          const response = createProblemDetail(429, url.pathname);
          return HttpResponse.json(response, {
            status: 429,
            headers: {'Retry-After': '1'},
          });
        }

        const response: BridgeResponse = {
          compatibility_level: 'v1',
          payload: {
            accountNumber: data.payload?.accountNumber,
            isValid: data.payload?.accountNumber === 1234,
          },
        };

        return HttpResponse.json(response, {status: responseCode});
      } catch {
        const url = new URL(request.url);
        const problemDetail = unhandled(url.pathname);
        return HttpResponse.json(problemDetail, {status: 500});
      }
    },
);

/**
 * All API Bridge endpoint handlers
 */
export const apiBridgeHandlers = [getHealthCheck, getDiscovery, postBridge];
