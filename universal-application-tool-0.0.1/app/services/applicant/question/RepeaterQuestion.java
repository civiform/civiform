package services.applicant.question;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Locale;
import services.LocalizationUtils;
import services.MessageKey;
import services.applicant.ValidationErrorMessage;
import services.question.types.QuestionType;
import services.question.types.RepeaterQuestionDefinition;

public class RepeaterQuestion implements PresentsErrors {

  private final ApplicantQuestion applicantQuestion;

  // TODO(#859): make this admin-configurable
  private final ImmutableMap<Locale, String> PLACEHOLDER =
      ImmutableMap.of(LocalizationUtils.DEFAULT_LOCALE, "");

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
    return !getAllTypeSpecificErrors().isEmpty();
  }

  @Override
  public ImmutableSet<ValidationErrorMessage> getAllTypeSpecificErrors() {
    // There are no inherent requirements in a repeater question.
    return ImmutableSet.of();
  }

  /** No blank values are allowed. */
  @Override
  public ImmutableSet<ValidationErrorMessage> getQuestionErrors() {
    if (isAnswered() && getEntityNames().stream().anyMatch(String::isBlank)) {
      return ImmutableSet.of(ValidationErrorMessage.create(MessageKey.EMPTY_ENTITY_NAME));
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

  public String getPlaceholder(Locale locale) {
    return PLACEHOLDER.containsKey(locale)
        ? PLACEHOLDER.get(locale)
        : PLACEHOLDER.get(LocalizationUtils.DEFAULT_LOCALE);
  }

  @Override
  public String getAnswerString() {
    return Joiner.on("\n").join(getEntityNames());
  }
}
