package views.questiontypes;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static j2html.TagCreator.div;
import static j2html.TagCreator.option;
import static j2html.TagCreator.select;

import j2html.tags.Tag;
import java.util.AbstractMap;
import play.i18n.Messages;
import services.MessageKey;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.SingleSelectQuestion;
import views.components.SelectWithLabel;

public class DropdownQuestionRenderer extends ApplicantQuestionRenderer {

  public DropdownQuestionRenderer(ApplicantQuestion question) {
    super(question);
  }

  @Override
  public String getReferenceClass() {
    return "cf-question-dropdown";
  }

  @Override
  public Tag render(ApplicantQuestionRendererParams params) {
    Messages messages = params.messages();
    SingleSelectQuestion singleSelectQuestion = question.createSingleSelectQuestion();

    SelectWithLabel select =
        new SelectWithLabel()
            .addReferenceClass("cf-dropdown-question")
            .setFieldName(singleSelectQuestion.getSelectionPath().toString())
            .setPlaceholderText(messages.at(MessageKey.DROPDOWN_PLACEHOLDER.getKeyName()))
            .setOptions(
                singleSelectQuestion.getOptions().stream()
                    .map(
                        option ->
                            new AbstractMap.SimpleEntry<>(
                                option.optionText(), String.valueOf(option.id())))
                    .collect(toImmutableList()));

    if (singleSelectQuestion.getSelectedOptionId().isPresent()) {
      select.setValue(String.valueOf(singleSelectQuestion.getSelectedOptionId().get()));
    }

    Tag dropdownQuestionFormContent = div().with(select.getContainer());

    return renderInternal(params.messages(), dropdownQuestionFormContent);
  }
}
