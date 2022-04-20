package services.applicant.question;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import services.Path;
import services.applicant.ValidationErrorMessage;
import services.question.types.QuestionType;

/**
 * Represents a static content question in the context of a specific applicant.
 *
 * <p>Static content question doesn't have an answer. This class is the boilerplate needed to fit
 * static content in the question framework.
 *
 * <p>See {@link ApplicantQuestion} for details.
 */
public class StaticContentQuestion implements Question {

  private final ApplicantQuestion applicantQuestion;

  public StaticContentQuestion(ApplicantQuestion applicantQuestion) {
    this.applicantQuestion = applicantQuestion;
    assertQuestionType();
  }

  public void assertQuestionType() {
    if (!applicantQuestion.getType().equals(QuestionType.STATIC)) {
      throw new RuntimeException(
          String.format(
              "Question is not a STATIC question: %s (type: %s)",
              applicantQuestion.getQuestionDefinition().getQuestionPathSegment(),
              applicantQuestion.getQuestionDefinition().getQuestionType()));
    }
  }

  @Override
  public ImmutableSet<ValidationErrorMessage> getQuestionErrors() {
    return ImmutableSet.of();
  }

  @Override
  public ImmutableSet<ValidationErrorMessage> getAllTypeSpecificErrors() {
    return ImmutableSet.of();
  }

  @Override
  public boolean isAnswered() {
    return true;
  }

  @Override
  public String getAnswerString() {
    return "";
  }

  @Override
  public ImmutableList<Path> getAllPaths() {
    return ImmutableList.of(getPath());
  }

  public Path getPath() {
    return applicantQuestion.getContextualizedPath();
  }
}
