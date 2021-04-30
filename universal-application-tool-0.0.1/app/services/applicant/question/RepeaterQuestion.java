package services.applicant.question;

import com.google.common.collect.ImmutableSet;
import play.i18n.Messages;
import services.question.types.QuestionType;
import services.question.types.RepeaterQuestionDefinition;

public class RepeaterQuestion implements PresentsErrors {

  private final ApplicantQuestion applicantQuestion;

  public RepeaterQuestion(ApplicantQuestion applicantQuestion) {
    this.applicantQuestion = applicantQuestion;
    assertQuestionType();
  }

  @Override
  public boolean hasQuestionErrors(Messages messages) {
    return !getQuestionErrors(messages).isEmpty();
  }

  @Override
  public ImmutableSet<String> getQuestionErrors(Messages messages) {
    return ImmutableSet.of();
  }

  @Override
  public boolean hasTypeSpecificErrors(Messages messages) {
    return !getAllTypeSpecificErrors(messages).isEmpty();
  }

  @Override
  public ImmutableSet<String> getAllTypeSpecificErrors(Messages messages) {
    // There are no inherent requirements in a repeater question.
    return ImmutableSet.of();
  }

  public void assertQuestionType() {
    if (!applicantQuestion.getType().equals(QuestionType.REPEATER)) {
      throw new RuntimeException(
          String.format(
              "Question is not a REPEATER question: %s (type: %s)",
              applicantQuestion.getQuestionDefinition().getQuestionPathSegment(),
              applicantQuestion.getQuestionDefinition().getQuestionType()));
    }
  }

  public RepeaterQuestionDefinition getQuestionDefinition() {
    assertQuestionType();
    return (RepeaterQuestionDefinition) applicantQuestion.getQuestionDefinition();
  }

  /** This is answered if there is at least one entity name stored. */
  @Override
  public boolean isAnswered() {
    return applicantQuestion
        .getApplicantData()
        .hasPath(applicantQuestion.getContextualizedPath().atIndex(0).join(Scalar.ENTITY_NAME));
  }
}
