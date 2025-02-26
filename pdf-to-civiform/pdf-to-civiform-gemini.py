import pdfplumber
import google.generativeai as genai
import json
from flask import Flask, request, jsonify, render_template
from werkzeug.utils import secure_filename
import os
import logging
from convert_to_civiform_json import convert_to_civiform_json
from prompts import pdf_extract_v1, post_process_v1, format_json_v1
import re

# This script extracts text from uploaded PDF files, uses Gemini LLM to 
# convert the text into structured JSON representing a form, formats the JSON
# for better readability, and then converts it into a CiviForm-compatible 
# JSON format.  It uses a Flask web server to handle file uploads.

# make sure you have your gemini API key in ~/google_api_key

# run this script from command line as: python pdf_to_civiform.py
# output files are stored in ~/pdf-to-civiform/

# TODOs
# 1. refactor to use code for formatting json. LLM is not needed for this step and is slow


# known iisues/limitations: 
#    * redio button questions were extracted as checkbox questions: 
#       ex: marital status in  https://www.miamidade.gov/housing/library/forms/condominium-special-assessments-program-application.pdf
#    * Not reliably identifying number fields. treated as text fields.
#    * conditional questions are treated as a separate question and not conditionally related to other questions
#    * not all help text is extracted/assocated with applicable question
#    * all questions are treated as required


# Configure logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')

app = Flask(__name__)

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
        logging.info(f"INFO: Gemini model used: Model name: {model_name} version: {genai.__version__};")

        return model

    except FileNotFoundError:
        logging.error(f"Error: Google API key file not found at {api_key_file}.")
        exit(1)
    except Exception as e:
        logging.error(f"Error loading Google API key: {e}")
        exit(1)


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
    prompt = pdf_extract_v1.get_prompt(text)
    logging.info(f"LLM processing input text extracted from PDF with prompt:{prompt}")

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
    #  TODO: could not reliablely move repeating sections out of sections without LLM creating unnecessary repeating sections.   
    #  If a "repeating_section" is inside a section, move it out to a separate section. Do not create new repeating section for single entries. Do not create new repeating section if the entries are not already in a "repeating_section".

    prompt_post_processing_json = post_process_v1.get_prompt(text)
    logging.info(f"post processing prompt:{prompt_post_processing_json}")
    
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

# TODO:
# 1. LLM is not really needed and too slow for this step. It can be done with a simple python script. 

def format_json(model, model_name, text, base_name, use_llm=True):

    """Sends extracted json text to Gemini and asks it to format the atrributes of each field in one line."""
    prompt_format_json = format_json_v1.get_prompt(text)
    
    try:
        if use_llm:
            logging.info(f"format_json_with_llm with prompt:{prompt_format_json}")
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
