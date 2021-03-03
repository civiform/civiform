package services.question;

import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.OptionalLong;

public class AddressQuestionDefinition extends QuestionDefinition {

  public AddressQuestionDefinition(
      OptionalLong id,
      long version,
      String name,
      String path,
      String description,
      ImmutableMap<Locale, String> questionText,
      ImmutableMap<Locale, String> questionHelpText) {
    super(id, version, name, path, description, questionText, questionHelpText);
  }

  public AddressQuestionDefinition(
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
    return QuestionType.ADDRESS;
  }

  @Override
  public ImmutableMap<String, ScalarType> getScalars() {
    return ImmutableMap.of(
        getStreetPath(),
        getStreetType(),
        getCityPath(),
        getCityType(),
        getStatePath(),
        getStateType(),
        getZipPath(),
        getZipType());
  }

  public String getStreetPath() {
    return getPath() + ".street";
  }

  public ScalarType getStreetType() {
    return ScalarType.STRING;
  }

  public String getCityPath() {
    return getPath() + ".city";
  }

  public ScalarType getCityType() {
    return ScalarType.STRING;
  }

  public String getStatePath() {
    return getPath() + ".state";
  }

  public ScalarType getStateType() {
    return ScalarType.STRING;
  }

  public String getZipPath() {
    return getPath() + ".zip";
  }

  public ScalarType getZipType() {
    return ScalarType.STRING;
  }
}
