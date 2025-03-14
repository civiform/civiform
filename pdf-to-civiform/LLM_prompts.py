import json

JSON_EXAMPLE = {
    "title": "[Extracted Form Title]",
    "help_text": "[Relevant Instructional Text]",
    "sections": [
        {
            "title": "[Section Name]",
            "help_text": "[Relevant Instructional or informational Text (can be enclosed in brackets)]",
            "fields": [
                {"label": "[Field Label]", "type": "[Field Type]", "help_text": "[Field-specific Instruction]", "id": "[Generated Field ID]"},
                {"label": "[Field Label]", "type": "[radio]", "options": ["opt 1", "opt 2", "opt 3"], "help_text": "[Field-specific Instruction]", "id": "[Generated Field ID]"},
                {"label": "[Field Label]", "type": "[checkbox]", "options": ["opt 1", "opt 2", "opt 3"], "help_text": "[Field-specific Instruction]", "id": "[Generated Field ID]"}
            ]
        },
        {
            "title": "[Section Name]",
            "help_text": "[Relevant Instructional or informational Text (can be enclosed in brackets)]",
            "type": "repeating_section",
            "fields": [
                {"label": "[Field Label]", "type": "[Field Type]", "help_text": "[Field-specific Instruction]", "id": "[Generated Field ID]"},
                {"label": "[Field Label]", "type": "[radio]", "options": ["opt 1", "opt 2", "opt 3"], "help_text": "[Field-specific Instruction]", "id": "[Generated Field ID]"},
                {"label": "[Field Label]", "type": "[checkbox]", "options": ["opt 1", "opt 2", "opt 3"], "help_text": "[Field-specific Instruction]", "id": "[Generated Field ID]"}
            ]
        },
        {
            "title": "[Section Name]",
            "help_text": "[Relevant Instructional or informational Text (can be enclosed in brackets)]",
            "fields": [
                {"label": "[Field Label]", "type": "[file_upload]", "help_text": "[Field-specific Instruction]", "id": "[Generated Field ID]"}
            ]
        },
    ]
}


class LLMPrompts:
    @staticmethod
    def pdf_to_json_prompt():
        """Prompt for converting PDF text to intermediary JSON."""
        prompt = f"""
        You are an expert in document analysis and structured form modeling.  
        You are given a PDF document containing a blank government application form.
        
        Instructions:
        
        Identify form fields, labels, and instructions, and format the output as JSON.
        Ensure correct field types (number, radio button, text, checkbox, etc.), group fields into sections,
        and associate instructions with relevant fields. Please DO NOT ignore identifying help text.
        Please skip checklist/instructions pages which is for context.
        
        Additionally, detect repeating sections and mark them accordingly.

        A table is usually a repeating section. 

        make sure to consider the following rules to extract input fields and types:
        1. **Address**: address (e.g., residential, work, mailing). Unit, city, zip code, street, municipality, county, district etc are included. Please collate them into a single field.
        2. **Currency**: Currency values with decimal separators (e.g., income, debts).
        3. **Checkbox**: Allows multiple selections (e.g., ethnicity, available benefits, languages spoken etc). collate options for checkboxes as one field of "checkbox" type if possible. Checkbox options must be unique.
        4. **Date**: Captures dates (e.g., birth date, graduation date, month, year etc).
        5. **Email**: email address. Please collate domain and username if asked separately.
        6. **File Upload**: File attachments (e.g., PDFs, images)
        7. **Name**: A person's name. Please collate first name, middle name, and last name into full name.
        8. **Number**: Integer values e.g., number of household members etc.
        9. **Radio Button**: Single selection from short lists (<=7 items, e.g., Yes/No questions).
        10. **Text**: Open-ended text field for letters, alphanumerics, or symbols.
        11. **Phone**: phone numbers.
        12.  If you see a field you do not understand, please use "unknown" as the type, associate relevant text as help text and assign a unique ID.

        Output JSON structure should match this example:
        {json.dumps(JSON_EXAMPLE, indent=4)}
        
        Output only JSON, no explanations.
        """
        return prompt

    @staticmethod
    def post_process_json_prompt(text):
        """Sends extracted json text to Gemini and asks it to collate related fields into appropriate civiform types, in particular names and address."""
        #  TODO: could not reliably move repeating sections out of sections without LLM creating unnecessary repeating sections.
        
        prompt = f"""
        You are an expert in government forms.  Process the following extracted json from a government form to be easier to use:
        
        {text}
        
        make sure to consider the following rules to process the json: 
        1. Do NOT create nested sections.
        2. Within each section, If you find separate fields for first name, middle name, and last name, you must collate them into a single 'name' type field. Please DO NOT create separate fields for name fields.
        3. Within each section, If you find separate address related fields for unit, city, zip code, street, municipality, county, district etc, you must collate them into a single 'address' type field. Please DO NOT create separate fields for address components. However, do separate mailing address from physical address.
        4. For each "repeating_section", create an "entity_nickname" field which best describes the entity that the repeating entries are about.
        5. make sure IDs are unique across the entire form.
        6. Any text field that can be a number (integer) must be corrected to a number type - such as company number, frequency etc.
        7. If necessary, create an additional new section with ONE file_upload field for text/checkbox fields that can be file attachments.
        
        Output JSON structure should match this example:
        {json.dumps(JSON_EXAMPLE, indent=4)}

        Output only JSON, no explanations.
        """
        return prompt
