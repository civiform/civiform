package services.question;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.OptionalLong;
import services.Path;

public class DropdownQuestionDefinition extends MultiOptionQuestionDefinition {

  public DropdownQuestionDefinition(
      OptionalLong id,
      long version,
      String name,
      Path path,
      String description,
      ImmutableMap<Locale, String> questionText,
      ImmutableMap<Locale, String> questionHelpText,
      ImmutableListMultimap<Locale, String> options) {
    super(id, version, name, path, description, questionText, questionHelpText, options);
  }

  public DropdownQuestionDefinition(
      long version,
      String name,
      Path path,
      String description,
      ImmutableMap<Locale, String> questionText,
      ImmutableMap<Locale, String> questionHelpText,
      ImmutableListMultimap<Locale, String> options) {
    super(version, name, path, description, questionText, questionHelpText, options);
  }

  @Override
  public QuestionType getQuestionType() {
    return QuestionType.DROPDOWN;
  }

  @Override
  public ScalarType getSelectionType() {
    return ScalarType.STRING;
  }
}
