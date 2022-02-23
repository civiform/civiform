package services.question.types;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.auto.value.AutoValue;
import java.util.Optional;
import java.util.OptionalLong;
import services.LocalizedStrings;

/** Defines an email question. */
public class EmailQuestionDefinition extends QuestionDefinition {

  public EmailQuestionDefinition(
      String name,
      Optional<Long> enumeratorId,
      String description,
      LocalizedStrings questionText,
      LocalizedStrings questionHelpText) {
    super(
        name,
        enumeratorId,
        description,
        questionText,
        questionHelpText,
        EmailQuestionDefinition.EmailValidationPredicates.create());
  }

  public EmailQuestionDefinition(
      OptionalLong id,
      String name,
      Optional<Long> enumeratorId,
      String description,
      LocalizedStrings questionText,
      LocalizedStrings questionHelpText) {
    super(
        id,
        name,
        enumeratorId,
        description,
        questionText,
        questionHelpText,
        EmailQuestionDefinition.EmailValidationPredicates.create());
  }

  @AutoValue
  public abstract static class EmailValidationPredicates extends ValidationPredicates {

    public static EmailQuestionDefinition.EmailValidationPredicates parse(String jsonString) {
      try {
        return mapper.readValue(
            jsonString, AutoValue_EmailQuestionDefinition_EmailValidationPredicates.class);
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }
    }

    public static EmailQuestionDefinition.EmailValidationPredicates create() {
      return new AutoValue_EmailQuestionDefinition_EmailValidationPredicates();
    }
  }

  public EmailQuestionDefinition.EmailValidationPredicates getEmailValidationPredicates() {
    return (EmailQuestionDefinition.EmailValidationPredicates) getValidationPredicates();
  }

  @Override
  public QuestionType getQuestionType() {
    return QuestionType.EMAIL;
  }
}
