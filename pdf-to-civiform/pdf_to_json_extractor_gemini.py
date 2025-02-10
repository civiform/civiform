import pdfplumber
import google.generativeai as genai
import json
import re
from jsonschema import validate, ValidationError
from flask import Flask, request, jsonify, render_template
from werkzeug.utils import secure_filename
import os
import logging
from convert_to_civiform_json import convert_to_civiform_json 

# This script extracts text from uploaded PDF files, uses a Gemini LLM to 
# convert the text into structured JSON representing a form, formats the JSON
# for better readability, and then converts it into a CiviForm-compatible 
# JSON format.  It uses a Flask web server to handle file uploads.

# Configure logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')

app = Flask(__name__)
app.config['UPLOAD_FOLDER'] = 'uploads'
os.makedirs(app.config['UPLOAD_FOLDER'], exist_ok=True)

# Set your Google API key
GOOGLE_API_KEY = "ADD YOUR OWN"
genai.configure(api_key=GOOGLE_API_KEY)

# List available models
models = genai.list_models()
for model in models:
    print(model.name)

model = genai.GenerativeModel("models/gemini-2.0-flash-exp")

output_filename="formated_pdf-extract.json"

# Define JSON Schema for validation
JSON_EXAMPLE = {
        "title": "[Extracted Form Title]",
        "help_text": "[Relevant Instructional Text]",

        "sections": [
            {
                "title": "[Section Name]",
                "fields": [
                    {"label": "[Field Label]", "type": "[Field Type]",
                        "help_text": "[Field-specific Instruction]", "id": "[Generated Field ID]"},
                    {"label": "[Field Label]", "type": "[radio]", "options": ["NEW UDP APPLICANT", "CURRENTLY ENROLLED IN UDP",
                                                                              "WITHDRAWING FROM UDP"], "help_text": "[Field-specific Instruction]", "id": "[Generated Field ID]"},
                    {"label": "[Field Label]", "type": "[checkbox]", "options": ["NEW UDP APPLICANT", "CURRENTLY ENROLLED IN UDP",
                                                                                 "WITHDRAWING FROM UDP"], "help_text": "[Field-specific Instruction]", "id": "[Generated Field ID]"}
                ]
            },
            {
                "title": "[Section Name]",
                "help_text": "[Relevant Instructional Text]",
                "type": "repeating_section",
                "fields": [
                    {"label": "[Field Label]", "type": "[Field Type]",
                        "help_text": "[Field-specific Instruction]", "id": "[Generated Field ID]"},
                    {"label": "[Field Label]", "type": "[radio]", "options": ["NEW UDP APPLICANT", "CURRENTLY ENROLLED IN UDP",
                                                                              "WITHDRAWING FROM UDP"], "help_text": "[Field-specific Instruction]", "id": "[Generated Field ID]"},
                    {"label": "[Field Label]", "type": "[checkbox]", "options": ["NEW UDP APPLICANT", "CURRENTLY ENROLLED IN UDP",
                                                                                 "WITHDRAWING FROM UDP"], "help_text": "[Field-specific Instruction]", "id": "[Generated Field ID]"}
                ]
            }
        ]
}


def extract_text_from_pdf(pdf_path):
    print(f"Extracts text from a given PDF file: {pdf_path}")
    """Extracts text from a given PDF file while maintaining basic layout structure."""
    text_blocks = []
    with pdfplumber.open(pdf_path) as pdf:
        for page in pdf.pages:
            text = page.extract_text()
            if text:
                text_blocks.append(text)
    return "\n\n".join(text_blocks)

