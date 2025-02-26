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
        You are an expert in government forms.  Process the following extracted json from a government form to be easier to use:

        {text}

        make sure to consider the following rules to process the json:
        1. Do NOT create nested sections.
        2. Within each section, If you find separate fields for first name, middle name, and last name, collate them into a single 'name' type field if possible. Do not create separate fields for name components.
        2. Within each section, If you find separate address related fields for Unit, city, zip code, street etc, collate them into a single 'address' type field if possible. Do not create separate fields for address components.
        3. For each "repeating_section", create an "entity_nickname" field which best describes the entity that the repeating entries are about.

        Output JSON structure should match this example:
        {json.dumps(JSON_EXAMPLE, indent=4)}

        Output only JSON, no explanations.
        """
