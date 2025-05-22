request_schema = {
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "$id": "https://civiform.us/schemas/request.json",
    "title": "Response title",
    "description": "Request schema description",
    "type": "object",
    "properties":
        {
            "accountNumber":
                {
                    "type": "number",
                    "description": "Account Number",
                },
            "zipCode": {
                "type": "string",
                "description": "Zip Code"
            },
        },
    "required": [
        "accountNumber",
        "zipCode",
    ],
    "additionalProperties": False,
}

response_schema = {
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "$id": "https://civiform.us/schemas/response-schema.json",
    "title": "Response title",
    "description": "Response schema description",
    "type": "object",
    "properties":
        {
            "accountNumber":
                {
                    "type": "number",
                    "description": "Account Number",
                },
            "isValid": {
                "type": "boolean",
                "description": "Has valid account",
            },
        },
    "required": ["accountNumber, isValid"],
    "additionalProperties": False,
}
