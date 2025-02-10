import json
import random
import argparse
import sys
import os
import uuid

# This script converts a JSON representation of a form into a CiviForm-compatible JSON format.
# It handles different field types, including repeating sections, and generates 
# the necessary structure for CiviForm programs and questions.

# Replace type "textarea", "signature" as "text"
# since CiviForm uses text for free form field
# CiviForm does not have signature type
def replace_field_types(data):
    if isinstance(data, dict):
        # Check if the key "type" exists and update its value accordingly
        if "type" in data:
            if data["type"] == "textarea":
                data["type"] = "text"
            elif data["type"] == "signature":
                data["type"] = "text"
           # elif data["type"] == "number":
           #     data["type"] = "text"  # Convert number to text
            # Add more type conversions if needed
        # Recursively process the dictionary
        return {k: replace_field_types(v) for k, v in data.items()}
    elif isinstance(data, list):
        # Process each item in the list
        return [replace_field_types(item) for item in data]
    else:
        return data


def create_multioption_question(field, question_id, enumerator_id):
    question_options = [] # Initialize lists here
    option_admin_names =[]
    for idx, option in enumerate(field.get("options",), start=1):
        question_options.append({
            "id": idx,
            "adminName": option.lower(),
            "localizedOptionText": {"translations": {"en_US": option}, "isRequired": True},
            "displayOrder": idx
        })
        option_admin_names.append(option.lower())

    return {
        "type": "multioption",
        "config": {
            "name": field["id"],
            "description": field["label"],
            "questionText": {"translations": {"en_US": field["label"]}, "isRequired": True},
            "questionHelpText": {"translations": {"en_US": field.get("help_text", "questionHelpTextTO-BE-EDITED")}, "isRequired": True},
            "validationPredicates": {
                "type": "multioption",
                "minChoicesRequired": 1,
                "maxChoicesAllowed": 1 if field.get("original_type") == "radio" else len(field.get("options",))
            },
            "id": question_id,
            "enumeratorId": enumerator_id,
            "universal": False,
            "primaryApplicantInfoTags": [] # Empty list
        },
        "questionOptions": question_options,
        "multiOptionQuestionType": "RADIO_BUTTON" if field.get("original_type") == "radio" else "CHECKBOX",
        "optionAdminNames": option_admin_names,
        "options": question_options
    }

def create_question(field, question_id, enumerator_id=None):
    is_multioption = field["type"] in ["radio", "checkbox"]
    is_enumerator = field["type"] == "enumerator"

    question = {
        "type": "multioption" if is_multioption else field["type"],
        "config": {
            "name": field["id"],
            "description": field["label"],
            "questionText": {
                "translations": {"en_US": field["label"]},
                "isRequired": True
            },
            "questionHelpText": {
                "translations": {"en_US": field.get("help_text", "TO-BE-EDITED")},
                "isRequired": True
            },
            # Initialize validationPredicates with the basic structure
            "validationPredicates": {
                "type": "multioption" if is_multioption else field["type"]
            },
            "id": question_id,
            "universal": False,
            "primaryApplicantInfoTags":[]
        }
    }

    if is_multioption:
        question_options =[]
        option_admin_names =[]
        for idx, option in enumerate(field.get("options",), start=1):
            option_entry = {
                "id": idx,
                "adminName": option.lower().replace(" ", "_"),
                "localizedOptionText": {
                    "translations": {"en_US": option},
                    "isRequired": True
                },
                "displayOrder": idx
            }
            question_options.append(option_entry)
            option_admin_names.append(option.lower().replace(" ", "_"))

        question["questionOptions"] = question_options
        question["multiOptionQuestionType"] = "RADIO_BUTTON" if field["type"] == "radio" else "CHECKBOX"
        question["optionAdminNames"] = option_admin_names
        question["options"] = question_options

    if is_enumerator:
        question["entityType"] = {"translations": {"en_US": "Entity"}, "isRequired": True}
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
        question["config"]["validationPredicates"]["minChoicesRequired"] = 1 if is_multioption else None
        question["config"]["validationPredicates"]["maxChoicesAllowed"] = len(field.get("options",)) if is_multioption else None
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
    #entity_type_label = section["fields"]["label"]  # Use the label of the first field as entity type
    entity_type_label = section["fields"][0]["label"] # using the first table column as entity_type 
    enumerator_question = create_question({
        "type": "enumerator",
        "id": section["title"],
        "label": entity_type_label
    }, question_id)
    output["questions"].append(enumerator_question)
    enumerator_id = question_id
    question_id += 1

    # Create separate screen for the enumerator question
    output["program"]["blockDefinitions"].append({
        "id": block_id,
        "name": section["title"],
        "description": section["title"],
        "localizedName": {"translations": {"en_US": section["title"]}, "isRequired": True},
        "localizedDescription": {"translations": {"en_US": section.get("help_text", "TO-BE-EDITED")}, "isRequired": True},
        "localizedEligibilityMessage": None,
        "hidePredicate": None,
        "optionalPredicate": None,
        "questionDefinitions": [{"id": enumerator_id, "optional": False, "addressCorrectionEnabled": False}]
    })
    block_id += 1  # Increment block_id after creating the enumerator block

    # Create repeated screen for the repeated questions
    repeated_block = {
        "id": block_id,
        "name": f"{section['title']} - Details",
        "description": f"Fields for {section['title']}",
        "localizedName": {"translations": {"en_US": f"{section['title']} - Details"}, "isRequired": True},
        "localizedDescription": {"translations": {"en_US": "Details for repeated section."}, "isRequired": True},
        "localizedEligibilityMessage": None,
        "hidePredicate": None,
        "optionalPredicate": None,
        "questionDefinitions":[],
        "repeaterId": block_id - 1  # Link to the enumerator screen's ID
    }

    for field in section["fields"]:
        question = create_question(field, question_id, enumerator_id)
        question["config"]["questionText"]["translations"]["en_US"] = f"{field['label']} for $this"
        output["questions"].append(question)
        repeated_block["questionDefinitions"].append({"id": question_id, "optional": False, "addressCorrectionEnabled": False})
        question_id += 1

    output["program"]["blockDefinitions"].append(repeated_block)
    block_id += 1
    return question_id, block_id


