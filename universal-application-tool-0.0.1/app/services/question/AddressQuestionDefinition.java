package services.question;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Locale;

public class AddressQuestionDefinition extends QuestionDefinition {

  public AddressQuestionDefinition(
      String id,
      String version,
      String name,
      String path,
      String description,
      ImmutableMap<Locale, String> questionText,
      ImmutableMap<Locale, String> questionHelpText,
      ImmutableSet<String> tags) {
    super(id, version, name, path, description, questionText, questionHelpText);
  }

  @Override
  public QuestionType getQuestionType() {
    return QuestionType.ADDRESS;
  }

  @Override
  public ImmutableMap<String, Class> getScalars() {
    return ImmutableMap.of(
        "street", String.class, "city", String.class, "state", String.class, "zip", String.class);
  }
}
