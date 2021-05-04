package services.applicant.question;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import services.applicant.ValidationErrorMessage;
import services.question.types.QuestionType;
import services.question.types.RepeaterQuestionDefinition;

public class RepeaterQuestion implements PresentsErrors {

  private final ApplicantQuestion applicantQuestion;

  // TODO(#859): make this admin-configurable
  private final String PLACEHOLDER = "";

  public RepeaterQuestion(ApplicantQuestion applicantQuestion) {
    this.applicantQuestion = applicantQuestion;
    assertQuestionType();
  }

  @Override
  public boolean hasQuestionErrors() {
    return !getQuestionErrors().isEmpty();
  }

  @Override
  public boolean hasTypeSpecificErrors() {
    // There are no inherent requirements in a repeater question.
    return false;
  }

  /** No blank values are allowed. */
  public ImmutableSet<ValidationErrorMessage> getQuestionErrors() {
    if (isAnswered() && getEntityNames().stream().anyMatch(String::isBlank)) {
      return ImmutableSet.of(ValidationErrorMessage.entityNameRequired(getPlaceholder()));
    }
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

  /** Return the repeated entity names associated with this enumerator question. */
  public ImmutableList<String> getEntityNames() {
    return applicantQuestion
        .getApplicantData()
        .readRepeatedEntities(applicantQuestion.getContextualizedPath());
  }

  public String getPlaceholder() {
    return PLACEHOLDER;
  }

  @Override
  public String getAnswerString() {
    return Joiner.on("\n").join(getEntityNames());
  }
}
