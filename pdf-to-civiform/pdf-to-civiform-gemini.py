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
# known limitations: 
#    * conditional questions are treated as a separate question and not conditionally related to other questions
#    * not all help text is extracted/assocated with applicable question
#    * all questions are treated as required


# Configure logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')

app = Flask(__name__)

# Read Google API key from file
GOOGLE_API_KEY_FILE = os.path.expanduser("~/google_api_key")  # Use expanduser for home directory
model = genai.GenerativeModel("models/gemini-2.0-flash-exp")
logging.info(f"INFO: google.generativeai version: {genai.__version__}")


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

    make sure to consider the following rules to extract input fields and types:
    1. **Address**: If you find separate address related fields for Unit, city, zip code, street etc, collate them into a single 'address' field if possible.
    2. **Currency**: Currency values with decimal separators (e.g., income, debts).
    3. **Checkbox**: collate options for checkboxes as one field of "checkbox" type if possible.
    4. **Date**: Captures dates (e.g., birth date, graduation date).
    6. **Email**: Applicant’s email address. Collate domain and username if asked separately.
    8. **File Upload**: File attachments (e.g., PDFs, images). 
    9. **Name**: If you find separate fields for first name, middle name, and last name, collate them into a single 'name' field in the JSON output.
    10. **Phone**: phone numbers

    If you see a field you do not understand, please use "unknown" as the type, associate relevant text as help text and assign a unique ID.
    If you find separate address related fields for Unit, city, zip code, street etc, collate them into a single 'address' field if possible.
    If you find separate fields for first name, middle name, and last name, collate them into a single 'name' field .

    
    Output JSON structure should match this example:
    {json.dumps(JSON_EXAMPLE, indent=4)}
    
    Output only JSON, no explanations.
    """
    
    logging.info(f"LLM processing input txt extracted from PDF...")

    try:
        response = model.generate_content(prompt )
        logging.debug(f"LLM Response (First 500 chars): {response.text[:500]}...")
        logging.debug(f"LLM Response (Last 500 chars): {response.text[-500:]}")
        response = response.text.strip("`").lstrip("json") # Remove ``` and json if present

        save_response_to_file(response, base_name, "-llm-pdf-extract", work_dir)

        return response  # Return the processed text

    except Exception as e:
        logging.error(f"Error during LLM processing: {e}")
        return None


def post_processing_llm(text, base_name):

    """Sends extracted json text to Gemini and asks it to collate related fields into appropriate civiform types, in particular names and address."""
    prompt_format_json = f"""
    You are an expert in government forms.  Adapt the following extracted json from a government form to be easier to use:
    
    {text}
    
    If you find separate address related fields for Unit, city, zip code, street etc, collate them into a single 'address' field if possible.
    If you find separate fields for first name, middle name, and last name, collate them into a single 'name' field if possible.
    if you find duplicate fields within the same section asking for similar information, create a separate repeating_section for them.
    For each repeating_section, create an "entity_nickname" field which best describes the entity that the repeating entries are about
    If a section contains fields of repeating_section and non repeating_section fields, separate them into individual sections
    
    Output only JSON, no explanations.
    """
    
    try:
  
        logging.info(f"post_processing_json_with_llm. collating names, addresses ...")
        response = model.generate_content(prompt_format_json)
        logging.debug(f"LLM Response (First 500 chars): {response.text[:500]}...")
        logging.debug(f"LLM Response (Last 500 chars): {response.text[-500:]}")
        response = response.text.strip("`").lstrip("json") # Remove ``` and json if present

        save_response_to_file(response, base_name, "-llm-adapted", work_dir)

        return response  # Return the processed text

    except Exception as e:
        logging.error(f"Error during collating fields: {e}")
        return None

# collate names, address, etc

def format_json(text, base_name, use_llm=True):

    """Sends extracted json text to Gemini and asks it to format the atrributes of each field in one line."""
    prompt_format_json = f"""
    You are an json expert. Format the following json to be easier to read:
    
    {text}
    
    put the attributes of each field in one line
    
    Output only JSON, no explanations.
    """
    
    try:
        if use_llm:
            logging.info(f"format_json_with_llm...")
            response = model.generate_content(prompt_format_json)
            logging.debug(f"LLM Response (First 500 chars): {response.text[:500]}...")
            logging.debug(f"LLM Response (Last 500 chars): {response.text[-500:]}")
            response = response.text.strip("`").lstrip("json") # Remove ``` and json if present

        else:
            logging.info(f"format_json_with_python...")
            response = format_json_single_line_fields(text)

        save_response_to_file(response, base_name, "-formated", work_dir)

        return response  # Return the processed text

    except Exception as e:
        logging.error(f"Error during formatting json: {e}")
        return None

def format_json_single_line_fields(input_json: str) -> str:
    """
    Formats a JSON string to ensure:
    - Each field's attributes stay on one line.
    - Each field appears on a separate line.

    Args:
        input_json (str): The input JSON string.

    Returns:
        str: The formatted JSON string.
    """
    # TODO: This function is a placeholder and needs to be implemented.

def save_response_to_file(response, base_name, output_suffix, work_dir):
    """
    Saves a given response string to a file.

    Args:
        response (str): The string to be saved to the file.
        base_name (str): The base name of the file.
        output_suffix (str): The suffix to append to the filename.
        work_dir (str): The working directory where the file will be saved.
    """
    try:
        output_file_full = os.path.join(work_dir, f"{base_name}-{output_suffix}.json")
        with open(output_file_full, "w", encoding="utf-8") as f:
            f.write(response)
        logging.info(f"{output_suffix} Response saved to: {output_file_full}")
    except Exception as e:
        logging.error(f"Error saving response to file: {e}")

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

    formated_json = format_json(structured_json, base_name, use_llm=True)
    post_processed_json = post_processing_llm(formated_json, base_name)

    try:
        parsed_json = json.loads(post_processed_json)
    
        civiform_json = convert_to_civiform_json(parsed_json)

        save_response_to_file(civiform_json, base_name, "-civiform", work_dir)

        output_file_full = os.path.join(work_dir, f"{base_name}-civiform.json")
        logging.info(f"Converted JSON saved to {output_file_full}")


    except json.JSONDecodeError:
        return jsonify({"error": "Failed to parse JSON from AI response."}), 500
    
    
    logging.info("Done!")
    
    return jsonify(json.loads(civiform_json))


if __name__ == '__main__':
    app.run(debug=True)
