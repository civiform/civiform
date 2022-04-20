package views.questiontypes;

import static j2html.TagCreator.div;
import static j2html.TagCreator.input;
import static j2html.TagCreator.label;

import j2html.attributes.Attr;
import j2html.tags.ContainerTag;
import j2html.tags.Tag;
import java.util.Comparator;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.MultiSelectQuestion;
import services.question.LocalizedQuestionOption;
import views.style.BaseStyles;
import views.style.ReferenceClasses;
import views.style.StyleUtils;
import views.style.Styles;

/** Renders a checkbox question. */
public class CheckboxQuestionRenderer extends ApplicantQuestionRendererImpl {

  @Override
  public String getReferenceClass() {
    return ReferenceClasses.CHECKBOX_QUESTION;
  }

  public CheckboxQuestionRenderer(ApplicantQuestion question) {
    super(question);
  }

  @Override
  protected boolean shouldDisplayQuestionErrors() {
      return true;
  }

  @Override
  protected Tag renderTag(ApplicantQuestionRendererParams params) {
    MultiSelectQuestion multiOptionQuestion = question.createMultiSelectQuestion();

    Tag checkboxQuestionFormContent =
        div()
            // Hidden input that's always selected to allow for clearing mutli-select data.
            .with(
                input()
                    .withType("checkbox")
                    .withName(multiOptionQuestion.getSelectionPathAsArray())
                    .withValue("")
                    .condAttr(!multiOptionQuestion.hasValue(), Attr.CHECKED, "")
                    .withClasses(ReferenceClasses.RADIO_DEFAULT, Styles.HIDDEN))
            .with(
                multiOptionQuestion.getOptions().stream()
                    .sorted(Comparator.comparing(LocalizedQuestionOption::order))
                    .map(
                        option ->
                            renderCheckboxOption(
                                multiOptionQuestion.getSelectionPathAsArray(),
                                option,
                                multiOptionQuestion.optionIsSelected(option))));

    return checkboxQuestionFormContent;
  }

  private Tag renderCheckboxOption(
      String selectionPath, LocalizedQuestionOption option, boolean isSelected) {
    String id = "checkbox-" + question.getContextualizedPath() + "-" + option.id();
    ContainerTag labelTag =
        label()
            .withClasses(
                ReferenceClasses.RADIO_OPTION,
                BaseStyles.CHECKBOX_LABEL,
                isSelected ? BaseStyles.BORDER_SEATTLE_BLUE : "")
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
