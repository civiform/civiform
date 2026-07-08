package forms.questions;

import java.util.OptionalInt;
import java.util.OptionalLong;
import services.LocalizedStrings;
import services.question.types.EnumeratorQuestionDefinition;
import services.question.types.QuestionDefinitionBuilder;
import services.question.types.QuestionType;

/** Form for updating an enumerator question. */
public class EnumeratorQuestionForm extends QuestionForm {
  private String entityType;
  private OptionalInt minEntities;
  private OptionalInt maxEntities;
  // Only populated by the "Create repeated set" flow on the block edit page; unused elsewhere.
  private OptionalLong initialQuestionId;
  private boolean initialQuestionWasNewlyCreated;
  public static final int MAX_ENUM_ENTITIES_ALLOWED = 50;

  public EnumeratorQuestionForm() {
    super();
    this.entityType = "";
    this.minEntities = OptionalInt.empty();
    this.maxEntities = OptionalInt.empty();
    this.initialQuestionId = OptionalLong.empty();
    this.initialQuestionWasNewlyCreated = false;
  }

  public EnumeratorQuestionForm(EnumeratorQuestionDefinition qd) {
    super(qd);
    this.entityType = qd.getEntityType().isEmpty() ? "" : qd.getEntityType().getDefault();
    this.minEntities = qd.getMinEntities();
    this.maxEntities = qd.getMaxEntities();
    this.initialQuestionId = OptionalLong.empty();
    this.initialQuestionWasNewlyCreated = false;
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
    return maxEntities.isPresent() ? maxEntities : OptionalInt.of(MAX_ENUM_ENTITIES_ALLOWED);
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
            ? OptionalInt.of(MAX_ENUM_ENTITIES_ALLOWED)
            : OptionalInt.of(Integer.parseInt(maxEntitiesAsString));
  }

  public void setEntityType(String entityType) {
    this.entityType = entityType;
  }

  public OptionalLong getInitialQuestionId() {
    return initialQuestionId;
  }

  public void setInitialQuestionId(String initialQuestionIdAsString) {
    this.initialQuestionId =
        initialQuestionIdAsString.isEmpty()
            ? OptionalLong.empty()
            : OptionalLong.of(Long.parseLong(initialQuestionIdAsString));
  }

  public boolean getInitialQuestionWasNewlyCreated() {
    return initialQuestionWasNewlyCreated;
  }

  public void setInitialQuestionWasNewlyCreated(boolean initialQuestionWasNewlyCreated) {
    this.initialQuestionWasNewlyCreated = initialQuestionWasNewlyCreated;
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
