package services.applicant.question;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
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
 * Represents a date question in the context of a specific applicant.
 *
 * <p>See {@link ApplicantQuestion} for details.
 */
public final class DateQuestion extends Question {

  private Optional<LocalDate> dateValue;

  DateQuestion(ApplicantQuestion applicantQuestion) {
    super(applicantQuestion);
  }

  @Override
  protected ImmutableMap<Path, ImmutableSet<ValidationErrorMessage>> getValidationErrorsInternal() {
    // When staging updates, the attempt to update ApplicantData would have failed to
    // convert to a date and been noted as a failed update. We check for that here.
    if (applicantQuestion.getApplicantData().updateDidFailAt(getDatePath())) {
      return ImmutableMap.of(
          getDatePath(),
          ImmutableSet.of(
              ValidationErrorMessage.create(MessageKey.DATE_VALIDATION_INVALID_DATE_FORMAT)));
    }
    return ImmutableMap.of(
        getMonthPath(), validateMonth(),
        getDayPath(), validateDay(),
        getYearPath(), validateYear());
  }

  private ImmutableSet<ValidationErrorMessage> validateMonth() {
    ApplicantData applicantData = applicantQuestion.getApplicantData();

    if (applicantData.readString(getMonthPath()).isEmpty()) {
      return ImmutableSet.of(
          ValidationErrorMessage.create(MessageKey.DATE_VALIDATION_MONTH_REQUIRED));
    }

    return ImmutableSet.of();
  }

  private ImmutableSet<ValidationErrorMessage> validateYear() {
    ApplicantData applicantData = applicantQuestion.getApplicantData();

    if (applicantData.readString(getYearPath()).isEmpty()) {
      return ImmutableSet.of(
          ValidationErrorMessage.create(MessageKey.DATE_VALIDATION_YEAR_REQUIRED));
    }

    return ImmutableSet.of();
  }

  private ImmutableSet<ValidationErrorMessage> validateDay() {
    ApplicantData applicantData = applicantQuestion.getApplicantData();

    if (applicantData.readString(getDayPath()).isEmpty()) {
      return ImmutableSet.of(
          ValidationErrorMessage.create(MessageKey.DATE_VALIDATION_DAY_REQUIRED));
    }

    return ImmutableSet.of();
  }

  @Override
  public ImmutableList<Path> getAllPaths() {
    return ImmutableList.of(getDatePath(), getMonthPath(), getDayPath(), getYearPath());
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

    // TODO (#7266): After north star launch, clean up this section so we only read from memorable
    // date.
    ApplicantData applicantData = applicantQuestion.getApplicantData();
    dateValue = applicantData.readDate(getDatePath());

    // In the North Star UI
    if (dateValue.isEmpty()) {
      Optional<Long> yearValue = getYearValue();
      Optional<Long> monthValue = getMonthValue();
      Optional<Long> dayValue = getDayValue();
      if (yearValue.isPresent() && monthValue.isPresent() && dayValue.isPresent()) {
        dateValue =
            Optional.of(
                LocalDate.of(
                    yearValue.get().intValue(),
                    monthValue.get().intValue(),
                    dayValue.get().intValue()));
      }
    }

    if (dateValue.isEmpty() && isPaiQuestion()) {
      dateValue = applicantData.getDateOfBirth();
    }
    return dateValue;
  }

  public Optional<Long> getMonthValue() {
    ApplicantData applicantData = applicantQuestion.getApplicantData();
    return applicantData.readLong(getMonthPath());
  }

  public Optional<Long> getYearValue() {
    ApplicantData applicantData = applicantQuestion.getApplicantData();
    return applicantData.readLong(getYearPath());
  }

  public Optional<Long> getDayValue() {
    ApplicantData applicantData = applicantQuestion.getApplicantData();
    return applicantData.readLong(getDayPath());
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
