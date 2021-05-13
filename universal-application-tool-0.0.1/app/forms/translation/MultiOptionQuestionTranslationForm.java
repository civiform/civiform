package forms.translation;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import services.LocalizedStrings;
import services.question.QuestionOption;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.MultiOptionQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionBuilder;

public class MultiOptionQuestionTranslationForm extends QuestionTranslationForm {

  private List<String> options;

  public MultiOptionQuestionTranslationForm() {
    super();
    this.options = new ArrayList<>();
  }

  public List<String> getOptions() {
    return options;
  }

  public void setOptions(List<String> options) {
    this.options = options;
  }

  @Override
  public QuestionDefinitionBuilder builderWithUpdates(
      QuestionDefinition definition, Locale updatedLocale) throws UnsupportedQuestionTypeException {
    QuestionDefinitionBuilder partiallyUpdated =
        super.builderWithUpdates(definition, updatedLocale);
    ImmutableList<QuestionOption> currentOptions =
        ((MultiOptionQuestionDefinition) definition).getOptions();

    ImmutableList.Builder<QuestionOption> updatedOptionsBuilder = ImmutableList.builder();
    for (int i = 0; i < currentOptions.size(); i++) {
      QuestionOption current = currentOptions.get(i);
      LocalizedStrings translations = current.optionText().updateTranslation(updatedLocale, this.options.get(i));
      updatedOptionsBuilder.add(current.toBuilder().setOptionText(translations).build());
    }
    return partiallyUpdated.setQuestionOptions(updatedOptionsBuilder.build());
  }
}
