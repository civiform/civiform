package views.questiontypes;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import j2html.tags.Tag;
import java.util.ArrayList;
import services.Path;
import services.applicant.ValidationErrorMessage;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.TextQuestion;
import views.components.FieldWithLabel;

/** Renders a text question. */
public class TextQuestionRenderer extends ApplicantQuestionRendererImpl {

  public TextQuestionRenderer(ApplicantQuestion question) {
    super(question);
  }

  @Override
  public String getReferenceClass() {
    return "cf-question-text";
  }

  @Override
  protected Tag renderTag(
      ApplicantQuestionRendererParams params,
      ImmutableMap<Path, ImmutableSet<ValidationErrorMessage>> validationErrors,
      ArrayList<String> ariaDescribedByIds, boolean hasQuestionErrors) {
    TextQuestion textQuestion = question.createTextQuestion();

    Tag questionFormContent =
        FieldWithLabel.input()
            .setFieldName(textQuestion.getTextPath().toString())
            .setValue(textQuestion.getTextValue().orElse(""))
            .setAriaDescribedByIds(ariaDescribedByIds)
            .setHasQuestionErrors(hasQuestionErrors)
            .setFieldErrors(
                params.messages(),
                validationErrors.getOrDefault(textQuestion.getTextPath(), ImmutableSet.of()))
            .setScreenReaderText(question.getQuestionText())
            .getContainer();

    return questionFormContent;
  }
}
