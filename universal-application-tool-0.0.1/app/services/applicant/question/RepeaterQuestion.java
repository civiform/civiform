package services.applicant.question;

import services.question.types.QuestionType;

public class RepeaterQuestion implements PresentsErrors {

  private final ApplicantQuestion applicantQuestion;

  public RepeaterQuestion(ApplicantQuestion applicantQuestion) {
    this.applicantQuestion = applicantQuestion;
    assertQuestionType();
  }

  @Override
  public boolean hasQuestionErrors() {
    return false;
  }

  @Override
  public boolean hasTypeSpecificErrors() {
    // There are no inherent requirements in a text question.
    return false;
  }

  public void assertQuestionType() {
    if (!applicantQuestion.getType().equals(QuestionType.REPEATER)) {
      throw new RuntimeException(
          String.format(
              "Question is not a REPEATER question: %s (type: %s)",
              applicantQuestion.getQuestionDefinition().getPath(),
              applicantQuestion.getQuestionDefinition().getQuestionType()));
    }
  }
}
