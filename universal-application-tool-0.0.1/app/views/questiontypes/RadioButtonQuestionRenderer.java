package views.questiontypes;

import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.input;
import static j2html.TagCreator.label;

import j2html.attributes.Attr;
import j2html.tags.Tag;
import services.applicant.ApplicantQuestion;
import views.BaseHtmlView;
import views.style.ReferenceClasses;
import views.style.Styles;

public class RadioButtonQuestionRenderer extends BaseHtmlView implements ApplicantQuestionRenderer {

  private final ApplicantQuestion question;

  public RadioButtonQuestionRenderer(ApplicantQuestion question) {
    this.question = question;
  }

  @Override
  public Tag render() {
    ApplicantQuestion.SingleSelectQuestion singleOptionQuestion =
        question.getSingleSelectQuestion();

    return div()
        .withId(question.getPath().toString())
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
                singleOptionQuestion.getOptions(),
                option ->
                    renderSingleRadioOption(
                        singleOptionQuestion.getSelectionPath().toString(),
                        option,
                        singleOptionQuestion.optionIsSelected(option))));
  }

  private Tag renderSingleRadioOption(String selectionPath, String optionName, boolean checked) {
    String id = optionName.replaceAll("\\s+", "_");
    return div()
        .with(
            input()
                .withId(id)
                .withType("radio")
                .withName(selectionPath)
                .withValue(optionName)
                .condAttr(checked, Attr.CHECKED, ""),
            label(optionName).attr(Attr.FOR, id));
  }
}
