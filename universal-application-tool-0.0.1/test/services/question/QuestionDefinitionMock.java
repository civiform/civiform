package services.question;

import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Optional;

public class QuestionDefinitionMock extends QuestionDefinition {
  public QuestionDefinitionMock(
      long id,
      String version,
      String name,
      String path,
      String description,
      ImmutableMap<Locale, String> questionText,
      Optional<ImmutableMap<Locale, String>> questionHelpText) {
    super(id, version, name, path, description, questionText, questionHelpText);
  }
}
