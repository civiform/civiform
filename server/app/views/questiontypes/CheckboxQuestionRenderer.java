package views.questiontypes;

import static j2html.TagCreator.div;
import static j2html.TagCreator.input;
import static j2html.TagCreator.label;
import static j2html.TagCreator.span;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.LabelTag;
import java.util.Comparator;
import services.Path;
import services.applicant.ValidationErrorMessage;
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
  protected DivTag renderTag(
      ApplicantQuestionRendererParams params,
      ImmutableMap<Path, ImmutableSet<ValidationErrorMessage>> validationErrors) {
    MultiSelectQuestion multiOptionQuestion = question.createMultiSelectQuestion();

    DivTag checkboxQuestionFormContent =
        div()
            // Hidden input that's always selected to allow for clearing mutli-select data.
            .with(
                input()
                    .withType("checkbox")
                    .withName(multiOptionQuestion.getSelectionPathAsArray())
                    .withValue("")
                    .withCondChecked(!multiOptionQuestion.hasValue())
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

  private DivTag renderCheckboxOption(
      String selectionPath, LocalizedQuestionOption option, boolean isSelected) {
    String id = "checkbox-" + question.getContextualizedPath() + "-" + option.id();
    LabelTag labelTag =
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
                    .withCondChecked(isSelected)
                    .withClasses(
                        StyleUtils.joinStyles(ReferenceClasses.RADIO_INPUT, BaseStyles.CHECKBOX)),
                span(option.optionText()).withClasses(ReferenceClasses.MULTI_OPTION_VALUE));

    return div()
        .withClasses(ReferenceClasses.MULTI_OPTION_QUESTION_OPTION, Styles.MY_2, Styles.RELATIVE)
        .with(labelTag);
  }
}
