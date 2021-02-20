package services.question;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Optional;

public class AddressQuestionDefinition extends QuestionDefinition {

  @JsonCreator
  public AddressQuestionDefinition(
      @JsonProperty("id") long id,
      @JsonProperty("version") long version,
      @JsonProperty("name") String name,
      @JsonProperty("path") String path,
      @JsonProperty("description") String description,
      @JsonProperty("questionText") ImmutableMap<Locale, String> questionText,
      @JsonProperty("questionHelpText") Optional<ImmutableMap<Locale, String>> questionHelpText) {
    super(id, version, name, path, description, questionText, questionHelpText);
  }

  @Override
  public QuestionType getQuestionType() {
    return QuestionType.ADDRESS;
  }

  @Override
  public ImmutableMap<String, ScalarType> getScalars() {
    return ImmutableMap.of(
        "street",
        ScalarType.STRING,
        "city",
        ScalarType.STRING,
        "state",
        ScalarType.STRING,
        "zip",
        ScalarType.STRING);
  }
}
