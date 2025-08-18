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
                    "title": "Account Number",
                    "description": "Account Number",
                },
            "zipCode":
                {
                    "type": "string",
                    "title": "ZIP Code",
                    "description": "ZIP Code description",
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
                    "title": "Account Number",
                    "description": "Account Number",
                },
            "isValid":
                {
                    "type": "boolean",
                    "title": "Is Valid",
                    "description": "Has valid account",
                },
        },
    "required": ["accountNumber, isValid"],
    "additionalProperties": False,
}
