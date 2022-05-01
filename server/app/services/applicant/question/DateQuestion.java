package services.applicant.question;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import services.Path;
import services.applicant.ValidationErrorMessage;
import services.question.types.DateQuestionDefinition;
import services.question.types.QuestionType;

/**
 * Represents a date question in the context of a specific applicant.
 *
 * <p>See {@link ApplicantQuestion} for details.
 */
public class DateQuestion extends QuestionImpl {

  private Optional<LocalDate> dateValue;

  public DateQuestion(ApplicantQuestion applicantQuestion) {
    super(applicantQuestion);
  }

  @Override
  protected ImmutableSet<QuestionType> validQuestionTypes() {
    return ImmutableSet.of(QuestionType.DATE);
  }

  @Override
  protected ImmutableMap<Path, ImmutableSet<ValidationErrorMessage>> getValidationErrorsInternal() {
    // When staging updates, the attempt to update ApplicantData would have failed to
    // convert to a date and been noted as a failed update. We check for that here.
    if (applicantQuestion.getApplicantData().getFailedUpdates().containsKey(getDatePath())) {
      return ImmutableMap.of(
          getDatePath(),
          ImmutableSet.of(
              ValidationErrorMessage.create(MessageKey.DATE_VALIDATION_MISFORMATTED)));
    }
    return ImmutableMap.of();
  }

  @Override
  public ImmutableList<Path> getAllPaths() {
    return ImmutableList.of(getDatePath());
  }

  public Path getDatePath() {
    return applicantQuestion.getContextualizedPath().join(Scalar.DATE);
  }

  @Override
  public String getAnswerString() {
    return getDateValue()
        .map(localDate -> localDate.format(DateTimeFormatter.ofPattern("MM/dd/yyyy")))
        .orElse("-");
  }

  public Optional<LocalDate> getDateValue() {
    if (dateValue != null) {
      return dateValue;
    }

    dateValue = applicantQuestion.getApplicantData().readDate(getDatePath());

    return dateValue;
  }

  public DateQuestionDefinition getQuestionDefinition() {
    return (DateQuestionDefinition) applicantQuestion.getQuestionDefinition();
  }
}
