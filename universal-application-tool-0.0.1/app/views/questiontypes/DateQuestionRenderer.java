package views.questiontypes;

import j2html.tags.Tag;
import java.time.LocalDate;
import java.util.Optional;
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
  protected Tag renderTag(ApplicantQuestionRendererParams params) {
    DateQuestion dateQuestion = question.createDateQuestion();

    FieldWithLabel dateField =
        FieldWithLabel.date()
            .setFieldName(dateQuestion.getDatePath().toString())
            .setScreenReaderText(question.getQuestionText());
    if (dateQuestion.getDateValue().isPresent()) {
      Optional<String> value = dateQuestion.getDateValue().map(LocalDate::toString);
      dateField.setValue(value);
    }
    Tag dateQuestionFormContent = dateField.getContainer();

    return dateQuestionFormContent;
  }
}
