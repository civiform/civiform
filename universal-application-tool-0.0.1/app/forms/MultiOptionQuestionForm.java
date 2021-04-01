package forms;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import java.util.Locale;
import services.question.MultiOptionQuestionDefinition;
import services.question.QuestionDefinitionBuilder;
import services.question.QuestionType;

public class MultiOptionQuestionForm extends QuestionForm {
  private ImmutableList<String> answerOptions;

  public MultiOptionQuestionForm(QuestionType type) {
    super();
    setQuestionType(type);
    this.answerOptions = ImmutableList.of();
  }

  public MultiOptionQuestionForm(MultiOptionQuestionDefinition qd) {
    super(qd);
    setQuestionType(qd.getQuestionType());
    if (qd.getOptions().containsKey(Locale.US)) {
      this.answerOptions = qd.getOptions().get(Locale.US);
    } else {
      this.answerOptions = ImmutableList.of();
    }
  }

  public ImmutableList<String> getAnswerOptions() {
    return answerOptions;
  }

  public void setAnswerOptions(ImmutableList<String> answerOptions) {
    this.answerOptions = answerOptions;
  }

  @Override
  public QuestionDefinitionBuilder getBuilder() {
    return super.getBuilder()
        .setQuestionOptions(
            ImmutableListMultimap.<Locale, String>builder()
                .putAll(Locale.US, getAnswerOptions())
                .build());
  }
}
