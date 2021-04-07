package views.questiontypes;

import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static views.components.FieldWithLabel.checkbox;

import j2html.tags.Tag;
import services.applicant.ApplicantQuestion;
import views.BaseHtmlView;
import views.style.ReferenceClasses;
import views.style.Styles;

public class CheckboxQuestionRenderer extends BaseHtmlView implements ApplicantQuestionRenderer {

  private final ApplicantQuestion question;

  public CheckboxQuestionRenderer(ApplicantQuestion question) {
    this.question = question;
  }

  @Override
  public Tag render() {
    ApplicantQuestion.MultiSelectQuestion multiOptionQuestion = question.getMultiSelectQuestion();

    return div()
        .withId(question.getPath().path())
        .withClasses(Styles.MX_AUTO, Styles.W_MAX)
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
            each(
                multiOptionQuestion.getOptions(),
                option ->
                    checkbox()
                        .setLabelText(option)
                        .setFieldName(multiOptionQuestion.getSelectionPathAsArray().toString())
                        .setValue(option)
                        .setChecked(multiOptionQuestion.optionIsSelected(option))
                        .getContainer()));
  }
}
