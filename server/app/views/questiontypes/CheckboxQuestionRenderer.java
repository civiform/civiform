package views.questiontypes;

import static j2html.TagCreator.div;
import static j2html.TagCreator.input;
import static j2html.TagCreator.label;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import j2html.attributes.Attr;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.LabelTag;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicBoolean;
import services.Path;
import services.applicant.ValidationErrorMessage;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.MultiSelectQuestion;
import services.question.LocalizedQuestionOption;
import views.components.TextFormatter;
import views.style.BaseStyles;
import views.style.ReferenceClasses;
import views.style.StyleUtils;

/** Renders a checkbox question. */
public class CheckboxQuestionRenderer extends ApplicantCompositeQuestionRenderer {

  @Override
  public String getReferenceClass() {
    return ReferenceClasses.CHECKBOX_QUESTION;
  }

  public CheckboxQuestionRenderer(ApplicantQuestion question) {
    super(question);
  }

  @Override
  protected DivTag renderInputTags(
      ApplicantQuestionRendererParams params,
      ImmutableMap<Path, ImmutableSet<ValidationErrorMessage>> validationErrors,
      boolean isOptional) {

    boolean hasErrors = !validationErrors.isEmpty();
    AtomicBoolean alreadyAutofocused = new AtomicBoolean(false);
    MultiSelectQuestion multiOptionQuestion = applicantQuestion.createMultiSelectQuestion();

    DivTag checkboxQuestionFormContent =
        div()
            // Hidden input that's always selected to allow for clearing multi-select data.
            .with(
                input()
                    .withType("checkbox")
                    .withName(multiOptionQuestion.getSelectionPathAsArray())
                    .withValue("")
                    .withCondChecked(!multiOptionQuestion.hasValue())
                    .withClasses(ReferenceClasses.RADIO_DEFAULT, "hidden"))
            .with(
                multiOptionQuestion.getOptions().stream()
                    .sorted(Comparator.comparing(LocalizedQuestionOption::order))
                    .map(
                        option -> {
                          boolean shouldAutofocus = false;

                          if (params.autofocusSingleField() && !alreadyAutofocused.get()) {
                            shouldAutofocus = true;
                            alreadyAutofocused.setPlain(true);
                          }

                          return renderCheckboxOption(
                              multiOptionQuestion.getSelectionPathAsArray(),
                              option,
                              multiOptionQuestion.optionIsSelected(option),
                              hasErrors,
                              isOptional,
                              shouldAutofocus);
                        }));

    return checkboxQuestionFormContent;
  }

  private DivTag renderCheckboxOption(
      String selectionPath,
      LocalizedQuestionOption option,
      boolean isSelected,
      boolean hasErrors,
      boolean isOptional,
      boolean autofocus) {
    String id = "checkbox-" + applicantQuestion.getContextualizedPath() + "-" + option.id();
    LabelTag labelTag =
        label()
            .withClasses(
                ReferenceClasses.RADIO_OPTION,
                BaseStyles.CHECKBOX_LABEL,
                "checked:" + BaseStyles.BORDER_CIVIFORM_BLUE)
            .with(
                input()
                    .withId(id)
                    .withType("checkbox")
                    .withName(selectionPath)
                    .withValue(String.valueOf(option.id()))
                    .withCondChecked(isSelected)
                    .condAttr(autofocus, Attr.AUTOFOCUS, "")
                    .condAttr(hasErrors, "aria-invalid", "true")
                    .condAttr(!isOptional, "aria-required", "true")
                    .withClasses(
                        StyleUtils.joinStyles(ReferenceClasses.RADIO_INPUT, BaseStyles.CHECKBOX)),
                div()
                    .with(TextFormatter.formatText(option.optionText()))
                    .withClasses(ReferenceClasses.MULTI_OPTION_VALUE));

    return div()
        .withClasses(ReferenceClasses.MULTI_OPTION_QUESTION_OPTION, "my-2", "relative")
        .with(labelTag);
  }
}
