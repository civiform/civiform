package forms;

import services.LocalizedStrings;
import services.question.types.EnumeratorQuestionDefinition;
import services.question.types.QuestionDefinitionBuilder;
import services.question.types.QuestionType;

/** Form for updating an enumerator question. */
public class EnumeratorQuestionForm extends QuestionForm {
  private String entityType;

  public EnumeratorQuestionForm() {
    super();
    this.entityType = "";
  }

  public EnumeratorQuestionForm(EnumeratorQuestionDefinition qd) {
    super(qd);
    this.entityType = qd.getEntityType().isEmpty() ? "" : qd.getEntityType().getDefault();
  }

  @Override
  public QuestionType getQuestionType() {
    return QuestionType.ENUMERATOR;
  }

  public String getEntityType() {
    return entityType;
  }

  public void setEntityType(String entityType) {
    this.entityType = entityType;
  }

  @Override
  public QuestionDefinitionBuilder getBuilder() {
    return super.getBuilder().setEntityType(LocalizedStrings.withDefaultValue(this.entityType));
  }
}
