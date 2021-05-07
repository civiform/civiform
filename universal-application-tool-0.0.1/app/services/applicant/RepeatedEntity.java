package services.applicant;

import com.google.auto.value.AutoValue;
import services.question.types.QuestionDefinition;

/** A repeated entity represents one of the applicant's answers to an enumerator question. */
@AutoValue
public abstract class RepeatedEntity {

  public static RepeatedEntity create(
      QuestionDefinition enumeratorQuestionDefinition, String entityName) {
    assert enumeratorQuestionDefinition.isEnumerator();
    return new AutoValue_RepeatedEntity(enumeratorQuestionDefinition, entityName);
  }

  /**
   * The {@link services.question.types.QuestionType#ENUMERATOR} question definition associated with
   * this repeated entity.
   */
  public abstract QuestionDefinition enumeratorQuestionDefinition();

  /** The entity name provided by the applicant. */
  public abstract String entityName();
}
