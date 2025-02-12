import pdfplumber
import google.generativeai as genai
import json
from flask import Flask, request, jsonify, render_template
from werkzeug.utils import secure_filename
import os
import logging
from convert_to_civiform_json import convert_to_civiform_json 

# This script extracts text from uploaded PDF files, uses Gemini LLM to 
# convert the text into structured JSON representing a form, formats the JSON
# for better readability, and then converts it into a CiviForm-compatible 
# JSON format.  It uses a Flask web server to handle file uploads.

# make sure you have your gemini API key in ~/google_api_key

# run this script from command line as: python pdf_to_civiform.py
# output files are stored in ~/pdf-to-civiform/

# Configure logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')

app = Flask(__name__)

# Read Google API key from file
GOOGLE_API_KEY_FILE = os.path.expanduser("~/google_api_key")  # Use expanduser for home directory
model = genai.GenerativeModel("models/gemini-2.0-flash-exp")

try:
    with open(GOOGLE_API_KEY_FILE, "r") as f:
        GOOGLE_API_KEY = f.read().strip()
    genai.configure(api_key=GOOGLE_API_KEY)
    logging.info("Google API key loaded successfully.")
except FileNotFoundError:
    logging.error(f"Error: Google API key file not found at {GOOGLE_API_KEY_FILE}.")
    # Exit or handle the error appropriately for your application
    exit(1)  # Example: exit the application
except Exception as e:
    logging.error(f"Error loading Google API key: {e}")
    # Exit or handle the error


# app.config['WORK_DIR'] = '~/pdf-to-civiform/'

# work_dir: ~/pdf-to-civiform/
# temporary upload dir: ~/pdf-to-civiform/uploads
work_dir = os.path.expanduser("~/pdf-to-civiform/")
upload_dir = os.path.join(work_dir, 'uploads')
os.makedirs(upload_dir, exist_ok=True)
logging.info("Working directory: %s", work_dir)

# set python cache dir
default_python_cache_path=os.path.join(work_dir, 'python_cache')
if 'PYTHONPYCACHEPREFIX' not in os.environ:
    os.environ['PYTHONPYCACHEPREFIX'] = default_python_cache_path

print(f"PYTHONPYCACHEPREFIX  set to: {os.environ['PYTHONPYCACHEPREFIX']}")

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

def process_text_with_llm(text, base_name):
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

    If you can, consider the following rules to assign input field types:
    1. **Address**: address (e.g., residential, work, mailing). Collate Unit, city, zip code, street etc fields if possible.
    2. **Currency**: Currency values with decimal separators (e.g., income, debts).
    4. **Date**: Captures dates (e.g., birth date, graduation date).
    6. **Email**: Applicant’s email address. Collate domain and username if asked separately.
    8. **File Upload**: File attachments (e.g., PDFs, images). 
    10. **Name**: Applicant’s full legal name, or first or middle or last name. Collate them if possible.
    15. **Phone**: phone numbers

    
    Output JSON structure should match this example:
    {json.dumps(JSON_EXAMPLE, indent=4)}
    
    Output only JSON, no explanations.
    """
    
    logging.info(f"LLM processing input txt extracted from PDF...")

    try:
        response = model.generate_content(prompt)
        logging.debug(f"LLM Response (First 500 chars): {response.text[:500]}...")
        logging.debug(f"LLM Response (Last 500 chars): {response.text[-500:]}")
        response = response.text.strip("`").lstrip("json") # Remove ``` and json if present

        output_file_full = os.path.join(work_dir, f"{base_name}-llm-pdf-extract.json")

        # *** Save the response to a file ***
        try:
            with open(output_file_full, "w", encoding="utf-8") as f:  # Use UTF-8 encoding
                f.write(response)
            logging.info(f"LLM response saved to: {output_file_full}")
        except Exception as e:
            logging.error(f"Error saving LLM response to file: {e}")

        return response  # Return the processed text

    except Exception as e:
        logging.error(f"Error during LLM processing: {e}")
        return None


def format_json_with_llm(text, base_name):
    logging.info(f"format_json_with_llm...")

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

        # *** Save the response to a file ***
        try:
            output_file_full = os.path.join(work_dir, f"{base_name}-llm-formated-pdf-extract.json")
            with open(output_file_full, "w", encoding="utf-8") as f:  # Use UTF-8 encoding
                f.write(response)
            logging.info(f"LLM response saved to: {output_file_full}")
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
    file_full = os.path.join(upload_dir, filename)
    file.save(file_full)
    
    # Extract the base filename without extension
    base_name, _ = os.path.splitext(os.path.basename(filename))


    extracted_text = extract_text_from_pdf(file_full)
    structured_json = process_text_with_llm(extracted_text, base_name)

    if structured_json is None:
        return jsonify({"error": "LLM processing failed."}), 500

    formated_json = format_json_with_llm(structured_json, base_name)

    try:
        parsed_json = json.loads(formated_json)
    
        civiform_json = convert_to_civiform_json(parsed_json)

        output_file_full = os.path.join(work_dir, f"{base_name}-civiform.json")
        logging.info(f"Converted JSON saved to {output_file_full}")
    
        with open(output_file_full, "w") as f:
            f.write(civiform_json)
    
    except json.JSONDecodeError:
        return jsonify({"error": "Failed to parse JSON from AI response."}), 500
    
    
    logging.info("Done!")
    
    return jsonify(parsed_json)


if __name__ == '__main__':
    app.run(debug=True)
