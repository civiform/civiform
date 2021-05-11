package services.applicant;

import com.google.auto.value.AutoValue;
import services.question.types.EnumeratorQuestionDefinition;

/** A repeated entity represents one of the applicant's answers to an enumerator question. */
@AutoValue
public abstract class RepeatedEntity {

  public static RepeatedEntity create(
      EnumeratorQuestionDefinition enumeratorQuestionDefinition, String entityName, int index) {
    assert enumeratorQuestionDefinition.isEnumerator();
    return new AutoValue_RepeatedEntity(enumeratorQuestionDefinition, entityName, index);
  }

  /**
   * The {@link services.question.types.QuestionType#ENUMERATOR} question definition associated with
   * this repeated entity.
   */
  public abstract EnumeratorQuestionDefinition enumeratorQuestionDefinition();

  /** The entity name provided by the applicant. */
  public abstract String entityName();

  /** The positional index with respect to its siblings. */
  public abstract int index();
}
