package views.questiontypes;

import j2html.tags.Tag;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.IdQuestion;
import views.components.FieldWithLabel;

/** Renders an id question. */
public class IdQuestionRenderer extends ApplicantQuestionRendererImpl {

  public IdQuestionRenderer(ApplicantQuestion question) {
    super(question);
  }

  @Override
  public String getReferenceClass() {
    return "cf-question-id";
  }
  @Override
  protected boolean shouldDisplayQuestionErrors() {
      return false;
  }

  @Override
  protected Tag renderTag(ApplicantQuestionRendererParams params) {
    IdQuestion idQuestion = question.createIdQuestion();

    Tag questionFormContent =
        FieldWithLabel.input()
            .setFieldName(idQuestion.getIdPath().toString())
            .setValue(idQuestion.getIdValue().orElse(""))
            .setFieldErrors(params.messages(), idQuestion.getQuestionErrors())
            .setScreenReaderText(question.getQuestionText())
            .getContainer();

    return questionFormContent;
  }
}
