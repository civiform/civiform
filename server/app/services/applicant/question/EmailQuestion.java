package services.applicant.question;

import com.google.common.collect.ImmutableList;
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
public class EmailQuestion extends QuestionImpl {

  private Optional<String> emailValue;

  public EmailQuestion(ApplicantQuestion applicantQuestion) {
    super(applicantQuestion);
  }

  @Override
  protected ImmutableSet<QuestionType> validQuestionTypes() {
    return ImmutableSet.of(QuestionType.EMAIL);
  }

  @Override
  public ImmutableSet<ValidationErrorMessage> getQuestionErrors() {
    return ImmutableSet.of();
  }

  @Override
  public ImmutableSet<ValidationErrorMessage> getAllTypeSpecificErrors() {
    // TODO: Need to add some Email specific validation.
    return ImmutableSet.of();
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
