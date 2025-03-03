import json

JSON_EXAMPLE = {
    "title": "[Extracted Form Title]",
    "help_text": "[Relevant Instructional Text]",
    "sections": [
        {
            "title": "[Section Name]",
            "fields": [
                {"label": "[Field Label]", "type": "[Field Type]", "help_text": "[Field-specific Instruction]", "id": "[Generated Field ID]"},
                {"label": "[Field Label]", "type": "[radio]", "options": ["opt 1", "opt 2", "opt 3"], "help_text": "[Field-specific Instruction]", "id": "[Generated Field ID]"},
                {"label": "[Field Label]", "type": "[checkbox]", "options": ["opt 1", "opt 2", "opt 3"], "help_text": "[Field-specific Instruction]", "id": "[Generated Field ID]"}
            ]
        },
        {
            "title": "[Section Name]",
            "help_text": "[Relevant Instructional Text]",
            "type": "repeating_section",
            "fields": [
                {"label": "[Field Label]", "type": "[Field Type]", "help_text": "[Field-specific Instruction]", "id": "[Generated Field ID]"},
                {"label": "[Field Label]", "type": "[radio]", "options": ["opt 1", "opt 2", "opt 3"], "help_text": "[Field-specific Instruction]", "id": "[Generated Field ID]"},
                {"label": "[Field Label]", "type": "[checkbox]", "options": ["opt 1", "opt 2", "opt 3"], "help_text": "[Field-specific Instruction]", "id": "[Generated Field ID]"}
            ]
        }
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
        and associate instructions with relevant fields.
        
        Additionally, detect repeating sections and mark them accordingly.

        A table is usually a repeating section. 

        make sure to consider the following rules to extract input fields and types:
        1. **Address**: address (e.g., residential, work, mailing). Unit, city, zip code, street etc are included. Collate them if possible.
        2. **Currency**: Currency values with decimal separators (e.g., income, debts).
        3. **Checkbox**: Allows multiple selections (e.g., ethinicity, available benefits, languages spoken etc). collate options for checkboxes as one field of "checkbox" type if possible. Checkbox options must be uqique.
        4. **Date**: Captures dates (e.g., birth date, graduation date).
        6. **Email**: email address. Collate domain and username if asked separately.
        8. **File Upload**: File attachments (e.g., PDFs, images).
        10. **Name**: person's name. Collate first name, middle name, and last name into full name if possible.
        11. **Number**: Integer values (e.g., number of household members etc).
        12. **Radio Button**: Single selection from short lists (<=7 items, e.g., Yes/No questions).
        14. **Text**: Open-ended text field for letters, numbers, or symbols.
        15. **Phone**: phone numbers.
        16.  If you see a field you do not understand, please use "unknown" as the type, associate relevant text as help text and assign a unique ID.
        
        Output JSON structure should match this example:
        {json.dumps(JSON_EXAMPLE, indent=4)}
        
        Output only JSON, no explanations.
        """
        return prompt

    @staticmethod
    def post_process_json_prompt(text):
        """Sends extracted json text to Gemini and asks it to collate related fields into appropriate civiform types, in particular names and address."""
        #  TODO: could not reliablely move repeating sections out of sections without LLM creating unnecessary repeating sections.   
        
        prompt = f"""
        You are an expert in government forms.  Process the following extracted json from a government form to be easier to use:
        
        {text}
        
        make sure to consider the following rules to process the json: 
        1. Do NOT create nested sections. 
        2. Within each section, If you find separate fields for first name, middle name, and last name, collate them into a single 'name' type field if possible. Do not create separate fields for name components. 
        2. Within each section, If you find separate address related fields for Unit, city, zip code, street etc, collate them into a single 'address' type field if possible. Do not create separate fields for address components.
        3. For each "repeating_section", create an "entity_nickname" field which best describes the entity that the repeating entries are about.    
        4. make sure IDs are unique across the entire form.

        
        Output JSON structure should match this example:
        {json.dumps(JSON_EXAMPLE, indent=4)}

        Output only JSON, no explanations.
        """
        return prompt
