package services.applicant.question;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
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
public class StaticContentQuestion extends Question {

  public StaticContentQuestion(ApplicantQuestion applicantQuestion) {
    super(applicantQuestion);
  }

  @Override
  protected ImmutableSet<QuestionType> validQuestionTypes() {
    return ImmutableSet.of(QuestionType.STATIC);
  }

  @Override
  protected ImmutableMap<Path, ImmutableSet<ValidationErrorMessage>> getValidationErrorsInternal() {
    return ImmutableMap.of();
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
    return ImmutableList.of();
  }
}
