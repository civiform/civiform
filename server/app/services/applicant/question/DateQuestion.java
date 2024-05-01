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
 * Represents a date question in the context of a specific applicant. TODO (#7266): After north star
 * launch, clean up this file so we only read from Memorable date.
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
    ApplicantData applicantData = applicantQuestion.getApplicantData();
    // When staging updates, the attempt to update ApplicantData would have failed to
    // convert to a date and been noted as a failed update. We check for that here.
    // TODO(#7356): Implement client side validation to prevent invalid dates from being entered,
    // since
    // it's difficult to separate which input is causing issues on the server.
    if (applicantData.updateDidFailAt(getDatePath())) {
      return ImmutableMap.of(
          getDatePath(),
          ImmutableSet.of(
              ValidationErrorMessage.create(MessageKey.DATE_VALIDATION_INVALID_DATE_FORMAT)));
    }
    return ImmutableMap.of();
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

  // Load the date value. We first check the single input value, if present. If that isn't there,
  // we check the memorable date component. In the Thymeleaf implementation, we clear the single
  // input path, which ensures that if we want to use the memorable date, it is the only one
  // present.
  public Optional<LocalDate> getDateValue() {
    if (dateValue != null) {
      return dateValue;
    }

    ApplicantData applicantData = applicantQuestion.getApplicantData();
    dateValue = applicantData.readDate(getDatePath());

    if (dateValue.isEmpty() && isPaiQuestion()) {
      dateValue = applicantData.getDateOfBirth();
    }
    return dateValue;
  }

  // Rather than reading directly from the month input, this checks the date value and extracts the
  // month, to be able to load the Month value if it was set by the user prior to migration to the
  // Memorable date component.
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
