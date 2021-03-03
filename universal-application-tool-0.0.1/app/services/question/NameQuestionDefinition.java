package services.question;

import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.OptionalLong;

public class NameQuestionDefinition extends QuestionDefinition {

  public NameQuestionDefinition(
      OptionalLong id,
      long version,
      String name,
      String path,
      String description,
      ImmutableMap<Locale, String> questionText,
      ImmutableMap<Locale, String> questionHelpText) {
    super(id, version, name, path, description, questionText, questionHelpText);
  }

  public NameQuestionDefinition(
      long version,
      String name,
      String path,
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
  public ImmutableMap<String, ScalarType> getScalars() {
    return ImmutableMap.of(
        getFirstNamePath(),
        getFirstNameType(),
        getMiddleNamePath(),
        getMiddleNameType(),
        getLastNamePath(),
        getLastNameType());
  }

  public String getFirstNamePath() {
    return getPath() + ".first";
  }

  public ScalarType getFirstNameType() {
    return ScalarType.STRING;
  }

  public String getMiddleNamePath() {
    return getPath() + ".middle";
  }

  public ScalarType getMiddleNameType() {
    return ScalarType.STRING;
  }

  public String getLastNamePath() {
    return getPath() + ".last";
  }

  public ScalarType getLastNameType() {
    return ScalarType.STRING;
  }
}
