package forms;

import com.google.common.collect.ImmutableListMultimap;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import services.question.MultiOptionQuestionDefinition;
import services.question.QuestionDefinitionBuilder;
import services.question.QuestionType;

public abstract class MultiOptionQuestionForm extends QuestionForm {
  // TODO(https://github.com/seattle-uat/civiform/issues/354): Handle other locales besides
  // Locale.US
  // Caution: This must be a mutable list type, or else Play's form binding cannot add elements to
  // the list. This means the constructors MUST set this field to a mutable List type, NOT
  // ImmutableList.
  private List<String> options;

  protected MultiOptionQuestionForm(QuestionType type) {
    super();
    setQuestionType(type);
    this.options = new ArrayList<>();
  }

  protected MultiOptionQuestionForm(MultiOptionQuestionDefinition qd) {
    super(qd);
    setQuestionType(qd.getQuestionType());
    if (qd.getOptions().containsKey(Locale.US)) {
      this.options = new ArrayList<>();
      this.options.addAll(qd.getOptions().get(Locale.US));
    } else {
      this.options = new ArrayList<>();
    }
  }

  public List<String> getOptions() {
    return this.options;
  }

  public void setOptions(List<String> options) {
    this.options = options;
  }

  @Override
  public QuestionDefinitionBuilder getBuilder() {
    return super.getBuilder()
        .setQuestionOptions(
            ImmutableListMultimap.<Locale, String>builder()
                .putAll(Locale.US, getOptions())
                .build());
  }
}
