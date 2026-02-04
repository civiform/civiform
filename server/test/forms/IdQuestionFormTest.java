package forms;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import java.util.UUID;
import org.junit.Test;
import services.LocalizedStrings;
import services.question.types.IdQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionBuilder;
import services.question.types.QuestionDefinitionConfig;

public class IdQuestionFormTest {

  @Test
  public void getBuilder_returnsCompleteBuilder() throws Exception {
    UUID initialToken = UUID.randomUUID();
    IdQuestionForm form = new IdQuestionForm();
    form.setQuestionName("name");
    form.setQuestionDescription("description");
    form.setQuestionText("What is the question text?");
    form.setQuestionHelpText("");
    form.setConcurrencyToken(initialToken);
    form.setMinLength("4");
    form.setMaxLength("6");
    QuestionDefinitionBuilder builder = form.getBuilder();

    IdQuestionDefinition expected =
        new IdQuestionDefinition(
            QuestionDefinitionConfig.builder()
                .setName("name")
                .setDescription("description")
                .setQuestionText(LocalizedStrings.of(Locale.US, "What is the question text?"))
                .setQuestionHelpText(LocalizedStrings.empty())
                .setConcurrencyToken(initialToken)
                .setValidationPredicates(IdQuestionDefinition.IdValidationPredicates.create(4, 6))
                .build());

    QuestionDefinition actual = builder.build();

    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void getBuilder_withQdConstructor_returnsCompleteBuilder() throws Exception {
    IdQuestionDefinition originalQd =
        new IdQuestionDefinition(
            QuestionDefinitionConfig.builder()
                .setName("name")
                .setDescription("description")
                .setQuestionText(LocalizedStrings.of(Locale.US, "What is the question text?"))
                .setQuestionHelpText(LocalizedStrings.empty())
                .setConcurrencyToken(UUID.randomUUID())
                .setValidationPredicates(IdQuestionDefinition.IdValidationPredicates.create(4, 6))
                .build());

    IdQuestionForm form = new IdQuestionForm(originalQd);
    QuestionDefinitionBuilder builder = form.getBuilder();

    QuestionDefinition actual = builder.build();

    assertThat(actual).isEqualTo(originalQd);
  }

  @Test
  public void getBuilder_emptyStringMinMax_noPredicateSet() throws Exception {
    UUID initialToken = UUID.randomUUID();
    IdQuestionForm form = new IdQuestionForm();
    form.setQuestionName("name");
    form.setQuestionDescription("description");
    form.setQuestionText("What is the question text?");
    form.setQuestionHelpText("");
    form.setConcurrencyToken(initialToken);
    form.setMinLength("");
    form.setMaxLength("");
    QuestionDefinitionBuilder builder = form.getBuilder();

    IdQuestionDefinition expected =
        new IdQuestionDefinition(
            QuestionDefinitionConfig.builder()
                .setName("name")
                .setDescription("description")
                .setQuestionText(LocalizedStrings.of(Locale.US, "What is the question text?"))
                .setQuestionHelpText(LocalizedStrings.empty())
                .setConcurrencyToken(initialToken)
                .build());

    QuestionDefinition actual = builder.build();

    assertThat(actual).isEqualTo(expected);
  }
}
