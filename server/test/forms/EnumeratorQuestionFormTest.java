package forms;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import org.junit.Test;
import services.LocalizedStrings;
import services.question.types.EnumeratorQuestionDefinition;
import services.question.types.QuestionDefinitionConfig;

public class EnumeratorQuestionFormTest {
  @Test
  public void getEntityType_returnsEntityTypeFromQuestionDefinition() {
    QuestionDefinitionConfig config = makeQuestionDefinitionConfig();
    EnumeratorQuestionForm form =
        new EnumeratorQuestionForm(
            new EnumeratorQuestionDefinition(
                config, LocalizedStrings.withDefaultValue("entity type")));
    assertThat(form.getEntityType()).isEqualTo("entity type");
  }

  @Test
  public void getEntityType_returnsDefaultWhenMissingInQuestionDefinition() {
    QuestionDefinitionConfig config = makeQuestionDefinitionConfig();
    EnumeratorQuestionForm form =
        new EnumeratorQuestionForm(
            new EnumeratorQuestionDefinition(config, LocalizedStrings.empty()));
    assertThat(form.getEntityType()).isEqualTo("");
  }

  @Test
  public void getBuilder_returnsBuilderWithEntityType() throws Exception {
    EnumeratorQuestionForm form = new EnumeratorQuestionForm();
    form.setEntityType("entity type");
    EnumeratorQuestionDefinition qd = (EnumeratorQuestionDefinition) form.getBuilder().build();
    assertThat(qd.getEntityType()).isEqualTo(LocalizedStrings.withDefaultValue("entity type"));
  }

  /** Returns a QuestionDefinitionConfig with minimal required fields set */
  private QuestionDefinitionConfig makeQuestionDefinitionConfig() {
    return QuestionDefinitionConfig.builder()
        .setName("name")
        .setDescription("description")
        .setQuestionText(LocalizedStrings.of(Locale.US, "question text"))
        .build();
  }
}
