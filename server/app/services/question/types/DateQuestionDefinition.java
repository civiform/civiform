package services.question.types;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import java.time.LocalDate;
import java.util.Optional;

/** Defines a date question. */
public final class DateQuestionDefinition extends QuestionDefinition {

  public DateQuestionDefinition(@JsonProperty("config") QuestionDefinitionConfig config) {
    super(config);
  }

  @JsonDeserialize(
      builder = AutoValue_DateQuestionDefinition_DateValidationPredicates.Builder.class)
  @AutoValue
  public abstract static class DateValidationPredicates extends ValidationPredicates {

    public static DateValidationPredicates parse(String jsonString) {
      try {
        return mapper.readValue(
            jsonString, AutoValue_DateQuestionDefinition_DateValidationPredicates.class);
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }
    }

    public static DateValidationPredicates create() {
      return builder().build();
    }

    public static DateValidationPredicates create(
        Optional<DateValidationOption> minDate, Optional<DateValidationOption> maxDate) {
      return builder().setMinDate(minDate).setMaxDate(maxDate).build();
    }

    @JsonProperty("minDate")
    public abstract Optional<DateValidationOption> minDate();

    @JsonProperty("maxDate")
    public abstract Optional<DateValidationOption> maxDate();

    public static Builder builder() {
      return new AutoValue_DateQuestionDefinition_DateValidationPredicates.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
      @JsonProperty("minDate")
      public abstract Builder setMinDate(Optional<DateValidationOption> minDate);

      public abstract Builder setMinDate(DateValidationOption minDate);

      @JsonProperty("maxDate")
      public abstract Builder setMaxDate(Optional<DateValidationOption> maxDate);

      public abstract Builder setMaxDate(DateValidationOption maxDate);

      public abstract DateValidationPredicates build();
    }
  }

  @JsonDeserialize(builder = AutoValue_DateQuestionDefinition_DateValidationOption.Builder.class)
  @AutoValue
  public abstract static class DateValidationOption {
    @JsonProperty("dateType")
    public abstract DateType dateType();

    // Required and used iff dateType is CUSTOM.
    // Dates serialized to ISO-8601 date without time zone format. "Ex: 2025-06-25"
    @JsonProperty("customDate")
    public abstract Optional<LocalDate> customDate();

    public enum DateType {
      ANY, // Any min or max date
      APPLICATION_DATE, // The current date that the applicant is applying
      CUSTOM // A custom date specified in the customDate field
    }

    public static Builder builder() {
      return new AutoValue_DateQuestionDefinition_DateValidationOption.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
      @JsonProperty("dateType")
      public abstract Builder setDateType(DateType dateType);

      @JsonProperty("customDate")
      public abstract Builder setCustomDate(Optional<LocalDate> customDate);

      public abstract DateValidationOption build();
    }
  }

  @Override
  public QuestionType getQuestionType() {
    return QuestionType.DATE;
  }

  @Override
  ValidationPredicates getDefaultValidationPredicates() {
    return DateValidationPredicates.create();
  }

  @JsonIgnore
  public Optional<DateValidationOption> getMinDate() {
    return getDateValidationPredicates().minDate();
  }

  @JsonIgnore
  public Optional<DateValidationOption> getMaxDate() {
    return getDateValidationPredicates().maxDate();
  }

  @JsonIgnore
  private DateValidationPredicates getDateValidationPredicates() {
    return (DateValidationPredicates) getValidationPredicates();
  }
}
