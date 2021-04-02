package forms;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import java.util.List;
import java.util.Locale;
import services.question.MultiOptionQuestionDefinition;
import services.question.QuestionDefinitionBuilder;
import services.question.QuestionType;

public abstract class MultiOptionQuestionForm extends QuestionForm {
  private List<String> options;

  protected MultiOptionQuestionForm(QuestionType type) {
    super();
    setQuestionType(type);
    this.options = ImmutableList.of();
  }

  protected MultiOptionQuestionForm(MultiOptionQuestionDefinition qd) {
    super(qd);
    setQuestionType(qd.getQuestionType());
    if (qd.getOptions().containsKey(Locale.US)) {
      this.options = qd.getOptions().get(Locale.US);
    } else {
      this.options = ImmutableList.of();
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
