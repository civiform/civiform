package views.questiontypes;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import j2html.tags.Tag;
import java.time.LocalDate;
import java.util.Optional;
import services.Path;
import services.applicant.ValidationErrorMessage;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.DateQuestion;
import views.components.FieldWithLabel;
import views.style.ReferenceClasses;

/** Renders a date question. */
public class DateQuestionRenderer extends ApplicantQuestionRendererImpl {

  public DateQuestionRenderer(ApplicantQuestion question) {
    super(question);
  }

  @Override
  public String getReferenceClass() {
    return ReferenceClasses.DATE_QUESTION;
  }

  @Override
  protected Tag renderTag(
      ApplicantQuestionRendererParams params,
      ImmutableMap<Path, ImmutableSet<ValidationErrorMessage>> validationErrors) {
    DateQuestion dateQuestion = question.createDateQuestion();

    FieldWithLabel dateField =
        FieldWithLabel.date()
            .setFieldName(dateQuestion.getDatePath().toString())
            .setScreenReaderText(question.getQuestionText());
    if (dateQuestion.getDateValue().isPresent()) {
      // Note: If the provided input was invalid, there's no use rendering
      // the value on roundtrip since inputs with type="date" won't allow
      // setting a value that doesn't conform to the expected format.
      Optional<String> value = dateQuestion.getDateValue().map(LocalDate::toString);
      dateField.setValue(value);
    }
    Tag dateQuestionFormContent = dateField.getContainer();

    return dateQuestionFormContent;
  }
}
