package services.applicant.question;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import services.MessageKey;
import services.Path;
import services.applicant.ApplicantData;
import services.applicant.ValidationErrorMessage;
import services.question.PrimaryApplicantInfoTag;
import services.question.types.DateQuestionDefinition;

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
  @SuppressWarnings("JavaTimeDefaultTimeZone")
  private static LocalDate CURRENT_DATE = LocalDate.now(Clock.systemDefaultZone());

  public static int ALLOWABLE_YEAR_FOR_DATE_VALIDATION = 150;

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
    if (this.getDateValue().isPresent()) {

      LocalDate enteredDate = dateValue.get();
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

    return ImmutableSet.of();
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
}
