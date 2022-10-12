package services.question.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import java.time.Instant;
import java.util.Optional;
import java.util.OptionalLong;
import services.LocalizedStrings;

/** Defines an address question. */
public final class AddressQuestionDefinition extends QuestionDefinition {

  public AddressQuestionDefinition(
      OptionalLong id,
      String name,
      Optional<Long> enumeratorId,
      String description,
      LocalizedStrings questionText,
      LocalizedStrings questionHelpText,
      AddressValidationPredicates validationPredicates,
      Optional<Instant> lastModifiedTime) {
    super(
        id,
        name,
        enumeratorId,
        description,
        questionText,
        questionHelpText,
        validationPredicates,
        lastModifiedTime);
  }

  public AddressQuestionDefinition(
      String name,
      Optional<Long> enumeratorId,
      String description,
      LocalizedStrings questionText,
      LocalizedStrings questionHelpText,
      AddressValidationPredicates validationPredicates) {
    super(name, enumeratorId, description, questionText, questionHelpText, validationPredicates);
  }

  public AddressQuestionDefinition(
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
        AddressValidationPredicates.create());
  }

  @JsonDeserialize(
      builder = AutoValue_AddressQuestionDefinition_AddressValidationPredicates.Builder.class)
  @AutoValue
  public abstract static class AddressValidationPredicates extends ValidationPredicates {

    public static AddressValidationPredicates parse(String jsonString) {
      try {
        return mapper.readValue(
            jsonString, AutoValue_AddressQuestionDefinition_AddressValidationPredicates.class);
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }
    }

    public static AddressValidationPredicates create() {
      return builder().setDisallowPoBox(false).setDefaultState(Optional.empty()).build();
    }

    public static AddressValidationPredicates create(boolean disallowPoBox) {
      return builder().setDisallowPoBox(disallowPoBox).setDefaultState(Optional.empty()).build();
    }
    public static AddressValidationPredicates create(boolean disallowPoBox, Optional<String> defaultState) {
      return builder().setDisallowPoBox(disallowPoBox).setDefaultState(defaultState).build();
    }

    @JsonProperty("disallowPoBox")
    public abstract Optional<Boolean> disallowPoBox();
    @JsonProperty("defaultState")
    public abstract Optional<String> defaultState();

    public static Builder builder() {
      return new AutoValue_AddressQuestionDefinition_AddressValidationPredicates.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {

      @JsonProperty("disallowPoBox")
      public abstract Builder setDisallowPoBox(boolean disallowPoBox);

      public abstract AddressValidationPredicates build();

      @JsonProperty("defaultState")
      public abstract Builder setDefaultState(Optional<String> defaultState);
    }
  }

  public AddressValidationPredicates getAddressValidationPredicates() {
    return (AddressValidationPredicates) getValidationPredicates();
  }

  @Override
  public QuestionType getQuestionType() {
    return QuestionType.ADDRESS;
  }

  public boolean getDisallowPoBox() {
    return getAddressValidationPredicates().disallowPoBox().orElse(false);
  }
  public Optional<String> getDefaultState() {
    return getAddressValidationPredicates().defaultState();
  }
}
