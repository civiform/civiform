package views.questiontypes;

import static services.question.types.DateQuestionDefinition.DateValidationOption.DateType.APPLICATION_DATE;
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
import services.question.types.DateQuestionDefinition;
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
    DateQuestion dateQuestion = applicantQuestion.createDateQuestion();

    FieldWithLabel dateField =
        FieldWithLabel.date()
            .setFieldName(dateQuestion.getDatePath().toString())
            .setScreenReaderText(applicantQuestion.getQuestionTextForScreenReader())
            .setAriaRequired(!isOptional)
            .setFieldErrors(
                params.messages(),
                validationErrors.getOrDefault(dateQuestion.getDatePath(), ImmutableSet.of()))
            .setAriaDescribedByIds(ariaDescribedByIds);

    if (params.autofocusSingleField()) {
      dateField.focusOnInput();
    }

    if (!validationErrors.isEmpty()) {
      dateField.forceAriaInvalid();
    }

    if (dateQuestion.getDateValue().isPresent()) {
      // Note: If the provided input was invalid, there's no use rendering
      // the value on round trip since inputs with type="date" won't allow
      // setting a value that doesn't conform to the expected format.
      // Autofill with today's date if both minDate and maxDate are APPLICATION_DATE
      // and there's no existing applicant date value
      Optional<String> value = dateQuestion.getDateValue().map(LocalDate::toString);
      dateField.setValue(value);
    } else {
      if (shouldAutofillWithCurrentDate(dateQuestion)) {
        Optional<String> currentDateValue = Optional.of(LocalDate.now().toString());
        dateField.setValue(currentDateValue);
      }
    }

    return dateField.getDateTag();
  }

  /**
   * Determines if the date field should be autofilled with the current date.
   * This happens when both minDate and maxDate are set to APPLICATION_DATE and
   * there is no existing applicant date value.
   */
  private boolean shouldAutofillWithCurrentDate(DateQuestion dateQuestion) {
    DateQuestionDefinition definition = dateQuestion.getQuestionDefinition();
    Optional<DateQuestionDefinition.DateValidationOption> minDate = definition.getMinDate();
    Optional<DateQuestionDefinition.DateValidationOption> maxDate = definition.getMaxDate();
    
    return minDate.isPresent() 
        && maxDate.isPresent()
        && minDate.get().dateType() == APPLICATION_DATE
        && maxDate.get().dateType() == APPLICATION_DATE
        && dateQuestion.getDateValue().isEmpty();
  }
}
