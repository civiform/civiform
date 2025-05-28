from enum import Enum
from typing import Any, Dict, List, Optional, TypedDict


class CompatibilityLevel(str, Enum):
    V1 = "v1"


class ValidationError(TypedDict):
    name: str
    message: str


class ProblemDetail(TypedDict):
    type: str
    title: str
    status: int
    detail: str
    instance: Optional[str]


class ValidationProblemDetail(ProblemDetail):
    validation_errors: List[ValidationError]


class HealthcheckResponse(TypedDict):
    timestamp: int


class Endpoint(TypedDict):
    compatibility_level: CompatibilityLevel
    description: str
    request_schema: Dict[str, Any]
    response_schema: Dict[str, Any]


class DiscoveryResponse(TypedDict):
    endpoints: Dict[str, Endpoint]


class BridgeRequest(TypedDict):
    payload: Dict[str, Any]


class BridgeResponse(TypedDict):
    compatibility_level: CompatibilityLevel
    payload: Dict[str, Any]
