import json
import random
import argparse
import sys
import os
import uuid
import logging
import re

# TODO
# * templatize default config values such as "isRequired", "to-be-edited" etc
#

# This script converts a JSON representation of a form into a CiviForm-compatible JSON format.
# It handles different field types, including repeating sections, and generates
# the necessary structure for CiviForm programs and questions.

# Configure logging
logging.basicConfig(
    level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')

# Replace type "textarea", "signature" as "text"
# since CiviForm uses text for free form field
# CiviForm does not have signature type


def replace_field_types(data):
    if isinstance(data, dict):
        if "type" in data:
            data["type"] = data["type"].lower()
            if data["type"] not in ("name", "text", "number", "radio",
                                    "checkbox", "currency", "date", "email",
                                    "address", "phone", "repeating_section"):
                logging.warning(
                    f"Found unknown type that need to be replaced as text: {data}"
                )
                data["type"] = "text"
        # make sure ID not null, and that id exist.
        if "id" in data:
            if data["id"] == "null":
                new_id = "id-to-be-edited-" + uuid.uuid4().hex
                data["id"] = new_id.lower()
                logging.warning(
                    f"Replaced 'id' with: {data['id']} in field: {data}"
                )  #debug statement

        return {k: replace_field_types(v) for k, v in data.items()}
    elif isinstance(data, list):
        # Process each item in the list
        return [replace_field_types(item) for item in data]
    else:
        return data


def create_question(field, question_id, enumerator_id=None):
    is_multioption = field["type"] in ["radio", "checkbox", "dropdown"]
    is_enumerator = field["type"] == "enumerator"

    question = {
        "type": "multioption" if is_multioption else field["type"],
        "config":
            {
                "name": field["id"],
                "description": field["label"],
                "questionText":
                    {
                        "translations": {
                            "en_US": field["label"]
                        },
                        "isRequired": True
                    },
                "questionHelpText":
                    {
                        "translations": {
                            "en_US": field.get("help_text") or ""
                        },
                        "isRequired": True
                    },
                # Initialize validationPredicates with the basic structure
                "validationPredicates":
                    {
                        "type":
                            "multioption" if is_multioption else field["type"]
                    },
                "id": question_id,
                "universal": False,
                "primaryApplicantInfoTags": []
            }
    }

    if is_multioption:
        question_options = []
        option_admin_names = []

        if "options" in field:
            for idx, option in enumerate(field.get("options",), start=1):
                option_admin_name = re.sub(r'[^a-z0-9]+', '_', option.lower())

                # check if option name is invalid (empty)
                if not option_admin_name:
                    logging.error(
                        f"ERROR: Option name cannot be empty. Invalid option: '{option}' found in question : {field}"
                    )
                    raise ValueError(
                        f"ERROR: Option name cannot be empty. Invalid option: '{option}' found in question : {field}"
                    )

                option_entry = {
                    "id": idx,
                    "adminName": option_admin_name,
                    "localizedOptionText":
                        {
                            "translations": {
                                "en_US": option
                            },
                            "isRequired": True
                        },
                    "displayOrder": idx
                }
                question_options.append(option_entry)
                option_admin_names.append(option_admin_name)
        else:
            logging.warning(
                f"WARNING: Field '{field.get('id', 'unknown')}' is missing the 'options' key."
            )

        # Check the number of options for radio buttons
        if field["type"] == "radio" and len(question_options) < 2:
            logging.error(
                f"ERROR: Radio button question '{field['label']}' must have at least two options."
            )
            raise ValueError(
                f"ERROR: Radio button question '{field['label']}' must have at least two options."
            )

        # Check the number of options for multioption field (checkbox)
        if field["type"] == "checkbox" and len(question_options) < 1:
            logging.error(
                f"ERROR: Multioption field '{field['label']}' must have at least one option."
            )
            raise ValueError(
                f"ERROR: Multioption field '{field['label']}' must have at least one option."
            )

        question["questionOptions"] = question_options
        question["multiOptionQuestionType"] = "RADIO_BUTTON" if field[
            "type"] == "radio" else "CHECKBOX"
        question["optionAdminNames"] = option_admin_names
        question["options"] = question_options

    if is_enumerator:
        question["entityType"] = {
            "translations":
                {
                    "en_US": field.get("label", "entity-type-to-be-edited")
                },
            "isRequired": True
        }

        question["config"]["validationPredicates"] = {
            "type": "enumerator",
            "minEntities": None,
            "maxEntities": None
        }

    # Conditionally add enumeratorId for repeated questions
    if enumerator_id is not None:
        question["config"]["enumeratorId"] = enumerator_id

    # Set validationPredicates based on question type
    if question["type"] == "address":
        question["config"]["validationPredicates"]["disallowPoBox"] = False
    elif question["type"] == "multioption":
        question["config"]["validationPredicates"]["minChoicesRequired"] = 1
        # Set maxChoicesAllowed to 1 if it's a radio button question
        if field["type"] == "radio":
            question["config"]["validationPredicates"]["maxChoicesAllowed"] = 1
        else:  # For other multioption types (like checkbox), keep the original logic
            question["config"]["validationPredicates"][
                "maxChoicesAllowed"] = len(field.get("options",))
    elif question["type"] == "id":
        question["config"]["validationPredicates"]["minLength"] = None
        question["config"]["validationPredicates"]["maxLength"] = None
    elif question["type"] == "number":
        question["config"]["validationPredicates"]["min"] = None
        question["config"]["validationPredicates"]["max"] = None
    elif question["type"] == "text":
        question["config"]["validationPredicates"]["minLength"] = None
        question["config"]["validationPredicates"]["maxLength"] = None
    elif question["type"] == "fileupload":
        question["config"]["validationPredicates"]["maxFiles"] = None

    return question


def handle_repeating_section(section, question_id, block_id, output):
    # entity_type_label = section["fields"][0]["label"] # using the first table column as entity_type
    entity_type_label = section.get(
        "entity_nickname") or section["fields"][0]["label"]

    enumerator_question = create_question(
        {
            "type": "enumerator",
            "id": section["title"],
            "label": entity_type_label
        }, question_id)
    output["questions"].append(enumerator_question)
    enumerator_id = question_id
    question_id += 1

    # Create separate screen for the enumerator question
    output["program"]["blockDefinitions"].append(
        {
            "id":
                block_id,
            "name":
                section["title"],
            "description":
                section["title"],
            "localizedName":
                {
                    "translations": {
                        "en_US": section["title"]
                    },
                    "isRequired": True
                },
            "localizedDescription":
                {
                    "translations":
                        {
                            "en_US":
                                section.get(
                                    "help_text",
                                    "localizedDescription-TO-BE-EDITED")
                        },
                    "isRequired": True
                },
            "localizedEligibilityMessage":
                None,
            "hidePredicate":
                None,
            "optionalPredicate":
                None,
            "questionDefinitions":
                [
                    {
                        "id": enumerator_id,
                        "optional": False,
                        "addressCorrectionEnabled": False
                    }
                ]
        })
    block_id += 1  # Increment block_id after creating the enumerator block

    # Create repeated screen for the repeated questions
    repeated_block = {
        "id": block_id,
        "name": f"{section['title']} - Details",
        "description": f"Fields for {section['title']}",
        "localizedName":
            {
                "translations": {
                    "en_US": f"{section['title']} - Details"
                },
                "isRequired": True
            },
        "localizedDescription":
            {
                "translations": {
                    "en_US": "Details for repeated section."
                },
                "isRequired": True
            },
        "localizedEligibilityMessage": None,
        "hidePredicate": None,
        "optionalPredicate": None,
        "questionDefinitions": [],
        "repeaterId": block_id - 1  # Link to the enumerator screen's ID
    }

    for field in section["fields"]:
        question = create_question(field, question_id, enumerator_id)
        question["config"]["questionText"]["translations"][
            "en_US"] = f"{field['label']} for $this"
        output["questions"].append(question)
        repeated_block["questionDefinitions"].append(
            {
                "id": question_id,
                "optional": False,
                "addressCorrectionEnabled": False
            })
        question_id += 1

    output["program"]["blockDefinitions"].append(repeated_block)
    block_id += 1
    return question_id, block_id


def convert_to_civiform_json(unprocessed_input_json):
    logging.info("converting to CiviForm json ... ")

    input_json = replace_field_types(unprocessed_input_json)

    program_id = random.randint(1, 1000)
    output = {
        "program":
            {
                "id":
                    program_id,
                "adminName":
                    input_json["title"].lower().replace(" ", "_")[:8] +
                    str(program_id),
                "adminDescription":
                    "program-adminDescription-TO-BE-EDITED",
                "externalLink":
                    "",
                "displayMode":
                    "PUBLIC",
                "notificationPreferences": [],
                "localizedName":
                    {
                        "translations": {
                            "en_US": input_json["title"]
                        },
                        "isRequired": True
                    },
                "localizedDescription":
                    {
                        "translations":
                            {
                                "en_US":
                                    input_json.get("help_text") or
                                    "program-localizedDescription-TO-BE-EDITED"
                            },
                        "isRequired": True
                    },
                "localizedShortDescription":
                    {
                        "translations":
                            {
                                "en_US":
                                    input_json.get("help_text") or
                                    "program-localizedShortDescriptionTO-BE-EDITED"
                            },
                        "isRequired": True
                    },
                "localizedConfirmationMessage":
                    {
                        "translations":
                            {
                                "en_US":
                                    "program-localizedConfirmationMessageTO-BE-EDITED"
                            },
                        "isRequired": True
                    },
                "blockDefinitions": [],
                "programType":
                    "DEFAULT",
                "eligibilityIsGating":
                    True,
                "acls": {
                    "tiProgramViewAcls": []
                },
                "localizedSummaryImageDescription":
                    None,
                "categories": [],
                "applicationSteps":
                    [
                        {
                            "title":
                                {
                                    "translations":
                                        {
                                            "en_US":
                                                "program-applicationSteps TO-BE-EDITE"
                                        },
                                    "isRequired": True
                                },
                            "description":
                                {
                                    "translations":
                                        {
                                            "en_US":
                                                "program-applicationSteps description TO-BE-EDITED"
                                        },
                                    "isRequired": True
                                }
                        }
                    ]
            },
        "questions": []
    }

    question_id = 1
    block_id = 1
    for section in input_json["sections"]:
        block = {
            "id":
                block_id,
            "name":
                section["title"],
            "description":
                section.get("help_text", "block-description-TO-BE-EDITED"),
            "localizedName":
                {
                    "translations": {
                        "en_US": section["title"]
                    },
                    "isRequired": True
                },
            "localizedDescription":
                {
                    "translations":
                        {
                            "en_US":
                                section.get(
                                    "help_text",
                                    "block-localizedDescription-TO-BE-EDITED")
                        },
                    "isRequired": True
                },
            "localizedEligibilityMessage":
                None,
            "hidePredicate":
                None,
            "optionalPredicate":
                None,
            "questionDefinitions": []
        }

        if section.get("type") == "repeating_section":
            question_id, block_id = handle_repeating_section(
                section, question_id, block_id, output)
            # Note: block_id is incremented within handle_repeating_section
        else:
            for field in section["fields"]:
                question = create_question(field, question_id)
                output["questions"].append(question)
                block["questionDefinitions"].append(
                    {
                        "id": question_id,
                        "optional": False,
                        "addressCorrectionEnabled": False
                    })
                question_id += 1

            output["program"]["blockDefinitions"].append(block)
            block_id += 1

    return json.dumps(output, indent=2)


def main():
    parser = argparse.ArgumentParser(
        description="Convert JSON to CiviForm format.")
    parser.add_argument("input_file", help="Path to the input JSON file.")
    parser.add_argument("-o", "--output", help="Path to the output JSON file.")
    args = parser.parse_args()

    base_filename = os.path.basename(args.input_file)
    base_name, _ = os.path.splitext(base_filename)
    output_filename = args.output if args.output else f"{base_name}-civiform.json"
    print(f"CiviForm Output filename: {output_filename}")

    try:
        with open(args.input_file, "r") as f:
            unprocessed_input_json = json.load(f)

        civiform_json = convert_to_civiform_json(unprocessed_input_json)

        with open(output_filename, "w") as f:
            f.write(civiform_json)

        logging.info(f"Converted JSON saved to {output_filename}. Done!")

    except FileNotFoundError:
        print(f"Error: File '{args.input_file}' not found.")
        sys.exit(1)
    except json.JSONDecodeError:
        print(f"Error: File '{args.input_file}' is not valid JSON.")
        sys.exit(1)


if __name__ == "__main__":
    main()
