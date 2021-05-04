package views.questiontypes;

import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.input;
import static j2html.TagCreator.label;
import static j2html.TagCreator.span;

import static views.components.FieldWithLabel.checkbox;

import j2html.attributes.Attr;
import j2html.tags.ContainerTag;
import j2html.tags.Tag;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.MultiSelectQuestion;
import services.question.LocalizedQuestionOption;
import views.BaseHtmlView;
import views.style.ReferenceClasses;
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
            .withClasses(Styles.W_FULL, Styles.BLOCK, Styles.P_3)
            .with(
                input()
                    .withId(id)
                    .withType("checkbox")
                    .withName(selectionPath)
                    .withValue(String.valueOf(option.id()))
                    .condAttr(isSelected, Attr.CHECKED, "")
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
                    isSelected ? Styles.BG_BLUE_100 : "",
                    isSelected ? Styles.BORDER_BLUE_400 : ""));
  }
}
