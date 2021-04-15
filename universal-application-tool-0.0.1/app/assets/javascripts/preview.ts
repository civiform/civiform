/** The preview controller is responsible for updating quesiton preview text in the question builder. */
class PreviewController {
  static readonly QUESTION_TEXT_INPUT_ID = 'question-text-textarea';
  static readonly QUESTION_HELP_TEXT_INPUT_ID = 'question-help-text-textarea';

  static readonly QUESTION_TEXT_CLASS = '.cf-applicant-question-text';
  static readonly QUESTION_HELP_TEXT_CLASS = '.cf-applicant-question-help-text';

  static readonly DEFAULT_QUESTION_TEXT = "Sample question text";
  static readonly DEFAULT_QUESTION_HELP_TEXT = "Sample question help text";

  constructor() {
    const textInput =
      document.getElementById(PreviewController.QUESTION_TEXT_INPUT_ID);
    if (textInput) {
      textInput.addEventListener('input', PreviewController.onTextChanged, false);
      let text = (<HTMLInputElement>textInput).value;
      if (text.length > 0) {
        PreviewController.setTextContent(PreviewController.QUESTION_TEXT_CLASS, text);
      }
    }
    const helpTextInput =
      document.getElementById(PreviewController.QUESTION_HELP_TEXT_INPUT_ID);
    if (helpTextInput) {
      helpTextInput.addEventListener('input', PreviewController.onHelpTextChanged, false);
      let helpText = (<HTMLInputElement>helpTextInput).value;
      if (helpText.length > 0) {
        PreviewController.setTextContent(PreviewController.QUESTION_HELP_TEXT_CLASS, helpText);
      }
    }
  }

  static onTextChanged(e: Event) {
    let text = (<HTMLInputElement>e.target).value;
    if (text.length === 0) {
      text = PreviewController.DEFAULT_QUESTION_TEXT;
    }

    PreviewController.setTextContent(
      PreviewController.QUESTION_TEXT_CLASS,
      text);
  }

  static onHelpTextChanged(e: Event) {
    let text = (<HTMLInputElement>e.target).value;
    if (text.length === 0) {
      text = PreviewController.DEFAULT_QUESTION_HELP_TEXT;
    }

    PreviewController.setTextContent(
      PreviewController.QUESTION_HELP_TEXT_CLASS,
      text);
  }

  static setTextContent(selector: string, text: string) {
    const previewDiv = document.querySelector(selector);
    if (previewDiv) {
      previewDiv.textContent = text;
    }
  }

}

new PreviewController();
