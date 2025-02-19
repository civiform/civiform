import pdfplumber
import google.generativeai as genai
import json
from flask import Flask, request, jsonify, render_template
from werkzeug.utils import secure_filename
import os
import logging
from convert_to_civiform_json import convert_to_civiform_json 
import re

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

logging.info(f"PYTHONPYCACHEPREFIX  set to: {os.environ['PYTHONPYCACHEPREFIX']}")

def initialize_gemini_model(model_name="gemini-2.0-flash-thinking-exp", api_key_file=os.path.expanduser("~/google_api_key")):
    """
    Initializes and configures the Gemini GenerativeModel.

    Args:
        model_name (str): The name of the Gemini model to use.
        api_key_file (str): The path to the file containing the Google API key.

    Returns:
        genai.GenerativeModel: The initialized Gemini model.
    """
    try:
        with open(api_key_file, "r") as f:
            GOOGLE_API_KEY = f.read().strip()
        genai.configure(api_key=GOOGLE_API_KEY)
        logging.info("Google API key loaded successfully.")

        model = genai.GenerativeModel(f"models/{model_name}")

        # Print available Gemini models
        logging.info("Available Gemini models: %s", [model_info.name for model_info in genai.list_models()])
        logging.info(f"INFO: Gemini model used: Model name: {model_name} version: {genai.__version__};")

        return model

    except FileNotFoundError:
        logging.error(f"Error: Google API key file not found at {api_key_file}.")
        exit(1)
    except Exception as e:
        logging.error(f"Error loading Google API key: {e}")
        exit(1)


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
    logging.info(f"Extracts text from a given PDF file: {pdf_path}")
    """Extracts text from a given PDF file while maintaining basic layout structure."""
    text_blocks = []
    with pdfplumber.open(pdf_path) as pdf:
        for page in pdf.pages:
            text = page.extract_text()
            if text:
                text_blocks.append(text)
    return "\n\n".join(text_blocks)

def process_pdf_text_with_llm(model, model_name, text, base_name):
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

        save_response_to_file(response, base_name, f"pdf-extract-{model_name}", work_dir)

        return response  # Return the processed text
 
    except Exception as e:
        logging.error(f"process_pdf_text_with_llm: {e}")
        return None


def post_processing_llm(model, model_name, text, base_name):

    """Sends extracted json text to Gemini and asks it to collate related fields into appropriate civiform types, in particular names and address."""
    prompt_post_processing_json = f"""
    You are an expert in government forms.  Adapt the following extracted json from a government form to be easier to use:
    
    {text}
    
    If you find separate address related fields for Unit, city, zip code, street etc, collate them into a single 'address' field if possible. Do not create separate fields for address components.
    If you find separate fields for first name, middle name, and last name, collate them into a single 'name' field if possible. Do not create separate fields for name components.
    if you find duplicate fields within the same section asking for similar information, create a separate repeating_section for them.
    For each repeating_section, create an "entity_nickname" field which best describes the entity that the repeating entries are about.
    If a section contains fields of repeating_section and non repeating_section fields, separate them into individual sections
    

    Output only JSON, no explanations.
    """
    
    try:
  
        logging.info(f"post_processing_json_with_llm. collating names, addresses ...")
        response = model.generate_content(prompt_post_processing_json)
        logging.debug(f"LLM Response (Last 500 chars): {response.text[-500:]}")

        response = response.text.strip("`").lstrip("json") # Remove ``` and json if present

        save_response_to_file(response, base_name, f"post-processed-{model_name}", work_dir)

        return response  # Return the processed text

    except Exception as e:
        logging.error(f"Error during collating fields: {e}")
        return None

# collate names, address, etc

def format_json(model, model_name, text, base_name, use_llm=True):

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
            logging.info(f"Not yet implemented .... ")


        save_response_to_file(response, base_name, f"formated-{model_name}", work_dir)

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

        # Remove "```" from both ends and "json" from the start
        response = re.sub(r'^```\s*|\s*```$|\s*json\s*', '', response)

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


    # Get LLM model name from the request
    model_name = request.form.get('modelName')

    model = initialize_gemini_model(model_name)

    
    # Extract the base filename without extension
    base_name, _ = os.path.splitext(os.path.basename(filename))

    extracted_text = extract_text_from_pdf(file_full)
    structured_json = process_pdf_text_with_llm(model, model_name, extracted_text, base_name)

    if structured_json is None:
        return jsonify({"error": "LLM processing failed."}), 500

    formated_json = format_json(model, model_name, structured_json, base_name, use_llm=True)
    post_processed_json = post_processing_llm(model, model_name, formated_json, base_name)
    formated_post_processed_json = format_json(model, model_name, post_processed_json, f"{base_name}-formated-post-processed", use_llm=True)

    try:
        parsed_json = json.loads(formated_post_processed_json)
    
        civiform_json = convert_to_civiform_json(parsed_json)

        save_response_to_file(civiform_json, base_name, f"-civiform-{model_name}", work_dir)

      #  output_file_full = os.path.join(work_dir, f"{base_name}-{model_name}-civiform.json")
      #  logging.info(f"Converted JSON saved to {output_file_full}")


    except json.JSONDecodeError:
        return jsonify({"error": "Failed to parse JSON from LLM response."}), 500
    
    
    logging.info("Done!")
    
    return jsonify(json.loads(civiform_json))


if __name__ == '__main__':
    app.run(debug=True)
