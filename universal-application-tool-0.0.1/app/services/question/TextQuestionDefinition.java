package services.question;

import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;

public class TextQuestionDefinition extends QuestionDefinition {

  public TextQuestionDefinition(
      OptionalLong id,
      long version,
      String name,
      String path,
      String description,
      ImmutableMap<Locale, String> questionText,
      Optional<ImmutableMap<Locale, String>> questionHelpText) {
    super(id, version, name, path, description, questionText, questionHelpText);
  }

  @Override
  public QuestionType getQuestionType() {
    return QuestionType.TEXT;
  }

  @Override
  public ImmutableMap<String, ScalarType> getScalars() {
    return ImmutableMap.of("text", ScalarType.STRING);
  }
}