def process_text_with_llm(text, base_name ):
    print(f"process_text_with_llm...")

    """Sends extracted PDF text to Gemini and asks it to format the content into structured JSON."""
    prompt = f"""
    You are an AI that extracts structured form data from government application PDFs.
    The following text was extracted from a government form:
    
    {text}
    
    Identify form fields, labels, and instructions, and format the output as JSON.
    Ensure correct field types (text, checkbox, dropdown, etc.), group fields into sections,
    and associate instructions with relevant fields.
    
    Additionally, detect repeating sections and mark them accordingly.
    
    Output JSON structure should match this example:
    {json.dumps(JSON_EXAMPLE, indent=4)}
    
    Output only JSON, no explanations.
    """
    
    try:
        response = model.generate_content(prompt)
        logging.debug(f"LLM Response (First 500 chars): {response.text[:500]}...")
        logging.debug(f"LLM Response (Last 500 chars): {response.text[-500:]}")

         # Compose the output filename
        output_filename = f"{base_name}-llm-pdf-extract.json"
        print(f"Output filename: {output_filename}")
        response = response.text.strip("`").lstrip("json") # Remove ``` and json if present

        # *** Save the response to a file ***
        try:
            with open(output_filename, "w", encoding="utf-8") as f:  # Use UTF-8 encoding
                f.write(response)
            logging.info(f"LLM response saved to: {output_filename}")
        except Exception as e:
            logging.error(f"Error saving LLM response to file: {e}")
            # Consider what to do if saving fails - perhaps return the response anyway?


        return response  # Return the processed text

    except Exception as e:
        logging.error(f"Error during LLM processing: {e}")
        return None

def validate_json(json_data):
    """Validates extracted JSON against the schema."""
    try:
        validate(instance=json_data, schema=FORM_SCHEMA)
        return True, "JSON is valid."
    except ValidationError as e:
        return False, f"JSON validation error: {e.message}"


def format_json_with_llm(text, base_name):
    print(f"format_json_with_llm...")

    """Sends extracted json text to Gemini and asks it to format the atrributes of each field in one line."""
    prompt_format_json = f"""
    You are an json expert. Format the following json to be easier to read:
    
    {text}
    
    put the attributes of each field in one line
    
    Output only JSON, no explanations.
    """
    
    try:
        response = model.generate_content(prompt_format_json)
        logging.debug(f"LLM Response (First 500 chars): {response.text[:500]}...")
        logging.debug(f"LLM Response (Last 500 chars): {response.text[-500:]}")

        response = response.text.strip("`").lstrip("json") # Remove ``` and json if present

        # Compose the output filename
        output_filename = f"{base_name}-llm-formated-pdf-extract.json"
        print(f"Output filename: {output_filename}")

        # *** Save the response to a file ***
        try:
            with open(output_filename, "w", encoding="utf-8") as f:  # Use UTF-8 encoding
                f.write(response)
            logging.info(f"LLM response saved to: {output_filename}")
        except Exception as e:
            logging.error(f"Error saving LLM response to file: {e}")

        return response  # Return the processed text

    except Exception as e:
        logging.error(f"Error during LLM processing: {e}")
        return None
    


@app.route('/')
def index():
    return render_template('index.html')


@app.route('/upload', methods=['POST'])
def upload_file():
    if 'file' not in request.files:
        return jsonify({"error": "No file part"}), 400
    file = request.files['file']
    if file.filename == '':
        return jsonify({"error": "No selected file"}), 400
    
    filename = secure_filename(file.filename)
    file_path = os.path.join(app.config['UPLOAD_FOLDER'], filename)
    file.save(file_path)
    
    # Extract the base filename without extension
    base_filename = os.path.basename(filename)
    base_name, _ = os.path.splitext(base_filename)


    extracted_text = extract_text_from_pdf(file_path)
    structured_json = process_text_with_llm(extracted_text, base_name)

    if structured_json is None:
        return jsonify({"error": "LLM processing failed."}), 500


    #logging.debug(f"structured_json (First 500 chars): {structured_json[:500]}...") # Log first 500 chars

    #logging.debug(f"structured_json (Last 500 chars): {structured_json[-500:]}") # Log the last 500

    formated_json = format_json_with_llm(structured_json, base_name)

    logging.debug(f"formated_json : {formated_json}")


    try:
        parsed_json = json.loads(formated_json)
        
        print("converting to CiviForm json ... ") 

        civiform_json = convert_to_civiform_json(parsed_json)

        output_filename = f"{base_name}-civiform.json"
        print(f"Converted JSON saved to {output_filename}")
    
        with open(output_filename, "w") as f:
            f.write(civiform_json)
    
    except json.JSONDecodeError:
        return jsonify({"error": "Failed to parse JSON from AI response."}), 500
    
    # is_valid, validation_message = validate_json(parsed_json)
    # if not is_valid:
     #    return jsonify({"error": validation_message}), 400
    
    print("Done!")
    
    return jsonify(parsed_json)


if __name__ == '__main__':
    app.run(debug=True)
