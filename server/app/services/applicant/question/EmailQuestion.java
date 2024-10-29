package services.applicant.question;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import services.Path;
import services.applicant.ApplicantData;
import services.applicant.ValidationErrorMessage;
import services.question.PrimaryApplicantInfoTag;
import services.question.types.EmailQuestionDefinition;

/**
 * Represents an email question in the context of a specific applicant.
 *
 * <p>See {@link ApplicantQuestion} for details.
 */
public final class EmailQuestion extends Question {

  private Optional<String> emailValue;

  EmailQuestion(ApplicantQuestion applicantQuestion) {
    super(applicantQuestion);
  }

  @Override
  protected ImmutableMap<Path, ImmutableSet<ValidationErrorMessage>> getValidationErrorsInternal() {
    // TODO: Need to add some Email specific validation.
    return ImmutableMap.of();
  }

  @Override
  public ImmutableList<Path> getAllPaths() {
    return ImmutableList.of(getEmailPath());
  }

  public Path getEmailPath() {
    return applicantQuestion.getContextualizedPath().join(Scalar.EMAIL);
  }

  @Override
  public String getAnswerString() {
    return getEmailValue().orElse(getDefaultAnswerString());
  }

  public Optional<String> getEmailValue() {
    if (emailValue != null) {
      return emailValue;
    }

    ApplicantData applicantData = applicantQuestion.getApplicantData();
    Optional<String> emailValue = applicantData.readString(getEmailPath());

    if (emailValue.isEmpty() && isPaiQuestion()) {
      emailValue = getApplicantQuestion().getApplicant().getEmailAddress();
    }

    return emailValue;
  }

  public EmailQuestionDefinition getQuestionDefinition() {
    return (EmailQuestionDefinition) applicantQuestion.getQuestionDefinition();
  }

  private boolean isPaiQuestion() {
    return applicantQuestion
        .getQuestionDefinition()
        .containsPrimaryApplicantInfoTag(PrimaryApplicantInfoTag.APPLICANT_EMAIL);
  }
}
