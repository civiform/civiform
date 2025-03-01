import pdfplumber
import google.generativeai as genai
import json
from flask import Flask, request, jsonify, render_template
from werkzeug.utils import secure_filename
import os
import logging
import re
from convert_to_civiform_json import convert_to_civiform_json
from LLM_prompts import LLMPrompts


# This script extracts text from uploaded PDF files, uses Gemini LLM to
# convert the text into structured JSON representing a form, formats the JSON
# for better readability, and then converts it into a CiviForm-compatible
# JSON format.  It uses a Flask web server to handle file uploads.

# make sure you have your gemini API key in ~/google_api_key

# run this script from command line as: python pdf_to_civiform.py
# output files are stored in ~/pdf-to-civiform/

# known iisues/limitations:
#    * Radio button questions may extracted as checkbox questions:
#       ex: marital status in  https://www.miamidade.gov/housing/library/forms/condominium-special-assessments-program-application.pdf
#    * Not reliably identifying number fields. Some numeric fields are recognized as text fields.
#    * not all help text is extracted/assocated with applicable question
#    * all questions are marked as required


# Configure logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')

app = Flask(__name__)

work_dir = os.path.expanduser("~/pdf-to-civiform/")
upload_dir = os.path.join(work_dir, 'uploads')
os.makedirs(upload_dir, exist_ok=True)
logging.info("Working directory: %s", work_dir)

# set python cache dir
default_python_cache_path = os.path.join(work_dir, 'python_cache')
if 'PYTHONPYCACHEPREFIX' not in os.environ:
    os.environ['PYTHONPYCACHEPREFIX'] = default_python_cache_path

logging.info(f"PYTHONPYCACHEPREFIX  set to: {os.environ['PYTHONPYCACHEPREFIX']}")


def initialize_gemini_model(model_name="gemini-2.0-flash-thinking-exp",
                             api_key_file=os.path.expanduser("~/google_api_key")):
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

        # Print available Gemini models
        print("\n".join(
            m.name for m in genai.list_models() if 'generateContent' in m.supported_generation_methods))

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

    prompt = LLMPrompts.pdf_to_json_prompt(text)
    logging.info(f"LLM processing input txt extracted from PDF...")

    try:
        response = model.generate_content(prompt)
        response = response.text.strip("`").lstrip("json")  # Remove ``` and json if present

        if logging.getLogger().getEffectiveLevel() == logging.DEBUG:
            save_response_to_file(response, base_name, f"pdf-extract-{model_name}", work_dir)

        return response  # Return the processed text

    except Exception as e:
        logging.error(f"process_pdf_text_with_llm: {e}")
        return None


def post_processing_llm(model, model_name, text, base_name):
    """Sends extracted json text to Gemini and asks it to collate related fields into appropriate civiform types, in particular names and address."""
    prompt_post_processing_json = LLMPrompts.post_process_json_prompt(text)

    try:
        logging.info(f"post_processing_json_with_llm. collating names, addresses ...")
        response = model.generate_content(prompt_post_processing_json)
        response = response.text.strip("`").lstrip("json")  # Remove ``` and json if present

        if logging.getLogger().getEffectiveLevel() == logging.DEBUG:
            save_response_to_file(response, base_name, f"post-processed-{model_name}", work_dir)

        return response  # Return the processed text

    except Exception as e:
        logging.error(f"Error during collating fields: {e}")
        return None

def format_json_single_line_fields(json_string: str) -> str:
    """
    formats it to ensure all field attributes
    (including options) are on a single line, and returns the formatted string.
    """
    try:
        data = json.loads(json_string)

        def custom_dumps(obj, level=0):
            if isinstance(obj, dict):
                if "label" in obj and "type" in obj:
                    return json.dumps(obj, separators=(',', ': '))
                else:
                    return "{\n" + "".join(
                        f"{'    ' * (level + 1)}{json.dumps(k, separators=(',', ': '))}: {custom_dumps(v, level + 1)},\n"
                        for k, v in obj.items()
                    )[:-2] + "\n" + ("    " * level) + "}"
            elif isinstance(obj, list):
                return "[\n" + "".join(
                    f"{'    ' * (level + 1)}{custom_dumps(item, level + 1)},\n"
                    for item in obj
                )[:-2] + "\n" + ("    " * level) + "]"
            else:
                return json.dumps(obj, separators=(',', ': '))

        return custom_dumps(data)

    except json.JSONDecodeError as e:
        print(f"Error decoding JSON: {e}")
        return ""  # Return empty string on error
    except Exception as e:
        print(f"An unexpected error occurred: {e}")
        return ""


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
    base_name = base_name[:15]  # limit to 15 chars to avoid extremely long filenames

    try:
        extracted_text = extract_text_from_pdf(file_full)
        structured_json = process_pdf_text_with_llm(model, model_name, extracted_text,
                                                   base_name)

        if structured_json is None:
            return jsonify({"error": "LLM processing failed."}), 500

        logging.info(f"Formating json  .... ")
        formated_json = format_json_single_line_fields(structured_json)
        save_response_to_file(formated_json, base_name, f"formated-{model_name}", work_dir)

        post_processed_json = post_processing_llm(model, model_name, formated_json, base_name)
        
        logging.info(f"Formating post processed json  .... ")
        formated_post_processed_json = format_json_single_line_fields(post_processed_json)
        save_response_to_file(formated_post_processed_json, f"{base_name}-post-processed", f"formated-{model_name}", work_dir)

        parsed_json = json.loads(formated_post_processed_json)
        civiform_json = convert_to_civiform_json(parsed_json)
        save_response_to_file(civiform_json, base_name, f"civiform-{model_name}", work_dir)

    except Exception as e:
        return jsonify({"error": f"Failed to parse JSON from LLM response: {e}"}), 500

    logging.info("Done!")

    return jsonify(json.loads(civiform_json))


if __name__ == '__main__':
    app.run(debug=True)
