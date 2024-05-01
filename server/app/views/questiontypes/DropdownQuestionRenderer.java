package views.questiontypes;

import static j2html.TagCreator.div;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import j2html.tags.specialized.DivTag;
import java.util.Comparator;
import play.i18n.Messages;
import services.MessageKey;
import services.Path;
import services.applicant.ValidationErrorMessage;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.SingleSelectQuestion;
import services.question.LocalizedQuestionOption;
import views.components.SelectWithLabel;

/** Renders a dropdown question. */
public class DropdownQuestionRenderer extends ApplicantSingleQuestionRenderer {

  public DropdownQuestionRenderer(ApplicantQuestion question) {
    super(question);
  }

  @Override
  public String getReferenceClass() {
    return "cf-question-dropdown";
  }

  @Override
  protected DivTag renderInputTag(
      ApplicantQuestionRendererParams params,
      ImmutableMap<Path, ImmutableSet<ValidationErrorMessage>> validationErrors,
      ImmutableList<String> ariaDescribedByIds,
      boolean isOptional) {
    Messages messages = params.messages();
    SingleSelectQuestion singleSelectQuestion = applicantQuestion.createSingleSelectQuestion();

    SelectWithLabel select =
        new SelectWithLabel()
            .addReferenceClass("cf-dropdown-question")
            .setFieldName(singleSelectQuestion.getSelectionPath().toString())
            .setAriaRequired(!isOptional)
            .setPlaceholderText(messages.at(MessageKey.DROPDOWN_PLACEHOLDER.getKeyName()))
            .setOptions(
                singleSelectQuestion.getOptions().stream()
                    .sorted(Comparator.comparing(LocalizedQuestionOption::order))
                    .map(
                        option ->
                            SelectWithLabel.OptionValue.builder()
                                .setLabel(option.optionText())
                                .setValue(String.valueOf(option.id()))
                                .build())
                    .collect(ImmutableList.toImmutableList()))
            .setAriaDescribedByIds(ariaDescribedByIds);

    if (params.autofocusSingleField()) {
      select.focusOnInput();
    }

    if (!validationErrors.isEmpty()) {
      select.forceAriaInvalid();
    }
    select.setScreenReaderText(applicantQuestion.getQuestionTextForScreenReader());

    if (singleSelectQuestion.getSelectedOptionId().isPresent()) {
      select.setValue(String.valueOf(singleSelectQuestion.getSelectedOptionId().get()));
    }

    DivTag dropdownQuestionFormContent = div().with(select.getSelectTag());

    return dropdownQuestionFormContent;
  }
}
