package services.question;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Locale;

public class NameQuestionDefinition extends QuestionDefinition {

  public NameQuestionDefinition(
      String id,
      String version,
      String name,
      String path,
      String description,
      ImmutableMap<Locale, String> questionText,
      ImmutableMap<Locale, String> questionHelpText,
      ImmutableSet<String> tags) {
    super(id, version, name, path, description, questionText, questionHelpText, tags);
  }

  @Override
  public QuestionType getQuestionType() {
    return QuestionType.NAME;
  }

  @Override
  public ImmutableMap<String, Class> getScalars() {
    return ImmutableMap.of("first", String.class, "middle", String.class, "last", String.class);
  }
}
