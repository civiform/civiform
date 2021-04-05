package services.question;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.OptionalLong;
import models.LifecycleStage;
import services.Path;

public class RadioButtonQuestionDefinition extends MultiOptionQuestionDefinition {

  public RadioButtonQuestionDefinition(
      OptionalLong id,
      long version,
      String name,
      Path path,
      String description,
      LifecycleStage lifecycleStage,
      ImmutableMap<Locale, String> questionText,
      ImmutableMap<Locale, String> questionHelpText,
      ImmutableListMultimap<Locale, String> options) {
    super(
        id,
        version,
        name,
        path,
        description,
        lifecycleStage,
        questionText,
        questionHelpText,
        options);
  }

  public RadioButtonQuestionDefinition(
      long version,
      String name,
      Path path,
      String description,
      LifecycleStage lifecycleStage,
      ImmutableMap<Locale, String> questionText,
      ImmutableMap<Locale, String> questionHelpText,
      ImmutableListMultimap<Locale, String> options) {
    super(
        version, name, path, description, lifecycleStage, questionText, questionHelpText, options);
  }

  @Override
  public QuestionType getQuestionType() {
    return QuestionType.RADIO_BUTTON;
  }

  @Override
  public ScalarType getSelectionType() {
    return ScalarType.STRING;
  }
}
