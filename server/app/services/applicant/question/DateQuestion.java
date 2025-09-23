package services.applicant.question;

import static services.question.types.DateQuestionDefinition.DateValidationOption.DateType.APPLICATION_DATE;
import static services.question.types.DateQuestionDefinition.DateValidationOption.DateType.CUSTOM;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import services.MessageKey;
import services.Path;
import services.applicant.ApplicantData;
import services.applicant.ValidationErrorMessage;
import services.question.PrimaryApplicantInfoTag;
import services.question.types.DateQuestionDefinition;
import services.question.types.DateQuestionDefinition.DateValidationOption;

/**
 * Represents a date question in the context of a specific applicant. TODO (#7266): After north star
 * launch, clean up this file so we only read from Memorable date.
 *
 * <p>See {@link ApplicantQuestion} for details.
 */
public final class DateQuestion extends AbstractQuestion {

  private Optional<LocalDate> dateValue;

  DateQuestion(ApplicantQuestion applicantQuestion) {
    super(applicantQuestion);
  }

  // Since we cannot inject Config class here to check the zone, we take the systemDefaultZone as
  // the Zone which internally uses GMT.
  // Errorprone throws error on using system related timezones, hence we suppress the warning.
  @SuppressWarnings({"JavaTimeDefaultTimeZone", "TimeInStaticInitializer"})
  private static final LocalDate CURRENT_DATE = LocalDate.now(Clock.systemDefaultZone());

  @VisibleForTesting public static int ALLOWABLE_YEAR_FOR_DATE_VALIDATION = 150;

  @Override
  protected ImmutableMap<Path, ImmutableSet<ValidationErrorMessage>> getValidationErrorsInternal() {
    ApplicantData applicantData = applicantQuestion.getApplicantData();
    // When staging updates, the attempt to update ApplicantData would have failed to
    // convert to a date and been noted as a failed update. We check for that here.
    if (applicantData.updateDidFailAt(getDatePath())) {
      return ImmutableMap.of(
          getDatePath(),
          ImmutableSet.of(
              ValidationErrorMessage.create(MessageKey.DATE_VALIDATION_INVALID_DATE_FORMAT)));
    }

    return ImmutableMap.of(getDatePath(), validateDate());
  }

  private ImmutableSet<ValidationErrorMessage> validateDate() {
    if (this.getDateValue().isEmpty()) {
      return ImmutableSet.of();
    }
    LocalDate enteredDate = dateValue.get();
    DateQuestionDefinition definition = getQuestionDefinition();
    if (definition.getMinDate().isPresent() && definition.getMaxDate().isPresent()) {
      return validateDate(
          definition.getMinDate().get(), definition.getMaxDate().get(), enteredDate);
    }
    return legacyValidateDate(enteredDate);
  }

  private ImmutableSet<ValidationErrorMessage> validateDate(
      DateValidationOption minDate, DateValidationOption maxDate, LocalDate enteredDate) {
    ImmutableSet.Builder<ValidationErrorMessage> errors = ImmutableSet.builder();
    if (minDate.dateType() == APPLICATION_DATE
        && maxDate.dateType() == APPLICATION_DATE
        && !enteredDate.isEqual(CURRENT_DATE)) {
      errors.add(ValidationErrorMessage.create(MessageKey.DATE_VALIDATION_CURRENT_DATE_REQUIRED));
      // Skip remaining checks if date is not exactly today's date
      return errors.build();
    }
    if (minDate.dateType() == CUSTOM
        && maxDate.dateType() == CUSTOM
        && minDate.customDate().isPresent()
        && maxDate.customDate().isPresent()
        && (enteredDate.isBefore(minDate.customDate().get())
            || enteredDate.isAfter(maxDate.customDate().get()))) {
      errors.add(
          ValidationErrorMessage.create(
              MessageKey.DATE_VALIDATION_DATE_NOT_IN_RANGE,
              minDate.customDate().get().toString(),
              maxDate.customDate().get().toString()));
      // Skip remaining checks if date is not within specific range
      return errors.build();
    }
    // Date must be later than min
    if (minDate.dateType() == APPLICATION_DATE && enteredDate.isBefore(CURRENT_DATE)) {
      errors.add(ValidationErrorMessage.create(MessageKey.DATE_VALIDATION_FUTURE_DATE_REQUIRED));
    }
    if (minDate.dateType() == CUSTOM
        && minDate.customDate().isPresent()
        && enteredDate.isBefore(minDate.customDate().get())) {
      errors.add(
          ValidationErrorMessage.create(
              MessageKey.DATE_VALIDATION_DATE_TOO_FAR_IN_PAST,
              minDate.customDate().get().toString()));
    }
    // Date must be earlier than max
    if (maxDate.dateType() == APPLICATION_DATE && enteredDate.isAfter(CURRENT_DATE)) {
      errors.add(ValidationErrorMessage.create(MessageKey.DATE_VALIDATION_PAST_DATE_REQUIRED));
    }
    if (maxDate.dateType() == CUSTOM
        && maxDate.customDate().isPresent()
        && enteredDate.isAfter(maxDate.customDate().get())) {
      errors.add(
          ValidationErrorMessage.create(
              MessageKey.DATE_VALIDATION_DATE_TOO_FAR_IN_FUTURE,
              maxDate.customDate().get().toString()));
    }

    return errors.build();
  }

