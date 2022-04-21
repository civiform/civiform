package services.applicant.question;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import services.question.types.QuestionType;

/**
 * Abstract implementation for the {@link Question} interface. Subclasses are expected to implement
 * the majority of the interface methods. Common logic is pulled up to this class for code sharing.
 */
abstract class QuestionImpl implements Question {
  protected final ApplicantQuestion applicantQuestion;

  public QuestionImpl(ApplicantQuestion applicantQuestion) {
    this.applicantQuestion = Preconditions.checkNotNull(applicantQuestion);
    if (!validQuestionTypes().contains(applicantQuestion.getType())) {
      throw new RuntimeException(
          String.format(
              "Question is not a question of the following types: [%s]: %s (type: %s)",
              Joiner.on(", ").join(validQuestionTypes().stream().toArray()),
              applicantQuestion.getQuestionDefinition().getQuestionPathSegment(),
              applicantQuestion.getQuestionDefinition().getQuestionType()));
    }
  }

  /**
   * The set of acceptable question types for the {@link ApplicantQuestion} provided in the
   * constructor. This is used for validation purposes.
   */
  protected abstract ImmutableSet<QuestionType> validQuestionTypes();

  /**
   * A question is considered answered if the applicant data has been set for any of the paths
   * associated with the question. If a question type contains no paths, then it will be considered
   * as not answered.
   */
  @Override
  public boolean isAnswered() {
    return getAllPaths().stream().anyMatch(p -> applicantQuestion.getApplicantData().hasPath(p));
  }
}
