package views.questiontypes;

import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.input;
import static j2html.TagCreator.label;
import static j2html.TagCreator.span;

import j2html.attributes.Attr;
import j2html.tags.ContainerTag;
import j2html.tags.Tag;
import play.i18n.Messages;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.SingleSelectQuestion;
import services.question.LocalizedQuestionOption;
import views.BaseHtmlView;
import views.style.ReferenceClasses;
import views.style.Styles;

public class RadioButtonQuestionRenderer extends BaseHtmlView implements ApplicantQuestionRenderer {

  private final ApplicantQuestion question;

  public RadioButtonQuestionRenderer(ApplicantQuestion question) {
    this.question = question;
  }

  @Override
  public Tag render(Messages messages) {
    SingleSelectQuestion singleOptionQuestion = question.createSingleSelectQuestion();

    return div()
        .withId(question.getContextualizedPath().toString())
        .withClasses(Styles.MX_AUTO, Styles.PX_16)
        .with(
            div()
                .withClasses(ReferenceClasses.APPLICANT_QUESTION_TEXT, Styles.TEXT_3XL)
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
                        singleOptionQuestion.optionIsSelected(option))),
            fieldErrors(messages, singleOptionQuestion.getQuestionErrors())
                .withClasses(Styles.ML_2, Styles.TEXT_XS, Styles.TEXT_RED_600, Styles.FONT_BOLD));
  }

  private Tag renderSingleRadioOption(
      String selectionPath, LocalizedQuestionOption option, boolean checked) {
    String id = option.optionText().replaceAll("\\s+", "_");

    ContainerTag labelTag =
        label()
            .withClasses(Styles.W_FULL, Styles.BLOCK, Styles.P_3)
            .with(
                input()
                    .withId(id)
                    .withType("radio")
                    .withName(selectionPath)
                    .withValue(String.valueOf(option.id()))
                    .condAttr(checked, Attr.CHECKED, "")
                    .withClasses(ReferenceClasses.RADIO_INPUT, Styles.H_5, Styles.W_5),
                span(option.optionText()).withClasses(Styles.ML_4, Styles.TEXT_GRAY_700));

    return div()
        .withClasses(Styles.MY_2, Styles.RELATIVE)
        .with(
            div(labelTag)
                .withClasses(
                    ReferenceClasses.RADIO_OPTION,
                    Styles.M_AUTO,
                    Styles.W_4_5,
                    Styles.TEXT_2XL,
                    Styles.BORDER_4,
                    Styles.BG_WHITE,
                    Styles.ROUNDED_XL,
                    checked ? Styles.BG_BLUE_100 : "",
                    checked ? Styles.BORDER_BLUE_400 : ""));
  }
}