def convert_to_civiform_json(unprocessed_input_json):
    input_json = replace_field_types(unprocessed_input_json)

    program_id =  random.randint(1, 1000)
    output = {
        "program": {
            "id": str(program_id),
            "adminName": input_json["title"].lower().replace(" ", "_")[:8] + str(program_id),
            "adminDescription": "TO-BE-EDITED",
            "externalLink": "",
            "displayMode": "PUBLIC",
            "notificationPreferences":[],
            "localizedName": {
                "translations": {"en_US": input_json["title"]},
                "isRequired": True
            },
            "localizedDescription": {
                "translations": {"en_US": input_json.get("help_text", "TO-BE-EDITED")},
                "isRequired": True
            },
            "localizedShortDescription": {
                "translations": {"en_US": "TO-BE-EDITED"},
                "isRequired": True
            },
            "localizedConfirmationMessage": {
                "translations": {"en_US": "TO-BE-EDITED"},
                "isRequired": True
            },
            "blockDefinitions":[],
            "programType": "DEFAULT",
            "eligibilityIsGating": True,
            "acls": {"tiProgramViewAcls":[]},
            "localizedSummaryImageDescription": None,
            "categories":[],
            "applicationSteps":[ {
                "title" : {
                "translations" : {
                    "en_US" : "step 1 title"
                },
                "isRequired" : True
                },
                "description" : {
                "translations" : {
                    "en_US" : "step 1 description"
                },
                "isRequired" : True
                }
            } ]
        },
        "questions":[]
    }

    question_id = 1
    block_id = 1
    for section in input_json["sections"]:
        block = {
            "id": block_id,
            "name": section["title"],
            "description": section.get("help_text", "TO-BE-EDITED"),
            "localizedName": {"translations": {"en_US": section["title"]}, "isRequired": True},
            "localizedDescription": {"translations": {"en_US": section.get("help_text", "TO-BE-EDITED")}, "isRequired": True},
            "localizedEligibilityMessage": None,
            "hidePredicate": None,
            "optionalPredicate": None,
            "questionDefinitions":[]
        }

        if section.get("type") == "repeating_section":
            question_id, block_id = handle_repeating_section(section, question_id, block_id, output)
            # Note: block_id is incremented within handle_repeating_section
        else:
            for field in section["fields"]:
                question = create_question(field, question_id)
                output["questions"].append(question)
                block["questionDefinitions"].append({"id": question_id, "optional": False, "addressCorrectionEnabled": False})
                question_id += 1

            output["program"]["blockDefinitions"].append(block)
            block_id += 1

    return json.dumps(output, indent=2)



def main():
    parser = argparse.ArgumentParser(description="Convert JSON to CiviForm format.")
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
    except FileNotFoundError:
        print(f"Error: File '{args.input_file}' not found.")
        sys.exit(1)
    except json.JSONDecodeError:
        print(f"Error: File '{args.input_file}' is not valid JSON.")
        sys.exit(1)

    civiform_json = convert_to_civiform_json(unprocessed_input_json)

    with open(output_filename, "w") as f:
        f.write(civiform_json)

    print(f"Converted JSON saved to {output_filename}")

if __name__ == "__main__":
    main()