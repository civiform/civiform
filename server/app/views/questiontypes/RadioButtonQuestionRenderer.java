package views.questiontypes;

import static j2html.TagCreator.div;
import static j2html.TagCreator.input;
import static j2html.TagCreator.label;
import static j2html.TagCreator.span;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import j2html.attributes.Attr;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.InputTag;
import j2html.tags.specialized.LabelTag;
import java.util.Comparator;
import java.util.Optional;
import org.apache.commons.lang3.RandomStringUtils;
import services.Path;
import services.applicant.ValidationErrorMessage;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.SingleSelectQuestion;
import services.question.LocalizedQuestionOption;
import views.style.BaseStyles;
import views.style.ReferenceClasses;
import views.style.StyleUtils;

/** Renders a radio button question. */
public class RadioButtonQuestionRenderer extends ApplicantCompositeQuestionRenderer {

  public RadioButtonQuestionRenderer(ApplicantQuestion question) {
    super(question);
  }

  @Override
  public String getReferenceClass() {
    return "cf-question-radio";
  }

  @Override
  protected DivTag renderInputTags(
      ApplicantQuestionRendererParams params,
      ImmutableMap<Path, ImmutableSet<ValidationErrorMessage>> validationErrors,
      boolean isOptional) {
    SingleSelectQuestion singleOptionQuestion = applicantQuestion.createSingleSelectQuestion();
    boolean hasErrors = !validationErrors.isEmpty();

    DivTag radioQuestionFormContent =
        div()
            .with(
                singleOptionQuestion.getOptions().stream()
                    .sorted(Comparator.comparing(LocalizedQuestionOption::order))
                    .map(
                        option ->
                            renderRadioOption(
                                singleOptionQuestion.getSelectionPath().toString(),
                                option,
                                singleOptionQuestion.optionIsSelected(option),
                                hasErrors,
                                isOptional,
                                params.errorDisplayMode(),
                                params.questionName())));

    return radioQuestionFormContent;
  }

  private DivTag renderRadioOption(
      String selectionPath,
      LocalizedQuestionOption option,
      boolean checked,
      boolean hasErrors,
      boolean isOptional,
      ApplicantQuestionRendererParams.ErrorDisplayMode errorDisplayMode,
      Optional<String> questionName) {
    String id = RandomStringUtils.randomAlphabetic(8);

    InputTag inputTag =
        input()
            .withId(id)
            .withType("radio")
            .withName(selectionPath)
            .withValue(String.valueOf(option.id()))
            .withCondChecked(checked)
            .condAttr(applicantSelectedQuestion(questionName), Attr.AUTOFOCUS, "")
            .condAttr(hasErrors, "aria-invalid", "true")
            .condAttr(
                errorDisplayMode.equals(
                    ApplicantQuestionRendererParams.ErrorDisplayMode.DISPLAY_SINGLE_ERROR),
                Attr.AUTOFOCUS,
                "")
            .condAttr(!isOptional, "aria-required", "true")
            .withClasses(StyleUtils.joinStyles(ReferenceClasses.RADIO_INPUT, BaseStyles.RADIO));

    LabelTag labelTag =
        label()
            .withFor(id)
            .withClasses("inline-block", "w-full", "h-full")
            .with(inputTag)
            .with(span(option.optionText()).withClasses(ReferenceClasses.MULTI_OPTION_VALUE));

    return div()
        .withClasses(
            "my-2",
            "relative",
            ReferenceClasses.MULTI_OPTION_QUESTION_OPTION,
            ReferenceClasses.RADIO_OPTION,
            BaseStyles.RADIO_LABEL,
            checked ? BaseStyles.BORDER_SEATTLE_BLUE : "")
        .with(labelTag);
  }
}
