package forms.translation;

import java.util.Locale;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.EnumeratorQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionBuilder;

/** Form for updating translation for enumerator questions. */
public class EnumeratorQuestionTranslationForm extends QuestionTranslationForm {

  private String entityType;

  public EnumeratorQuestionTranslationForm() {
    super();
    this.entityType = "";
  }

  public String getEntityType() {
    return entityType;
  }

  public void setEntityType(String entityType) {
    this.entityType = entityType;
  }

  @Override
  public QuestionDefinitionBuilder builderWithUpdates(
      QuestionDefinition toUpdate, Locale updatedLocale) throws UnsupportedQuestionTypeException {
    QuestionDefinitionBuilder partiallyUpdated = super.builderWithUpdates(toUpdate, updatedLocale);

    return partiallyUpdated.setEntityType(
        ((EnumeratorQuestionDefinition) toUpdate)
            .getEntityType()
            .updateTranslation(updatedLocale, this.entityType));
  }
}
