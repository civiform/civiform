package views.questiontypes;

import static j2html.TagCreator.div;

import j2html.tags.Tag;
import java.time.LocalDate;
import java.util.Optional;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.DateQuestion;
import views.components.FieldWithLabel;
import views.style.ReferenceClasses;
import views.style.Styles;

public class DateQuestionRenderer extends ApplicantQuestionRenderer {

  public DateQuestionRenderer(ApplicantQuestion question) {
    super(question);
  }

  @Override
  public Tag render(ApplicantQuestionRendererParams params) {
    DateQuestion dateQuestion = question.createDateQuestion();

    FieldWithLabel dateField =
        FieldWithLabel.date().setFieldName(dateQuestion.getDatePath().toString());

    if (dateQuestion.getDateValue().isPresent()) {
      // TODO: [Refactor] Oof! Converting Optional<Long> to OptionalLong.
      Optional<String> value = dateQuestion.getDateValue().map(LocalDate::toString);
      // TODO: Convert value from long --> a date string that is compatible with <input type=date>
      dateField.setValue(value);
    }

    return div()
        .withId(question.getContextualizedPath().toString())
        .withClasses(Styles.MX_AUTO, Styles.PX_16)
        .with(
            div()
                .withClasses(ReferenceClasses.APPLICANT_QUESTION_TEXT)
                .withText(question.getQuestionText()),
            div()
                .withClasses(
                    ReferenceClasses.APPLICANT_QUESTION_HELP_TEXT,
                    Styles.TEXT_BASE,
                    Styles.FONT_THIN,
                    Styles.MB_2)
                .withText(question.getQuestionHelpText()),
            dateField.getContainer());
  }
}
