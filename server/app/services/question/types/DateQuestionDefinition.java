package services.question.types;

import static services.question.types.DateQuestionDefinition.DateValidationOption.DateType.CUSTOM;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;
import services.CiviFormError;
import services.question.types.DateQuestionDefinition.DateValidationOption.DateType;

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

  @Override
  ImmutableSet<CiviFormError> internalValidate(Optional<QuestionDefinition> previousDefinition) {
    ImmutableSet.Builder<CiviFormError> errors = new ImmutableSet.Builder<>();
    Optional<DateValidationOption> min = getMinDate();
    Optional<DateValidationOption> max = getMaxDate();
    // Skip checks if no settings are present
    if (min.isEmpty() && max.isEmpty()) {
      return errors.build();
    }

    if (min.isEmpty() || max.isEmpty()) {
      errors.add(CiviFormError.of("Both start and end date are required"));
      return errors.build();
    }
    DateValidationOption minDateOption = min.get();
    DateValidationOption maxDateOption = max.get();
    // Since we cannot inject Config class here to check the zone, we take the systemDefaultZone as
    // the Zone which internally uses GMT and add a buffer of +/- 1 day.
    // Errorprone throws error on using system related timezones, hence we suppress the warning.
    @SuppressWarnings("JavaTimeDefaultTimeZone")
    LocalDate currentDate = LocalDate.now(Clock.systemDefaultZone());

    if ((minDateOption.dateType() != CUSTOM && minDateOption.customDate().isPresent())
        || (maxDateOption.dateType() != CUSTOM && maxDateOption.customDate().isPresent())) {
      errors.add(
          CiviFormError.of(
              "Specific date must be empty if start and end date are not \"Custom date\""));
    }
    if ((minDateOption.dateType() == CUSTOM && !hasValidDate(minDateOption))
        || (maxDateOption.dateType() == CUSTOM && !hasValidDate(maxDateOption))) {
      errors.add(CiviFormError.of("A valid date is required for custom start and end dates"));
    } else {
      // At least one custom date is present. Check that custom dates represent valid date ranges.
      if (minDateOption.dateType() == DateType.CUSTOM
          && maxDateOption.dateType() == DateType.CUSTOM
          && minDateOption.customDate().get().isAfter(maxDateOption.customDate().get())) {
        errors.add(CiviFormError.of("Start date cannot be after end date"));
      }
      if (minDateOption.dateType() == DateType.CUSTOM
          && maxDateOption.dateType() == DateType.APPLICATION_DATE
          && minDateOption.customDate().get().isAfter(currentDate.plusDays(1))) {
        errors.add(CiviFormError.of("Start date cannot be after today's date"));
      }
      if (minDateOption.dateType() == DateType.APPLICATION_DATE
          && maxDateOption.dateType() == DateType.CUSTOM
          && maxDateOption.customDate().get().isBefore(currentDate.minusDays(1))) {
        errors.add(CiviFormError.of("End date cannot be before today's date"));
      }
    }
    return errors.build();
  }

  private boolean hasValidDate(DateValidationOption option) {
    // Date may be empty if any of the date parts were missing, or if they did not represent a valid
    // LocalDate
    return option.customDate().isPresent()
        && option.customDate().get().getYear() > 999 // Year must be a positive 4-digit number
        && option.customDate().get().getYear() < 10000;
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
