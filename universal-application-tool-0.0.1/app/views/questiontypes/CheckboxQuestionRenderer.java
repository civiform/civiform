package views.questiontypes;

import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.input;
import static j2html.TagCreator.label;

import j2html.attributes.Attr;
import j2html.tags.ContainerTag;
import j2html.tags.Tag;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.MultiSelectQuestion;
import services.question.LocalizedQuestionOption;
import views.BaseHtmlView;
import views.style.BaseStyles;
import views.style.ReferenceClasses;
import views.style.StyleUtils;
import views.style.Styles;

public class CheckboxQuestionRenderer extends BaseHtmlView implements ApplicantQuestionRenderer {

  private final ApplicantQuestion question;

  public CheckboxQuestionRenderer(ApplicantQuestion question) {
    this.question = question;
  }

  @Override
  public Tag render(ApplicantQuestionRendererParams params) {
    MultiSelectQuestion multiOptionQuestion = question.createMultiSelectQuestion();

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
            input() // Hidden input that's always selected to allow for clearing mutli-select data.
                .withType("checkbox")
                .withName(multiOptionQuestion.getSelectionPathAsArray())
                .withValue("")
                .condAttr(!multiOptionQuestion.hasValue(), Attr.CHECKED, "")
                .withClasses(ReferenceClasses.RADIO_DEFAULT, Styles.HIDDEN),
            fieldErrors(params.messages(), multiOptionQuestion.getQuestionErrors()),
            each(
                multiOptionQuestion.getOptions(),
                option ->
                    renderCheckboxQuestion(
                        multiOptionQuestion.getSelectionPathAsArray(),
                        option,
                        multiOptionQuestion.optionIsSelected(option))));
  }

  private Tag renderCheckboxQuestion(
      String selectionPath, LocalizedQuestionOption option, boolean isSelected) {
    String id = "checkbox-" + question.getContextualizedPath() + "-" + option.id();
    ContainerTag labelTag =
        label()
            .withClasses(
                ReferenceClasses.RADIO_OPTION,
                BaseStyles.CHECKBOX_LABEL,
                isSelected ? Styles.BG_BLUE_100 : "",
                isSelected ? Styles.BORDER_BLUE_500 : "")
            .with(
                input()
                    .withId(id)
                    .withType("checkbox")
                    .withName(selectionPath)
                    .withValue(String.valueOf(option.id()))
                    .condAttr(isSelected, Attr.CHECKED, "")
                    .withClasses(
                        StyleUtils.joinStyles(ReferenceClasses.RADIO_INPUT, BaseStyles.CHECKBOX)))
            .withText(option.optionText());

    return div().withClasses(Styles.MY_2, Styles.RELATIVE).with(labelTag);
  }
}
