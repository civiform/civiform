package services.applicant.question;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import services.Path;
import services.applicant.ValidationErrorMessage;
import services.question.types.EmailQuestionDefinition;
import services.question.types.QuestionType;

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
  protected ImmutableSet<QuestionType> validQuestionTypes() {
    return ImmutableSet.of(QuestionType.EMAIL);
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
    return getEmailValue().orElse("-");
  }

  public Optional<String> getEmailValue() {
    if (emailValue != null) {
      return emailValue;
    }

    emailValue = applicantQuestion.getApplicantData().readString(getEmailPath());

    return emailValue;
  }

  public EmailQuestionDefinition getQuestionDefinition() {
    return (EmailQuestionDefinition) applicantQuestion.getQuestionDefinition();
  }
}
