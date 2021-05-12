package views.questiontypes;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.input;
import static j2html.TagCreator.label;

import j2html.attributes.Attr;
import j2html.tags.ContainerTag;
import j2html.tags.Tag;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.SingleSelectQuestion;
import services.question.LocalizedQuestionOption;
import views.BaseHtmlView;
import views.style.BaseStyles;
import views.style.ReferenceClasses;
import views.style.StyleUtils;
import views.style.Styles;

public class RadioButtonQuestionRenderer extends BaseHtmlView implements ApplicantQuestionRenderer {

  private final ApplicantQuestion question;

  public RadioButtonQuestionRenderer(ApplicantQuestion question) {
    this.question = checkNotNull(question);
  }

  @Override
  public Tag render(ApplicantQuestionRendererParams params) {
    SingleSelectQuestion singleOptionQuestion = question.createSingleSelectQuestion();

    return div()
        .withId(question.getContextualizedPath().toString())
        .withClasses(Styles.MX_AUTO, Styles.PX_16, BaseStyles.FORM_FIELD_MARGIN_BOTTOM)
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
            fieldErrors(params.messages(), singleOptionQuestion.getQuestionErrors()),
            each(
                singleOptionQuestion.getOptions(),
                option ->
                    renderSingleRadioOption(
                        singleOptionQuestion.getSelectionPath().toString(),
                        option,
                        singleOptionQuestion.optionIsSelected(option))));
  }

  private Tag renderSingleRadioOption(
      String selectionPath, LocalizedQuestionOption option, boolean checked) {
    String id = option.optionText().replaceAll("\\s+", "_");

    ContainerTag labelTag =
        label()
            .withClasses(
                ReferenceClasses.RADIO_OPTION,
                BaseStyles.RADIO_LABEL,
                checked ? Styles.BG_BLUE_100 : "",
                checked ? Styles.BORDER_BLUE_500 : "")
            .with(
                input()
                    .withId(id)
                    .withType("radio")
                    .withName(selectionPath)
                    .withValue(String.valueOf(option.id()))
                    .condAttr(checked, Attr.CHECKED, "")
                    .withClasses(
                        StyleUtils.joinStyles(ReferenceClasses.RADIO_INPUT, BaseStyles.RADIO)))
            .withText(option.optionText());

    return div().withClasses(Styles.MY_2, Styles.RELATIVE).with(labelTag);
  }
}
