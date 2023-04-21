package views.questiontypes;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import j2html.tags.specialized.DivTag;
import java.time.LocalDate;
import java.util.Optional;
import services.Path;
import services.applicant.ValidationErrorMessage;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.DateQuestion;
import views.components.FieldWithLabel;
import views.style.ReferenceClasses;

/** Renders a date question. */
public class DateQuestionRenderer extends ApplicantSingleQuestionRenderer {

  public DateQuestionRenderer(ApplicantQuestion question) {
    super(question);
  }

  @Override
  public String getReferenceClass() {
    return ReferenceClasses.DATE_QUESTION;
  }

  @Override
  protected DivTag renderInputTag(
      ApplicantQuestionRendererParams params,
      ImmutableMap<Path, ImmutableSet<ValidationErrorMessage>> validationErrors,
      ImmutableList<String> ariaDescribedByIds,
      boolean isOptional) {
    DateQuestion dateQuestion = question.createDateQuestion();

    FieldWithLabel dateField =
        FieldWithLabel.date()
            .setFieldName(dateQuestion.getDatePath().toString())
            .setScreenReaderText(question.getQuestionTextForScreenReader())
            .setAriaRequired(!isOptional)
            .setFieldErrors(
                params.messages(),
                validationErrors.getOrDefault(dateQuestion.getDatePath(), ImmutableSet.of()))
            .setAriaDescribedByIds(ariaDescribedByIds);
    if (!validationErrors.isEmpty()) {
      dateField.forceAriaInvalid();
    }
    if (dateQuestion.getDateValue().isPresent()) {
      // Note: If the provided input was invalid, there's no use rendering
      // the value on roundtrip since inputs with type="date" won't allow
      // setting a value that doesn't conform to the expected format.
      Optional<String> value = dateQuestion.getDateValue().map(LocalDate::toString);
      dateField.setValue(value);
    }
    DivTag dateQuestionFormContent = dateField.getDateTag();

    return dateQuestionFormContent;
  }
}
