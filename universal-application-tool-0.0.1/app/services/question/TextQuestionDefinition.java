package services.question;

import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.OptionalInt;
import java.util.OptionalLong;
import services.Path;

public class TextQuestionDefinition extends QuestionDefinition {

  private OptionalInt minLength = OptionalInt.empty();
  private OptionalInt maxLength = OptionalInt.empty();

  public TextQuestionDefinition(
      OptionalLong id,
      long version,
      String name,
      Path path,
      String description,
      ImmutableMap<Locale, String> questionText,
      ImmutableMap<Locale, String> questionHelpText,
      ImmutableMap<ValidationPredicate, String> validationPredicates) {
    super(
        id, version, name, path, description, questionText, questionHelpText, validationPredicates);
  }

  public TextQuestionDefinition(
      long version,
      String name,
      Path path,
      String description,
      ImmutableMap<Locale, String> questionText,
      ImmutableMap<Locale, String> questionHelpText,
      ImmutableMap<ValidationPredicate, String> validationPredicates) {
    super(version, name, path, description, questionText, questionHelpText, validationPredicates);
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

  public OptionalInt getMinLength() {
    return minLength;
  }

  public void setMinLength(int minLength) {
    this.minLength = OptionalInt.of(minLength);
  }

  public OptionalInt getMaxLength() {
    return maxLength;
  }

  public void setMaxLength(int maxLength) {
    this.maxLength = OptionalInt.of(maxLength);
  }
}
