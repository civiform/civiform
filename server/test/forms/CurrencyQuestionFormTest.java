package forms;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import java.util.UUID;
import org.junit.Test;
import services.LocalizedStrings;
import services.question.types.CurrencyQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionBuilder;
import services.question.types.QuestionDefinitionConfig;

public class CurrencyQuestionFormTest {

  @Test
  public void getBuilder_returnsCompleteBuilder() throws Exception {
    UUID initialToken = UUID.randomUUID();
    CurrencyQuestionForm form = new CurrencyQuestionForm();
    form.setQuestionName("name");
    form.setQuestionDescription("description");
    form.setQuestionText("What is the question text?");
    form.setQuestionHelpText("");
    form.setConcurrencyToken(initialToken);
    QuestionDefinitionBuilder builder = form.getBuilder();

    CurrencyQuestionDefinition expected =
        new CurrencyQuestionDefinition(
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

  @Test
  public void getBuilder_withQdConstructor_returnsCompleteBuilder() throws Exception {
    CurrencyQuestionDefinition originalQd =
        new CurrencyQuestionDefinition(
            QuestionDefinitionConfig.builder()
                .setName("name")
                .setDescription("description")
                .setQuestionText(LocalizedStrings.of(Locale.US, "What is the question text?"))
                .setQuestionHelpText(LocalizedStrings.empty())
                .setConcurrencyToken(UUID.randomUUID())
                .build());

    CurrencyQuestionForm form = new CurrencyQuestionForm(originalQd);
    QuestionDefinitionBuilder builder = form.getBuilder();

    QuestionDefinition actual = builder.build();

    assertThat(actual).isEqualTo(originalQd);
  }
}
