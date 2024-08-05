package forms;

import java.util.OptionalInt;
import services.LocalizedStrings;
import services.question.types.EnumeratorQuestionDefinition;
import services.question.types.QuestionDefinitionBuilder;
import services.question.types.QuestionType;

/** Form for updating an enumerator question. */
public class EnumeratorQuestionForm extends QuestionForm {
  private String entityType;
  private OptionalInt minEntities;
  private OptionalInt maxEntities;

  public EnumeratorQuestionForm() {
    super();
    this.entityType = "";
    this.minEntities = OptionalInt.empty();
    this.maxEntities = OptionalInt.empty();
  }

  public EnumeratorQuestionForm(EnumeratorQuestionDefinition qd) {
    super(qd);
    this.entityType = qd.getEntityType().isEmpty() ? "" : qd.getEntityType().getDefault();
    this.minEntities = qd.getMinEntities();
    this.maxEntities = qd.getMaxEntities();
  }

  @Override
  public QuestionType getQuestionType() {
    return QuestionType.ENUMERATOR;
  }

  public String getEntityType() {
    return entityType;
  }

  public OptionalInt getMinEntities() {
    return minEntities;
  }

  public OptionalInt getMaxEntities() {
    return maxEntities;
  }

  public void setMinEntities(String minEntitiesAsString) {
    this.minEntities =
        minEntitiesAsString.isEmpty()
            ? OptionalInt.empty()
            : OptionalInt.of(Integer.parseInt(minEntitiesAsString));
  }

  public void setMaxEntities(String maxEntitiesAsString) {
    this.maxEntities =
        maxEntitiesAsString.isEmpty()
            ? OptionalInt.empty()
            : OptionalInt.of(Integer.parseInt(maxEntitiesAsString));
  }

  public void setEntityType(String entityType) {
    this.entityType = entityType;
  }

  @Override
  public QuestionDefinitionBuilder getBuilder() {
    EnumeratorQuestionDefinition.EnumeratorValidationPredicates validationPredicates =
        EnumeratorQuestionDefinition.EnumeratorValidationPredicates.builder()
            .setMinEntities(getMinEntities())
            .setMaxEntities(getMaxEntities())
            .build();

    return super.getBuilder()
        .setEntityType(LocalizedStrings.withDefaultValue(this.entityType))
        .setValidationPredicates(validationPredicates);
  }
}
