import json
from enum import Enum
from typing import Any, Dict, List, Optional, TypedDict

from webservice import WebService


class Status(str, Enum):
    OK = "ok"
    INTERNAL_ERROR = "internal-error"
    EXTERNAL_ERROR = "external-error"


class CompatibilityLevel(str, Enum):
    V1 = "v1"


class ValidationError(TypedDict):
    name: str
    message: str


class ProblemDetail(TypedDict):
    type: str
    title: Status
    status: int
    detail: str
    instance: Optional[str]


class ValidationProblemDetail(ProblemDetail):
    validation_errors: List[ValidationError]


class HealthcheckResponse(TypedDict):
    status: Status
    timestamp: int


class Endpoint(TypedDict):
    compatibility_level: CompatibilityLevel
    description: str
    uri: str
    request_schema: Dict[str, Any]
    response_schema: Dict[str, Any]


class DiscoveryResponse(TypedDict):
    endpoints: Dict[str, Endpoint]


class BridgeRequest(TypedDict):
    payload: Dict[str, Any]


class BridgeResponse(TypedDict):
    compatibility_level: CompatibilityLevel
    payload: Dict[str, Any]


class ApiBridgeMockWebService(WebService):
    """
    Mock service for testing the API Bridge
    """

    def __init__(self, app):
        WebService.__init__(self, app)
        self.app = app

    def healthcheck(self) -> str:
        """Health check endpoint to verify service status"""
        response: HealthcheckResponse = {
            "status": Status.OK,
            "timestamp": 1742405135,
        }

        return self.return_json_response(json.dumps(response))

    def discovery(self) -> str:
        """Discovery endpoint that provides information about available service endpoints"""

        response: DiscoveryResponse = {
            "endpoints":
                {
                    "/bridge/success":
                        {
                            "compatibility_level": CompatibilityLevel.V1,
                            "description": "Sample endpoint that is successful",
                            "uri": "/bridge/success",
                            "request_schema":
                                {
                                    "$schema":
                                        "https://json-schema.org/draft/2020-12/schema",
                                    "$id":
                                        "https://civiform.us/schemas/request.json",
                                    "title":
                                        "Response title",
                                    "description":
                                        "Request schema description",
                                    "type":
                                        "object",
                                    "properties":
                                        {
                                            "accountNumber":
                                                {
                                                    "type":
                                                        "number",
                                                    "description":
                                                        "Account Number",
                                                },
                                            "zipCode":
                                                {
                                                    "type": "string",
                                                    "description": "Zip Code"
                                                },
                                        },
                                    "required": [
                                        "accountNumber",
                                        "zipCode",
                                    ],
                                    "additionalProperties":
                                        False,
                                },
                            "response_schema":
                                {
                                    "$schema":
                                        "https://json-schema.org/draft/2020-12/schema",
                                    "$id":
                                        "https://civiform.us/schemas/response-schema.json",
                                    "title":
                                        "Response title",
                                    "description":
                                        "Response schema description",
                                    "type":
                                        "object",
                                    "properties":
                                        {
                                            "accountNumber":
                                                {
                                                    "type":
                                                        "number",
                                                    "description":
                                                        "Account Number",
                                                },
                                            "isValid":
                                                {
                                                    "type":
                                                        "boolean",
                                                    "description":
                                                        "Has valid account",
                                                },
                                        },
                                    "required": ["accountNumber, isValid"],
                                    "additionalProperties":
                                        False,
                                },
                        },
                    "/bridge/fail-validation":
                        {
                            "compatibility_level":
                                CompatibilityLevel.V1,
                            "description":
                                "Sample endpoint with failed validation.",
                            "uri":
                                "/adapter/fail-validation",
                            "request_schema": {},
                            "response_schema": {},
                        },
                    "/bridge/error":
                        {
                            "compatibility_level": CompatibilityLevel.V1,
                            "description": "Sample endpoint with server errors",
                            "uri": "/adapter/error",
                            "request_schema": {},
                            "response_schema": {},
                        },
                },
        }
        return self.return_json_response(json.dumps(response))

    def bridge(self, slug: str, data: Dict[str, Any]):
        """Sample bridge response"""
        match slug:
            case "error":
                response = self._bridge_error(slug)
            case "fail-validation":
                response = self._bridge_validation_error(slug)
            case _:
                response = self._bridge_success(data)

        return self.return_json_response(json.dumps(response))

    def _bridge_error(self, slug) -> ProblemDetail:
        """Sample bridge error response"""
        return {
            "type": "about:blank",
            "title": Status.INTERNAL_ERROR,
            "status": 400,
            "detail": f"No bridge endpoint found for slug: {slug}",
            "instance": f"/bridge/{slug}",
        }

    def _bridge_validation_error(self, slug) -> ValidationProblemDetail:
        """Sample bridge validation error response"""
        return {
            "type":
                "about:blank",
            "title":
                Status.INTERNAL_ERROR,
            "status":
                422,
            "detail":
                f"No bridge endpoint found for slug: {slug}",
            "instance":
                f"/bridge/{slug}",
            "validation_errors":
                [
                    {
                        "name": "field1",
                        "message": "message 123"
                    },
                    {
                        "name": "field2",
                        "message": "message 123"
                    },
                ],
        }

    def _bridge_success(self, data: Dict[str, Any]):
        """Sample bridge success response"""
        return {
            "compatibility_level": CompatibilityLevel.V1,
            "payload":
                {
                    "accountNumber": data["payload"]["accountNumber"],
                    "isValid": data["payload"]["accountNumber"] == 1234,
                },
        }
