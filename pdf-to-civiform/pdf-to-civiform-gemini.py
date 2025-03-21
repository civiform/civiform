from google import genai
from google.genai import types
from pathlib import Path
import json
from flask import Flask, request, jsonify, render_template
from werkzeug.utils import secure_filename
import os
import logging
import re
from convert_to_civiform_json import convert_to_civiform_json
from LLM_prompts import LLMPrompts
from io import StringIO
import traceback # Import the traceback module

# This script extracts text from uploaded PDF files, uses Gemini LLM to
# convert the text into structured JSON representing a form, formats the JSON
# for better readability, and then converts it into a CiviForm-compatible
# JSON format.  It uses a Flask web server to handle file uploads.

# make sure you have your gemini API key in ~/google_api_key
# install the latest geminiAPI package: pip install -U -q "google-genai"

# run this script from command line as: python pdf_to_civiform.py
# output files are stored in ~/pdf-to-civiform/

# Configure logging
logging.basicConfig(
    level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')

# Capture logging output for web display
log_stream = StringIO()
log_handler = logging.StreamHandler(log_stream)
log_handler.setFormatter(
    logging.Formatter('%(asctime)s - %(levelname)s - %(message)s'))
logging.getLogger().addHandler(log_handler)

app = Flask(__name__)

work_dir = os.path.expanduser("~/pdf-to-civiform/")
default_upload_dir = os.path.join(
    work_dir, 'uploads')  # Define the default directory
os.makedirs(default_upload_dir, exist_ok=True)
logging.info("Working directory: %s", work_dir)
logging.info(f"Default upload directory: {default_upload_dir}")

# set python cache dir
default_python_cache_path = os.path.join(work_dir, 'python_cache')
if 'PYTHONPYCACHEPREFIX' not in os.environ:
    os.environ['PYTHONPYCACHEPREFIX'] = default_python_cache_path

logging.info(
    f"PYTHONPYCACHEPREFIX  set to: {os.environ['PYTHONPYCACHEPREFIX']}")


def initialize_gemini_model(
    model_name="gemini-2.0-flash",
    api_key=None,
    api_key_file=os.path.expanduser("~/google_api_key")):
    """
    Initializes and configures the Gemini GenerativeModel.

    Args:
        model_name (str): The name of the Gemini model to use.
        api_key (str, optional): API key provided by the user. Defaults to None.
        api_key_file (str): The path to the file containing the Google API key.

    Returns:
        genai.Client: The initialized Gemini client.
    """
    try:
        if api_key:
            GOOGLE_API_KEY = api_key
            logging.info("Using Gemini API key provided in the input box.")
        else:
            try:
                with open(api_key_file, "r") as f:
                    GOOGLE_API_KEY = f.read().strip()
                logging.info("Google API key loaded successfully from file.")
            except FileNotFoundError:
                logging.error(
                    f"Error: Google API key file not found at {api_key_file} and no key provided in input."
                )
                return None
            except Exception as e:
                logging.error(f"Error loading Google API key from file: {e}")
                return None

        client = genai.Client(api_key=GOOGLE_API_KEY)
        return client

    except Exception as e:
        logging.error(f"Error initializing Gemini client: {e}")
        return None


def process_pdf_text_with_llm(client, model_name, file, base_name):
    """Sends extracted PDF text to Gemini and asks it to format the content into structured JSON."""

    prompt = LLMPrompts.pdf_to_json_prompt()
    logging.info(f"LLM processing input txt extracted from PDF...")

    try:
        input_file = types.Part.from_bytes(
            data=file,
            mime_type='application/pdf',
        )
        logging.debug(f"Sending PDF to LLM...")
        response = client.models.generate_content(
            model=model_name, contents=[input_file, prompt])
        response_text = response.text.strip("`").lstrip(
            "json")  # Remove ``` and json if present

        if logging.getLogger().getEffectiveLevel() == logging.DEBUG:
            save_response_to_file(
                response_text, base_name, f"pdf-extract-{model_name}", work_dir)

        return response_text, None  # Return response and None for error

    except Exception as e:
        error_details = f"process_pdf_text_with_llm: {e}"
        logging.error(error_details)
        return None, error_details # Return None for response and the error details


def post_processing_llm(client, model_name, text, base_name):
    """Sends extracted json text to Gemini and asks it to collate related fields into appropriate civiform types, in particular names and address."""
    prompt_post_processing_json = LLMPrompts.post_process_json_prompt(text)

    try:
        logging.info(
            f"post_processing_json_with_llm. collating names, addresses ...")
        logging.debug(f"Sending collating data to LLM: {text}")
        response = client.models.generate_content(
            model=model_name, contents=[prompt_post_processing_json])
        response = response.text.strip("`").lstrip(
            "json")  # Remove ``` and json if present

        if logging.getLogger().getEffectiveLevel() == logging.DEBUG:
            save_response_to_file(
                response, base_name, f"post-processed-{model_name}", work_dir)

        return response  # Return the processed text

    except Exception as e:
        logging.error(f"Error during collating fields: {e}")
        return None


def format_json_single_line_fields(json_string: str) -> str:
    """
    Formats a JSON string to ensure all field attributes (including options)
    are on a single line, while maintaining readability for nested structures.

    Args:
        json_string (str): The JSON string to format.

    Returns:
        str: The formatted JSON string.

    Raises:
        json.JSONDecodeError: If the input string is not valid JSON.
        ValueError: If an unexpected error occurs during formatting.
    """
    try:
        data = json.loads(json_string)

        def custom_dumps(obj, level=0):
            if isinstance(obj, dict):
                if "label" in obj and "type" in obj and "id" in obj:
                    # Format field objects on a single line
                    return json.dumps(
                        obj, separators=(',', ':'), sort_keys=False)
                else:
                    # Format other dictionaries with indentation
                    return "{\n" + "".join(
                        f"{'    ' * (level + 1)}{json.dumps(k, separators=(',', ':'), sort_keys=False)}: {custom_dumps(v, level + 1)},\n"
                        for k, v in obj.items())[:-2] + "\n" + (
                            "    " * level) + "}"
            elif isinstance(obj, list):
                # Format lists with indentation and single-line fields
                return "[\n" + "".join(
                    f"{'    ' * (level + 1)}{custom_dumps(item, level + 1)},\n"
                    for item in obj)[:-2] + "\n" + ("    " * level) + "]"
            else:
                return json.dumps(obj, separators=(',', ':'), sort_keys=False)

        return custom_dumps(data)

    except json.JSONDecodeError as e:
        logging.error(f"Error decoding JSON: {e}")
        raise  # Re-raise the exception to be handled by the caller
    except Exception as e:
        logging.error(f"An unexpected error occurred: {e}")
        raise ValueError(
            f"An unexpected error occurred during JSON formatting: {e}"
        ) from e  # Raise a ValueError


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
        output_file_full = os.path.join(
            work_dir, f"{base_name}-{output_suffix}.json")

        # Remove "```" from both ends and "json" from the start
        response = re.sub(r'^```\s*|\s*```$|\s*json\s*', '', response)

        with open(output_file_full, "w", encoding="utf-8") as f:
            f.write(response)
        logging.info(f"{output_suffix} Response saved to: {output_file_full}")
    except Exception as e:
        logging.error(f"Error saving response to file: {e}")


def process_file(file_full, model_name, client):
    """
    Processes a single PDF file, extracts data, interacts with the LLM, and converts it to CiviForm JSON.

    Args:
        file_full (str): The full path to the PDF file.
        model_name (str): The name of the LLM model to use.
        client : The initialized Gemini client.

    Returns:
        str: The CiviForm JSON string.
    Raises:
        Exception: If LLM processing or post-processing fails.
    """
    try:
        # Extract the base filename without extension
        filename = os.path.basename(file_full)
        base_name, _ = os.path.splitext(filename)
        base_name = base_name[:
                              15]  # limit to 15 chars to avoid extremely long filenames
        logging.info(f"Processing file: {file_full} ...")

        filepath = Path(file_full)
        file_bytes = filepath.read_bytes()
        structured_json, llm_error = process_pdf_text_with_llm(
            client, model_name, file_bytes, base_name)

        if structured_json is None:
            raise Exception(f"LLM processing failed for file: {file_full}. Details: {llm_error}")

        logging.info(f"Formating json  .... ")
        formated_json = format_json_single_line_fields(structured_json)
        save_response_to_file(
            formated_json, base_name, f"formated-{model_name}", work_dir)

        post_processed_json = post_processing_llm(
            client, model_name, formated_json, base_name)
        if post_processed_json is None:
            raise Exception(f"LLM post-processing failed for file: {file_full}")

        logging.info(f"Formating post processed json  .... ")
        formated_post_processed_json = format_json_single_line_fields(
            post_processed_json)
        save_response_to_file(
            formated_post_processed_json, f"{base_name}-post-processed",
            f"formated-{model_name}", work_dir)

        parsed_json = json.loads(formated_post_processed_json)
        civiform_json = convert_to_civiform_json(parsed_json)
        save_response_to_file(
            civiform_json, base_name, f"civiform-{model_name}", work_dir)
        logging.info(f"Done processing file: {file_full}")

        return civiform_json
    except Exception as e:
        logging.error(f"Failed to process file {file_full}: {e}")
        raise # Re-raise the exception to be caught in the route


@app.route('/')
def index():
    return render_template('index.html', debug_log="")


@app.route('/upload', methods=['POST'])
def upload_file():
    try:
        if 'file' not in request.files:
            return jsonify({"error": "No file part"}), 400
        file = request.files['file']
        if file.filename == '':
            return jsonify({"error": "No selected file"}), 400

        filename = secure_filename(file.filename)
        file_full = os.path.join(default_upload_dir, filename)
        file.save(file_full)

        # Get LLM model name, API key, and log level from the request
        model_name = request.form.get('modelName')
        gemini_api_key = request.form.get('geminiApiKey')
        log_level_str = request.form.get('logLevel', 'INFO').upper()
        log_level = getattr(logging, log_level_str, logging.INFO)
        logging.getLogger().setLevel(log_level)
        logging.info(f"Log level set to: {logging.getLevelName(log_level)}")
        logging.debug(f"log_level_str passed in: {log_level_str}")

        client = initialize_gemini_model(model_name, api_key=gemini_api_key)
        if client is None:
            error_message = "Failed to initialize Gemini client. Check API key or file."
            logging.error(error_message)
            return jsonify({"error": error_message}), 500

        civiform_json = process_file(file_full, model_name, client)

        # Log the length
        logging.info(f"Length of civiform_json: {len(civiform_json)}")

        return jsonify(civiform_json)

    except Exception as e:
        error_message = f"An error occurred during file upload: {e}"
        logging.error(f"{error_message}\n{traceback.format_exc()}") # Log full traceback
        return jsonify({"error": error_message, "details": traceback.format_exc()}), 500


def process_directory(directory, model_name, client):
    """
    Processes all PDF files in a given directory and returns summary information.

    Args:
        directory (str): The path to the directory containing PDF files.
        model_name (str): The name of the LLM model to use.
        client: The initialized Gemini client.

    Returns:
        dict: Dictionary containing summary details (total, success, fail, file_results).
    """
    success_count = 0
    fail_count = 0
    total_files = 0
    # Check if the directory is outside the allowed working directory
    if not os.path.abspath(directory).startswith(os.path.abspath(work_dir)):
        logging.error(
            f"Attempted access outside working directory: {directory}")
        return {
            "total_files": 0,
            "success_count": 0,
            "fail_count": 0,
            "file_results": {}
        }

    file_results = {}

    for filename in os.listdir(directory):
        if filename.lower().endswith(".pdf"):
            total_files += 1
            file_full = os.path.join(directory, filename)
            file_results[filename] = {"success": False, "error_message": ""}
            try:
                civiform_json = process_file(file_full, model_name, client)
                if civiform_json:
                    success_count += 1
                    file_results[filename]["success"] = True
                else:
                    fail_count += 1
                    file_results[filename]["error_message"] = "Failed to process file."
            except Exception as e:
                fail_count += 1
                error_message = f"Error processing {filename}: {e}"
                file_results[filename]["error_message"] = error_message
                logging.error(f"{error_message}\n{traceback.format_exc()}")


    # Prepare the debug log string
    debug_log = log_stream.getvalue()
    log_stream.seek(0)
    log_stream.truncate(0)

    return {
        "total_files": total_files,
        "success_count": success_count,
        "fail_count": fail_count,
        "file_results": file_results,
        "debug_log": debug_log
    }


@app.route('/upload_directory', methods=['POST'])
def upload_directory():
    """
    Endpoint to process a directory of files and return a summary.
    """
    log_stream.seek(0)
    log_stream.truncate(0)

    # Get LLM model name, API key, and log level from the request
    model_name = request.form.get('modelName')
    gemini_api_key = request.form.get('geminiApiKey')
    log_level_str = request.form.get('logLevel', 'INFO').upper()
    log_level = getattr(logging, log_level_str, logging.INFO)
    logging.getLogger().setLevel(log_level)
    logging.info(f"Log level set to: {logging.getLevelName(log_level)}")
    logging.debug(f"log_level_str passed in: {log_level_str}")

    directory_path = request.form.get(
        'directoryPath', default_upload_dir)  # Get path or default

    # Sanitize the directory path
    directory_path = os.path.expanduser(
        directory_path)  # Expand ~ and other special chars
    directory_path = os.path.abspath(directory_path)  # Convert to absolute path

    # Basic check to prevent accessing outside of the work dir
    if not directory_path.startswith(work_dir):
        logging.error(
            f"Requested directory is outside of the work directory: {directory_path}"
        )
        return jsonify({"error": "Invalid directory path."}), 400

    # Ensure that the requested path is a directory
    if not os.path.isdir(directory_path):
        logging.error(f"Invalid directory path: {directory_path}")
        return jsonify({"error": "Invalid directory path."}), 400

    client = initialize_gemini_model(model_name, api_key=gemini_api_key)
    if client is None:
        error_message = "Failed to initialize Gemini client. Check API key or file."
        logging.error(error_message)
        return jsonify({"error": error_message}), 500

    try:
        directory_result = process_directory(directory_path, model_name, client)
        debug_log = directory_result.get("debug_log", "No debug log captured.")

        response_data = {
            "summary": {
                "total_files": directory_result["total_files"],
                "success_count": directory_result["success_count"],
                "fail_count": directory_result["fail_count"],
                "file_results": directory_result["file_results"]
            },
            "debug_log": debug_log
        }
        return jsonify(response_data)
    except Exception as e:
        error_message = f"An error occurred during directory processing: {e}"
        logging.error(f"{error_message}\n{traceback.format_exc()}")
        return jsonify({"error": error_message, "details": traceback.format_exc()}), 500


if __name__ == '__main__':
    app.run(debug=True, host="0.0.0.0", port=int(os.environ.get("PORT", 7000)))