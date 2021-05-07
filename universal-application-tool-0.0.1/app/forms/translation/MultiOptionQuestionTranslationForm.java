package forms.translation;


import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
  public QuestionDefinition buildUpdates(QuestionDefinition definition, Locale updatedLocale)
      throws UnsupportedQuestionTypeException {
    QuestionDefinition semiUpdated = super.buildUpdates(definition, updatedLocale);
    QuestionDefinitionBuilder builder = new QuestionDefinitionBuilder(semiUpdated);
    ImmutableList<QuestionOption> currentOptions =
        ((MultiOptionQuestionDefinition) definition).getOptions();

    ImmutableList.Builder<QuestionOption> updatedOptionsBuilder = ImmutableList.builder();
    for (int i = 0; i < currentOptions.size(); i++) {
      QuestionOption current = currentOptions.get(i);
      updatedOptionsBuilder.add(
          current.toBuilder()
              .updateOptionText(current.optionText(), updatedLocale, this.options.get(i))
              .build());
    }
    return builder.setQuestionOptions(updatedOptionsBuilder.build()).build();
  }
}
