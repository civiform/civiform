"""
Mock service for testing the API Bridge
"""

import time

from apibridge import sample_data
from flask import Blueprint, Response, jsonify, request

from .models import (
    BridgeResponse,
    CompatibilityLevel,
    DiscoveryResponse,
    Endpoint,
    HealthcheckResponse,
    ProblemDetail,
    ValidationProblemDetail,
)

apibridge_blueprint = Blueprint("apibridge", __name__)


@apibridge_blueprint.route("/health-check")
def healthcheck() -> tuple[Response, int]:
    """Health check endpoint to verify service status"""
    try:
        response_code = int(request.headers.get("Emulate-Response-Code", 200))

        match response_code:
            case 400 | 401 | 500:
                response = create_problem_detail(response_code)
            case 429:
                response = create_problem_detail(response_code)
                http_response = jsonify(response)
                http_response.headers["Retry-After"] = "1"
                return http_response, response_code
            case _:
                response = HealthcheckResponse(
                    timestamp=int(time.time() + 1000))

        print(response, flush=True)
        return jsonify(response), response_code
    except Exception as e:
        print(e, flush=True)
        return jsonify(unhandled()), 500


@apibridge_blueprint.route("/discovery")
def discovery() -> tuple[Response, int]:
    """Discovery endpoint that provides information about available service endpoints"""
    try:
        response_code = int(request.headers.get("Emulate-Response-Code", 200))

        match response_code:
            case 400 | 401 | 500:
                response = create_problem_detail(response_code)
            case 429:
                response = create_problem_detail(response_code)
                http_response = jsonify(response)
                http_response.headers["Retry-After"] = "1"
                return http_response, response_code
            case _:
                response = DiscoveryResponse(
                    endpoints={
                        "/bridge/success":
                            Endpoint(
                                compatibility_level=CompatibilityLevel.V1,
                                description="Sample endpoint that is successful",
                                request_schema=sample_data.request_schema,
                                response_schema=sample_data.response_schema,
                            )
                    })
        return jsonify(response), response_code
    except Exception as e:
        print(e, flush=True)
        return jsonify(unhandled()), 500


@apibridge_blueprint.route("/bridge/<slug>", methods=["POST"])
def bridge(slug: str) -> tuple[Response, int]:
    """Sample bridge response endpoint"""
    try:
        data = request.get_json()
        print("Slug: " + slug, flush=True)
        print(data, flush=True)

        response_code = int(request.headers.get("Emulate-Response-Code", 200))

        match response_code:
            case 400 | 401 | 404 | 500:
                response = create_problem_detail(response_code)
            case 422:
                response = create_val_problem_detail()
            case 429:
                response = create_problem_detail(response_code)
                http_response = jsonify(response)
                http_response.headers["Retry-After"] = "1"
                return http_response, response_code
            case _:
                response = BridgeResponse(
                    compatibility_level=CompatibilityLevel.V1,
                    payload={
                        "accountNumber": data["payload"]["accountNumber"],
                        "isValid": data["payload"]["accountNumber"] == 1234,
                    },
                )

        return jsonify(response), response_code
    except Exception as e:
        print(e, flush=True)
        return jsonify(unhandled()), 500


def create_problem_detail(status: int) -> ProblemDetail:
    return ProblemDetail(
        type="https://localhost.localdomain/type/" + str(status),
        title="title-" + str(status),
        status=status,
        detail="detail-" + str(status),
        instance=request.path,
    )


def create_val_problem_detail() -> ValidationProblemDetail:
    return ValidationProblemDetail(
        type="https://localhost.localdomain/type/422",
        title="title-422",
        status=422,
        detail="detail-422",
        instance=request.path,
        validation_errors=[
            {
                "name": "accountNumber",
                "message": "Invalid account number"
            },
            {
                "name": "zipCode",
                "message": "Invalid zip code"
            },
        ],
    )


def unhandled() -> ProblemDetail:
    return ProblemDetail(
        type="https://localhost.localdomain/type/500",
        title="unhandled",
        status=500,
        detail="Error in the mock bridge. not a simulated error",
        instance=request.path,
    )
