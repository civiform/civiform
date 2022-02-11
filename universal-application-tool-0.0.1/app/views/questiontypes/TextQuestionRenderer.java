package views.questiontypes;

import j2html.tags.Tag;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.TextQuestion;
import views.components.FieldWithLabel;

/** Renders a text question. */
public class TextQuestionRenderer extends ApplicantQuestionRenderer {

  public TextQuestionRenderer(ApplicantQuestion question) {
    super(question);
  }

  @Override
  public String getReferenceClass() {
    return "cf-question-text";
  }

  @Override
  public Tag render(ApplicantQuestionRendererParams params) {
    TextQuestion textQuestion = question.createTextQuestion();

    Tag questionFormContent =
        FieldWithLabel.input()
            .setFieldName(textQuestion.getTextPath().toString())
            .setValue(textQuestion.getTextValue().orElse(""))
            .setFieldErrors(params.messages(), textQuestion.getQuestionErrors())
            .setScreenReaderText(question.getQuestionText())
            .setDescribedByHtmlId(questionHelpTextHtmlId())
            .setErrorMessageHtmlId(questionErrorMessageHtmlId())
            .setIsInvalid(isInvalid())
            .setIsRequired(question.isRequired())
            .getContainer();

    return renderInternal(params.messages(), questionFormContent, false);
  }
}
