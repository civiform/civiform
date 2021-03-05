package services.question;

import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.OptionalLong;
import services.Path;

public class NameQuestionDefinition extends QuestionDefinition {

  public NameQuestionDefinition(
      OptionalLong id,
      long version,
      String name,
      Path path,
      String description,
      ImmutableMap<Locale, String> questionText,
      ImmutableMap<Locale, String> questionHelpText) {
    super(id, version, name, path, description, questionText, questionHelpText);
  }

  public NameQuestionDefinition(
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
    return QuestionType.NAME;
  }

  @Override
  public ImmutableMap<Path, ScalarType> getScalars() {
    return ImmutableMap.of(
        getFirstNamePath(),
        getFirstNameType(),
        getMiddleNamePath(),
        getMiddleNameType(),
        getLastNamePath(),
        getLastNameType());
  }

  public Path getFirstNamePath() {
    return getPath().toBuilder().append(".first").build();
  }

  public ScalarType getFirstNameType() {
    return ScalarType.STRING;
  }

  public Path getMiddleNamePath() {
    return getPath().toBuilder().append(".middle").build();
  }

  public ScalarType getMiddleNameType() {
    return ScalarType.STRING;
  }

  public Path getLastNamePath() {
    return getPath().toBuilder().append(".last").build();
  }

  public ScalarType getLastNameType() {
    return ScalarType.STRING;
  }
}
