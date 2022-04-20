package views.questiontypes;

import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static j2html.TagCreator.div;

import j2html.tags.Tag;
import java.util.Comparator;
import play.i18n.Messages;
import services.MessageKey;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.SingleSelectQuestion;
import services.question.LocalizedQuestionOption;
import views.components.SelectWithLabel;

/** Renders a dropdown question. */
public class DropdownQuestionRenderer extends ApplicantQuestionRendererImpl {

  public DropdownQuestionRenderer(ApplicantQuestion question) {
    super(question);
  }

  @Override
  public String getReferenceClass() {
    return "cf-question-dropdown";
  }

  @Override
  protected Tag renderTag(ApplicantQuestionRendererParams params) {
    Messages messages = params.messages();
    SingleSelectQuestion singleSelectQuestion = question.createSingleSelectQuestion();

    SelectWithLabel select =
        new SelectWithLabel()
            .addReferenceClass("cf-dropdown-question")
            .setFieldName(singleSelectQuestion.getSelectionPath().toString())
            .setPlaceholderText(messages.at(MessageKey.DROPDOWN_PLACEHOLDER.getKeyName()))
            .setOptions(
                singleSelectQuestion.getOptions().stream()
                    .sorted(Comparator.comparing(LocalizedQuestionOption::order))
                    .collect(
                        toImmutableMap(
                            LocalizedQuestionOption::optionText,
                            option -> String.valueOf(option.id()))));
    select.setScreenReaderText(question.getQuestionText());

    if (singleSelectQuestion.getSelectedOptionId().isPresent()) {
      select.setValue(String.valueOf(singleSelectQuestion.getSelectedOptionId().get()));
    }

    Tag dropdownQuestionFormContent = div().with(select.getContainer());

    return dropdownQuestionFormContent;
  }
}
