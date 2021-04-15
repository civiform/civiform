package services.applicant.question;

import services.Path;
import services.question.types.QuestionType;
import services.question.types.RepeaterQuestionDefinition;

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

  public RepeaterQuestionDefinition getQuestionDefinition() {
    assertQuestionType();
    return (RepeaterQuestionDefinition) applicantQuestion.getQuestionDefinition();
  }

  public Path getRepeaterPath() {
    return getQuestionDefinition().getPath();
  }

  @Override
  public boolean isAnswered() {
    // TODO(https://github.com/seattle-uat/civiform/issues/783): Use hydrated path.
    return applicantQuestion.getApplicantData().hasPath(getRepeaterPath());
  }
}
