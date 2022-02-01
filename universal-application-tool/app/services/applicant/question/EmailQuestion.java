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
public class EmailQuestion implements Question {

  private final ApplicantQuestion applicantQuestion;
  private Optional<String> emailValue;

  public EmailQuestion(ApplicantQuestion applicantQuestion) {
    this.applicantQuestion = applicantQuestion;
    assertQuestionType();
  }

  @Override
  public boolean hasConditionErrors() {
    return !getQuestionErrors().isEmpty();
  }

  @Override
  public ImmutableSet<ValidationErrorMessage> getQuestionErrors() {
    return ImmutableSet.of();
  }

  @Override
  public boolean hasTypeSpecificErrors() {
    return !getAllTypeSpecificErrors().isEmpty();
  }

  @Override
  public ImmutableSet<ValidationErrorMessage> getAllTypeSpecificErrors() {
    // TODO: Need to add some Email specific validation.
    return ImmutableSet.of();
  }

  @Override
  public boolean isAnswered() {
    return applicantQuestion.getApplicantData().hasPath(getEmailPath());
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
    assertQuestionType();
    return (EmailQuestionDefinition) applicantQuestion.getQuestionDefinition();
  }

  public void assertQuestionType() {
    if (!applicantQuestion.getType().equals(QuestionType.EMAIL)) {
      throw new RuntimeException(
          String.format(
              "Question is not an Email question: %s (type: %s)",
              applicantQuestion.getQuestionDefinition().getQuestionPathSegment(),
              applicantQuestion.getQuestionDefinition().getQuestionType()));
    }
  }
}
