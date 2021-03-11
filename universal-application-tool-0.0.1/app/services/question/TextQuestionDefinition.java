package services.question;

import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.OptionalLong;
import services.Path;

public class TextQuestionDefinition extends QuestionDefinition {

  public TextQuestionDefinition(
      OptionalLong id,
      long version,
      String name,
      Path path,
      String description,
      ImmutableMap<Locale, String> questionText,
      ImmutableMap<Locale, String> questionHelpText) {
    super(id, version, name, path, description, questionText, questionHelpText);
  }

  public TextQuestionDefinition(
      long version,
      String name,
      Path path,
      String description,
      ImmutableMap<Locale, String> questionText,
      ImmutableMap<Locale, String> questionHelpText) {
    super(version, name, path, description, questionText, questionHelpText);
  }

  @Override
  public QuestionType getQuestionType() {
    return QuestionType.TEXT;
  }

  @Override
  public ImmutableMap<Path, ScalarType> getScalars() {
    return ImmutableMap.of(getTextPath(), getTextType());
  }

  public Path getTextPath() {
    return getPath();
  }

  public ScalarType getTextType() {
    return ScalarType.STRING;
  }

  public int getMinLength() {
    return 0;
  }

  public int getMaxLength() {
    return 20;
  }
}
