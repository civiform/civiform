package services.question.types;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import java.util.Optional;
import java.util.OptionalLong;
import services.LocalizedStrings;

public class PhoneNumberQuestionDefinition extends QuestionDefinition {

  public PhoneNumberQuestionDefinition(
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
        PhoneNumberValidationPredicates.create());
  }

  @JsonDeserialize(
      builder =
          AutoValue_PhoneNumberQuestionDefinition_PhoneNumberValidationPredicates.Builder.class)
  @AutoValue
  public abstract static class PhoneNumberValidationPredicates extends ValidationPredicates {

    public static PhoneNumberValidationPredicates parse(String jsonString) {
      try {
        return mapper.readValue(
            jsonString,
            AutoValue_PhoneNumberQuestionDefinition_PhoneNumberValidationPredicates.class);
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }
    }

    public static PhoneNumberValidationPredicates create() {
      return builder().build();
    }

    public static Builder builder() {
      return new AutoValue_PhoneNumberQuestionDefinition_PhoneNumberValidationPredicates.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
      public abstract PhoneNumberValidationPredicates build();
    }
  }

  public PhoneNumberValidationPredicates getPhoneNumberValidationPredicates() {
    return (PhoneNumberValidationPredicates) getValidationPredicates();
  }

  @Override
  public QuestionType getQuestionType() {
    return QuestionType.PHONENUMBER;
  }
}
