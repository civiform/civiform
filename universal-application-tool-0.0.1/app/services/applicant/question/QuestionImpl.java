package services.applicant.question;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import services.question.types.QuestionType;

abstract class QuestionImpl implements Question {
  protected final ApplicantQuestion applicantQuestion;

  public QuestionImpl(ApplicantQuestion applicantQuestion) {
    this.applicantQuestion = applicantQuestion;
    if (!validQuestionTypes().contains(applicantQuestion.getType())) {
      throw new RuntimeException(
          String.format(
              "Question is not a question of the following types: [%s]: %s (type: %s)",
              Joiner.on(", ").join(validQuestionTypes().stream().toArray()),
              applicantQuestion.getQuestionDefinition().getQuestionPathSegment(),
              applicantQuestion.getQuestionDefinition().getQuestionType()));
    }
  }

  protected abstract ImmutableSet<QuestionType> validQuestionTypes();

  @Override
  public boolean isAnswered() {
    return getAllPaths().stream().anyMatch(p -> applicantQuestion.getApplicantData().hasPath(p));
  }
}
