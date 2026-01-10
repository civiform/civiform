import {http, HttpResponse} from 'msw';
import {
  HealthcheckResponse,
  DiscoveryResponse,
  BridgeResponse,
  Endpoint,
  CompatibilityLevel,
  ProblemDetail,
  ValidationProblemDetail,
} from '../types/apibridge.js';

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
  required: ['accountNumber, isValid'],
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

export const apiBridgeHandlers = [
  // GET /api-bridge/health-check
  http.get('*/api-bridge/health-check', async ({request}) => {
    try {
      const url = new URL(request.url);
      const responseCode = parseInt(
          request.headers.get('emulate-response-code') || '200',
          10,
      );

      if ([400, 401, 500].includes(responseCode)) {
        const response = createProblemDetail(responseCode, url.pathname);
        console.log(JSON.stringify(response));
        return HttpResponse.json(response, {status: responseCode});
      }

      if (responseCode === 429) {
        const response = createProblemDetail(429, url.pathname);
        console.log(JSON.stringify(response));
        return HttpResponse.json(response, {
          status: 429,
          headers: {'Retry-After': '1'},
        });
      }

      const response: HealthcheckResponse = {
        timestamp: Math.floor(Date.now() / 1000) + 1000,
      };
      console.log(JSON.stringify(response));
      return HttpResponse.json(response, {status: responseCode});
    } catch (error) {
      console.error(error);
      const url = new URL(request.url);
      const problemDetail = unhandled(url.pathname);
      return HttpResponse.json(problemDetail, {status: 500});
    }
  }),

  // GET /api-bridge/discovery
  http.get('*/api-bridge/discovery', async ({request}) => {
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

      const endpoint: Endpoint = {
        compatibility_level: CompatibilityLevel.V1,
        description: 'Sample endpoint that is successful',
        request_schema: requestSchema,
        response_schema: responseSchema,
      };

      const response: DiscoveryResponse = {
        endpoints: {
          '/bridge/success': endpoint,
        },
      };

      return HttpResponse.json(response, {status: responseCode});
    } catch (error) {
      console.error(error);
      const url = new URL(request.url);
      const problemDetail = unhandled(url.pathname);
      return HttpResponse.json(problemDetail, {status: 500});
    }
  }),

  // POST /api-bridge/bridge/:slug
  http.post('*/api-bridge/bridge/:slug', async ({request, params}) => {
    try {
      const url = new URL(request.url);
      const {slug} = params;
      const data = (await request.json()) as any;

      console.log(`Slug: ${slug}`);
      console.log(JSON.stringify(data));

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
        compatibility_level: CompatibilityLevel.V1,
        payload: {
          accountNumber: data.payload?.accountNumber,
          isValid: data.payload?.accountNumber === 1234,
        },
      };

      return HttpResponse.json(response, {status: responseCode});
    } catch (error) {
      console.error(error);
      const url = new URL(request.url);
      const problemDetail = unhandled(url.pathname);
      return HttpResponse.json(problemDetail, {status: 500});
    }
  }),
];
