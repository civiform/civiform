"""Provides functionality for an environment variable documentation file meets the expected schema.
"""

import importlib
import json
import jsonschema
import typing


def validate(env_var_doc_file: typing.TextIO) -> str:
    """
    Validates an environment variable documentation file against the schema defined in schema.json.

    Returns an empty string if the file is valid and an empty string if not.
    """
    try:
        # Ensure we are reading from the beginning of the file.
        env_var_doc_file.seek(0)
        docs = json.load(env_var_doc_file)
    except json.JSONDecodeError as e:
        return f"file is not valid JSON: {e}"

    try:
        jsonschema.Draft7Validator(
            json.loads(
                # The schema itself is validated in schema_test.py.
                importlib.resources.files("env_var_docs.schema").joinpath(
                    "schema.json").read_text())).validate(docs)
    except jsonschema.exceptions.ValidationError as e:
        return f"file does not meet schema: {e}"

    return ""
