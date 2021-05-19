/** The preview controller is responsible for updating question preview text in the question builder. */
class PreviewController {
  static readonly QUESTION_TEXT_INPUT_ID = 'question-text-textarea';
  static readonly QUESTION_HELP_TEXT_INPUT_ID = 'question-help-text-textarea';
  static readonly QUESTION_ENUMERATOR_INPUT_ID = 'question-enumerator-select';
  static readonly QUESTION_ENTITY_TYPE_INPUT_ID = 'enumerator-question-entity-type-input';

  static readonly QUESTION_TEXT_CLASS = '.cf-applicant-question-text';
  static readonly QUESTION_HELP_TEXT_CLASS = '.cf-applicant-question-help-text';
  static readonly REPEATED_QUESTION_INFORMATION_ID = '#repeated-question-information';
  static readonly QUESTION_ENTITY_TYPE_BUTTON_ID = '#enumerator-field-add-button';
  static readonly QUESTION_ENTITY_NAME_INPUT_CLASS = '.cf-entity-name-input';

  static readonly DEFAULT_QUESTION_TEXT = "Sample question text";
  static readonly DEFAULT_QUESTION_HELP_TEXT = "Sample question help text";
  static readonly DEFAULT_ENTITY_TYPE = "Sample repeated entity type";

  // This regex is used to match $this and $this.parent (etc) strings so we can
  // highlight them in the question preview.
  static readonly THIS_REGEX = /(\$this(?:\.parent)*)/g;

  constructor() {
    const textInput =
      document.getElementById(PreviewController.QUESTION_TEXT_INPUT_ID);
    if (textInput) {
      textInput.addEventListener('input', PreviewController.onTextChanged, false);
      let text = (<HTMLInputElement>textInput).value;
      if (text.length > 0) {
        PreviewController.setChildrenTextNodes(PreviewController.QUESTION_TEXT_CLASS, text);
      }
    }
    const helpTextInput =
      document.getElementById(PreviewController.QUESTION_HELP_TEXT_INPUT_ID);
    if (helpTextInput) {
      helpTextInput.addEventListener('input', PreviewController.onHelpTextChanged, false);
      let helpText = (<HTMLInputElement>helpTextInput).value;
      if (helpText.length > 0) {
        PreviewController.setChildrenTextNodes(PreviewController.QUESTION_HELP_TEXT_CLASS, helpText);
      }
    }
    const enumeratorSelector = document.getElementById(PreviewController.QUESTION_ENUMERATOR_INPUT_ID);
    if (enumeratorSelector) {
      enumeratorSelector.addEventListener('input', PreviewController.onEnumeratorSelectorChanged, false);
      let enumerator = (<HTMLInputElement>enumeratorSelector).value;
      const repeatedQuestionInformation = document.querySelector(PreviewController.REPEATED_QUESTION_INFORMATION_ID);
      repeatedQuestionInformation.classList.toggle("hidden", enumerator === "");
    }
    const entityTypeInput =
      document.getElementById(PreviewController.QUESTION_ENTITY_TYPE_INPUT_ID);
    if (entityTypeInput) {
      entityTypeInput.addEventListener('input', PreviewController.onEntityTypeChanged, false);
      let entityType = (<HTMLInputElement>entityTypeInput).value;
      if (entityType.length > 0) {
        PreviewController.setAllMatchingPlaceholders(
          PreviewController.QUESTION_ENTITY_NAME_INPUT_CLASS + " input",
          "Nickname for " + entityType);
        PreviewController.setTextContent(
          PreviewController.QUESTION_ENTITY_TYPE_BUTTON_ID,
          "Add " + entityType);
      }
    }
  }

  static onTextChanged(e: Event) {
    let text = (<HTMLInputElement>e.target).value;
    if (text.length === 0) {
      text = PreviewController.DEFAULT_QUESTION_TEXT;
    }

    PreviewController.setChildrenTextNodes(
      PreviewController.QUESTION_TEXT_CLASS,
      text);
  }

  static onHelpTextChanged(e: Event) {
    let text = (<HTMLInputElement>e.target).value;
    if (text.length === 0) {
      text = PreviewController.DEFAULT_QUESTION_HELP_TEXT;
    }

    PreviewController.setChildrenTextNodes(
      PreviewController.QUESTION_HELP_TEXT_CLASS,
      text);
  }

  static onEnumeratorSelectorChanged(e: Event) {
    const repeatedQuestionInformation = document.querySelector(PreviewController.REPEATED_QUESTION_INFORMATION_ID);
    let enumerator = (<HTMLInputElement>e.target).value;
    repeatedQuestionInformation.classList.toggle("hidden", enumerator === "");
  }

  static onEntityTypeChanged(e: Event) {
    let entityType = (<HTMLInputElement>e.target).value;
    if (entityType.length === 0) {
      entityType = PreviewController.DEFAULT_ENTITY_TYPE;
    }
    PreviewController.setAllMatchingPlaceholders(
      PreviewController.QUESTION_ENTITY_NAME_INPUT_CLASS,
      "Nickname for " + entityType);
    PreviewController.setTextContent(
      PreviewController.QUESTION_ENTITY_TYPE_BUTTON_ID,
      "Add " + entityType);
  }

  /**
   * Sets the child nodes of the selected div as text or span nodes.
   * This will highlight text matching PreviewController.THIS_REGEX
   * in span nodes.
   *
   * This will only work when the selected div is only supposed to contain
   * text and has no other child nodes.
   */
  static setChildrenTextNodes(selector: string, text: string) {
    const previewDiv = document.querySelector(selector);
    const pieces = text.split(PreviewController.THIS_REGEX);

    previewDiv.innerHTML = "";
    pieces.forEach(piece => {
      if (piece.match(PreviewController.THIS_REGEX)) {
        const thisSpan = document.createElement("span");
        thisSpan.classList.add("bg-yellow-300");
        thisSpan.textContent = piece;
        previewDiv.appendChild(thisSpan);
      } else {
        previewDiv.appendChild(document.createTextNode(piece));
      }})
  }

  static setTextContent(selector: string, text: string) {
    const previewDiv = document.querySelector(selector);
    if (previewDiv) {
      previewDiv.textContent = text;
    }
  }

  static setAllMatchingPlaceholders(selector: string, text: string) {
    const inputFields = document.querySelectorAll(selector);
    Array.from(inputFields).forEach(function(inputField) { (<HTMLInputElement>inputField).placeholder = text });
  }
}

new PreviewController();
