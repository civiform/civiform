package views.questiontypes;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import j2html.tags.specialized.DivTag;
import services.Path;
import services.applicant.ValidationErrorMessage;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.TextQuestion;
import views.components.FieldWithLabel;

/** Renders a text question. */
public class TextQuestionRenderer extends ApplicantSingleQuestionRenderer {

  public TextQuestionRenderer(ApplicantQuestion question) {
    super(question);
  }

  @Override
  public String getReferenceClass() {
    return "cf-question-text";
  }

  @Override
  protected DivTag renderInputTag(
      ApplicantQuestionRendererParams params,
      ImmutableMap<Path, ImmutableSet<ValidationErrorMessage>> validationErrors,
      ImmutableList<String> ariaDescribedByIds,
      boolean isOptional) {
    TextQuestion textQuestion = applicantQuestion.createTextQuestion();

    FieldWithLabel textField =
        FieldWithLabel.input()
            .setFieldName(textQuestion.getTextPath().toString())
            .setValue(textQuestion.getTextValue().orElse(""))
            .setAriaRequired(!isOptional)
            .setFieldErrors(
                params.messages(),
                validationErrors.getOrDefault(textQuestion.getTextPath(), ImmutableSet.of()))
            .setAriaDescribedByIds(ariaDescribedByIds)
            .setScreenReaderText(applicantQuestion.getQuestionTextForScreenReader());

    if (params.autofocusSingleField()) {
      textField.focusOnInput();
    }

    if (!validationErrors.isEmpty()) {
      textField.forceAriaInvalid();
    }

    return textField.getInputTag();
  }
}