  /**
   * Legacy date validation that checks the date is within +/- 150 years. This should only used for
   * date questions created before custom date validation is available, new questions should use
   * {@link #validateDate()} with specific min/max parameters instead.
   */
  @Deprecated
  private ImmutableSet<ValidationErrorMessage> legacyValidateDate(LocalDate enteredDate) {
    ImmutableSet.Builder<ValidationErrorMessage> errors = ImmutableSet.builder();
    if (enteredDate.isBefore(CURRENT_DATE.minusYears(ALLOWABLE_YEAR_FOR_DATE_VALIDATION))) {
      errors.add(
          ValidationErrorMessage.create(
              MessageKey.DATE_VALIDATION_DATE_BEYOND_ALLOWABLE_YEARS_IN_PAST,
              ALLOWABLE_YEAR_FOR_DATE_VALIDATION));
    }

    if (enteredDate.isAfter(CURRENT_DATE.plusYears(ALLOWABLE_YEAR_FOR_DATE_VALIDATION))) {
      errors.add(
          ValidationErrorMessage.create(
              MessageKey.DATE_VALIDATION_DATE_BEYOND_ALLOWABLE_YEARS_IN_FUTURE,
              ALLOWABLE_YEAR_FOR_DATE_VALIDATION));
    }
    return errors.build();
  }

  @Override
  public ImmutableList<Path> getAllPaths() {
    return ImmutableList.of(getDatePath());
  }

  public Path getDatePath() {
    return applicantQuestion.getContextualizedPath().join(Scalar.DATE);
  }

  public Path getYearPath() {
    return applicantQuestion.getContextualizedPath().join(Scalar.YEAR);
  }

  public Path getMonthPath() {
    return applicantQuestion.getContextualizedPath().join(Scalar.MONTH);
  }

  public Path getDayPath() {
    return applicantQuestion.getContextualizedPath().join(Scalar.DAY);
  }

  @Override
  public String getAnswerString() {
    return getDateValue()
        .map(localDate -> localDate.format(DateTimeFormatter.ofPattern("MM/dd/yyyy")))
        .orElse(getDefaultAnswerString());
  }

  public Optional<LocalDate> getDateValue() {
    if (dateValue != null) {
      return dateValue;
    }

    ApplicantData applicantData = applicantQuestion.getApplicantData();
    dateValue = applicantData.readDate(getDatePath());

    if (dateValue.isEmpty() && isPaiQuestion()) {
      dateValue = getApplicantQuestion().getApplicant().getDateOfBirth();
    }
    
    // Autofill with current date if both minDate and maxDate are APPLICATION_DATE
    if (dateValue.isEmpty() && shouldAutofillWithCurrentDate()) {
      dateValue = Optional.of(LocalDate.now(ZoneId.systemDefault()));
    }
    
    return dateValue;
  }

  public Optional<Integer> getMonthValue() {
    return getDateValue()
        .map(localDate -> Optional.of(localDate.getMonthValue()))
        .orElse(Optional.empty());
  }

  public Optional<Integer> getYearValue() {
    return getDateValue()
        .map(localDate -> Optional.of(localDate.getYear()))
        .orElse(Optional.empty());
  }

  public Optional<Integer> getDayValue() {
    return getDateValue()
        .map(localDate -> Optional.of(localDate.getDayOfMonth()))
        .orElse(Optional.empty());
  }

  public DateQuestionDefinition getQuestionDefinition() {
    return (DateQuestionDefinition) applicantQuestion.getQuestionDefinition();
  }

  private boolean isPaiQuestion() {
    return applicantQuestion
        .getQuestionDefinition()
        .containsPrimaryApplicantInfoTag(PrimaryApplicantInfoTag.APPLICANT_DOB);
  }

  /**
   * This method will check to see if the date field should be autofilled with the current date.
   * This will occer only when both minDate and maxDate are set to APPLICATION_DATE and
   * there is no existing applicant date value.
   */
  private boolean shouldAutofillWithCurrentDate() {
    DateQuestionDefinition definition = getQuestionDefinition();
    Optional<DateValidationOption> minDate = definition.getMinDate();
    Optional<DateValidationOption> maxDate = definition.getMaxDate();
    
    return minDate.isPresent() 
        && maxDate.isPresent()
        && minDate.get().dateType() == APPLICATION_DATE
        && maxDate.get().dateType() == APPLICATION_DATE;
  }
}
