import json

JSON_EXAMPLE = {
        "title": "[Extracted Form Title]",
        "help_text": "[Relevant Instructional Text]",

        "sections": [
            {
                "title": "[Section Name]",
                "fields": [
                    {"label": "[Field Label]", "type": "[Field Type]",
                        "help_text": "[Field-specific Instruction]", "id": "[Generated Field ID]"},
                    {"label": "[Field Label]", "type": "[radio]", "options": ["opt 1", "opt 2",
                                                                              "opt 3"], "help_text": "[Field-specific Instruction]", "id": "[Generated Field ID]"},
                    {"label": "[Field Label]", "type": "[checkbox]", "options": ["opt 1", "opt 2",
                                                                                 "opt 3"], "help_text": "[Field-specific Instruction]", "id": "[Generated Field ID]"}
                ]
            },
            {
                "title": "[Section Name]",
                "help_text": "[Relevant Instructional Text]",
                "type": "repeating_section",
                "fields": [
                    {"label": "[Field Label]", "type": "[Field Type]",
                        "help_text": "[Field-specific Instruction]", "id": "[Generated Field ID]"},
                    {"label": "[Field Label]", "type": "[radio]", "options": ["opt 1", "opt 2",
                                                                              "opt 3"], "help_text": "[Field-specific Instruction]", "id": "[Generated Field ID]"},
                    {"label": "[Field Label]", "type": "[checkbox]", "options": ["opt 1", "opt 2",
                                                                              "opt 3"], "help_text": "[Field-specific Instruction]", "id": "[Generated Field ID]"}
                ]
            }
        ]
}

def get_prompt(text):
    return f"""
    You are an AI that extracts structured form data from government application PDFs.
    The following text was extracted from a government form:

    {text}

    Identify form fields, labels, and instructions, and format the output as JSON.
    Ensure correct field types (number, radio button, text, checkbox, etc.), group fields into sections,
    and associate instructions with relevant fields.

    Additionally, detect repeating sections and mark them accordingly.

    A table is usually a repeating section.

    make sure to consider the following rules to extract input fields and types:
    1. **Address**: address (e.g., residential, work, mailing). Unit, city, zip code, street etc are included. Collate them if possible.
    2. **Currency**: Currency values with decimal separators (e.g., income, debts).
    3. **Checkbox**: Allows multiple selections (e.g., ethinicity, available benefits, languages spoken etc). collate options for checkboxes as one field of "checkbox" type if possible.
    4. **Date**: Captures dates (e.g., birth date, graduation date).
    5. **Dropdown**: Single selection from long lists (>8 items) (e.g., list of products).
    6. **Email**: email address. Collate domain and username if asked separately.
    8. **File Upload**: File attachments (e.g., PDFs, images).
    9. **ID**: Numeric identifiers. Can specify min/max value.
    10. **Name**: person's name. Collate first name, middle name, and last name into full name if possible.
    11. **Number**: Integer values (e.g., number of household members etc).
    12. **Radio Button**: Single selection from short lists (<=7 items, e.g., Yes/No questions).
    14. **Text**: Open-ended text field for letters, numbers, or symbols.
    15. **Phone**: phone numbers.

    If you see a field you do not understand, please use "unknown" as the type, associate relevant text as help text and assign a unique ID.

    Output JSON structure should match this example:
    {json.dumps(JSON_EXAMPLE, indent=4)}

    Output only JSON, no explanations.
    """
