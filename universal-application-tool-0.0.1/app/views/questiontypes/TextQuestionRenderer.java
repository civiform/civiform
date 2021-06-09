package views.questiontypes;

import com.google.common.collect.ImmutableList;
import j2html.tags.Tag;
import services.Path;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.TextQuestion;
import views.components.FieldWithLabel;

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
            .getContainer();

    return renderInternal(params.messages(), questionFormContent, false);
  }

  @Override
  public ImmutableList<Path> getAllPaths() {
    return ImmutableList.of(question.createTextQuestion().getTextPath());
  }
}
