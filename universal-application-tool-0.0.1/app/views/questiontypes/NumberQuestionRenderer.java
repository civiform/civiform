package views.questiontypes;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;

import j2html.tags.Tag;
import java.util.OptionalLong;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.NumberQuestion;
import views.BaseHtmlView;
import views.components.FieldWithLabel;
import views.style.ReferenceClasses;
import views.style.Styles;

public class NumberQuestionRenderer extends BaseHtmlView implements ApplicantQuestionRenderer {

  private final ApplicantQuestion question;

  public NumberQuestionRenderer(ApplicantQuestion question) {
    this.question = checkNotNull(question);
  }

  @Override
  public Tag render(ApplicantQuestionRendererParams params) {
    NumberQuestion numberQuestion = question.createNumberQuestion();

    FieldWithLabel numberField =
        FieldWithLabel.number()
            .setFieldName(numberQuestion.getNumberPath().toString())
            .setFloatLabel(true);
    if (numberQuestion.getNumberValue().isPresent()) {
      // TODO: [Refactor] Oof! Converting Optional<Long> to OptionalLong.
      OptionalLong value = OptionalLong.of(numberQuestion.getNumberValue().orElse(0L));
      numberField.setValue(value);
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
            numberField.getContainer(),
            fieldErrors(numberQuestion.getQuestionErrors()));
  }
}
