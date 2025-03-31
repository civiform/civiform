package forms;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import java.util.OptionalInt;
import java.util.UUID;
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
  public void getBuilder_returnsBuilderWithEnumeratorFields() throws Exception {
    EnumeratorQuestionForm form = new EnumeratorQuestionForm();
    form.setEntityType("entity type");
    form.setMinEntities("2");
    form.setMaxEntities("3");

    EnumeratorQuestionDefinition qd = (EnumeratorQuestionDefinition) form.getBuilder().build();

    assertThat(qd.getEntityType()).isEqualTo(LocalizedStrings.withDefaultValue("entity type"));
    assertThat(qd.getMinEntities()).isEqualTo(OptionalInt.of(2));
    assertThat(qd.getMaxEntities()).isEqualTo(OptionalInt.of(3));
  }

  @Test
  public void getBuilder_emptyFormInputs_returnsDefaults() throws Exception {
    EnumeratorQuestionForm form = new EnumeratorQuestionForm();
    form.setEntityType("");
    form.setMinEntities("");
    form.setMaxEntities("");

    EnumeratorQuestionDefinition qd = (EnumeratorQuestionDefinition) form.getBuilder().build();

    assertThat(qd.getEntityType()).isEqualTo(LocalizedStrings.withDefaultValue(""));
    assertThat(qd.getMinEntities()).isEqualTo(OptionalInt.empty());
    assertThat(qd.getMaxEntities()).isEqualTo(OptionalInt.empty());
  }

  @Test
  public void getBuilder_includesConcurrencyToken() throws Exception {
    UUID initialToken = UUID.randomUUID();
    EnumeratorQuestionForm form = new EnumeratorQuestionForm();
    form.setConcurrencyToken(initialToken);

    EnumeratorQuestionDefinition qd = (EnumeratorQuestionDefinition) form.getBuilder().build();

    assertThat(qd.getConcurrencyToken()).hasValue(initialToken);
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
