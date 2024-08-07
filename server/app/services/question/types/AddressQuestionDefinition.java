package services.question.types;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import java.util.Optional;

/** Defines an address question. */
public final class AddressQuestionDefinition extends QuestionDefinition {

  public AddressQuestionDefinition(@JsonProperty("config") QuestionDefinitionConfig config) {
    super(config);
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
      return builder().setDisallowPoBox(false).build();
    }

    public static AddressValidationPredicates create(boolean disallowPoBox) {
      return builder().setDisallowPoBox(disallowPoBox).build();
    }

    @JsonProperty("disallowPoBox")
    public abstract Optional<Boolean> disallowPoBox();

    public static Builder builder() {
      return new AutoValue_AddressQuestionDefinition_AddressValidationPredicates.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {

      @JsonProperty("disallowPoBox")
      public abstract Builder setDisallowPoBox(boolean disallowPoBox);

      public abstract AddressValidationPredicates build();
    }
  }

  @Override
  public QuestionType getQuestionType() {
    return QuestionType.ADDRESS;
  }

  @Override
  ValidationPredicates getDefaultValidationPredicates() {
    return AddressValidationPredicates.create();
  }

  @JsonIgnore
  public boolean getDisallowPoBox() {
    return getAddressValidationPredicates().disallowPoBox().orElse(false);
  }

  @JsonIgnore
  private AddressValidationPredicates getAddressValidationPredicates() {
    return (AddressValidationPredicates) getValidationPredicates();
  }
}
