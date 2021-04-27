package views.questiontypes;

import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static views.components.FieldWithLabel.checkbox;

import j2html.tags.Tag;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.MultiSelectQuestion;
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
    MultiSelectQuestion multiOptionQuestion = question.createMultiSelectQuestion();

    return div()
        .withId(question.getContextualizedPath().toString())
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
                        .setId(
                            "checkbox-"
                                + question.getContextualizedPath()
                                + "-"
                                + String.valueOf(option.id()))
                        .setLabelText(option.optionText())
                        .setFieldName(multiOptionQuestion.getSelectionPathAsArray())
                        .setValue(String.valueOf(option.id()))
                        .setChecked(multiOptionQuestion.optionIsSelected(option))
                        .getContainer()));
  }
}
