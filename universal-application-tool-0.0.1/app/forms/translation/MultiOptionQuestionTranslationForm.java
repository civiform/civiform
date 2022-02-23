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

/** Form for updating translation for multi-option questions. */
public class MultiOptionQuestionTranslationForm extends QuestionTranslationForm {

  // These will be in the same order as the default translations.
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
      QuestionDefinition toUpdate, Locale updatedLocale) throws UnsupportedQuestionTypeException {
    QuestionDefinitionBuilder partiallyUpdated = super.builderWithUpdates(toUpdate, updatedLocale);
    ImmutableList.Builder<QuestionOption> updatedOptionsBuilder = ImmutableList.builder();

    // For each current option, add or update translations for the given locale.
    ImmutableList<QuestionOption> currentOptions =
        ((MultiOptionQuestionDefinition) toUpdate).getOptions();

    for (int i = 0; i < currentOptions.size(); i++) {
      QuestionOption current = currentOptions.get(i);
      LocalizedStrings updatedTranslations =
          current.optionText().updateTranslation(updatedLocale, this.options.get(i));
      updatedOptionsBuilder.add(current.toBuilder().setOptionText(updatedTranslations).build());
    }

    return partiallyUpdated.setQuestionOptions(updatedOptionsBuilder.build());
  }
}
