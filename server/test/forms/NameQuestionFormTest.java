package forms;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import org.junit.Test;
import services.LocalizedStrings;
import services.question.types.NameQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionBuilder;
import services.question.types.QuestionDefinitionConfig;

public class NameQuestionFormTest {
  @Test
  public void getBuilder_returnsCompleteBuilder() throws Exception {
    NameQuestionForm form = new NameQuestionForm();
    form.setQuestionName("name");
    form.setQuestionDescription("description");
    form.setQuestionText("What is the question text?");
    form.setQuestionHelpText("");
    QuestionDefinitionBuilder builder = form.getBuilder();

    NameQuestionDefinition expected =
        new NameQuestionDefinition(
            QuestionDefinitionConfig.builder()
                .setName("name")
                .setDescription("description")
                .setQuestionText(LocalizedStrings.of(Locale.US, "What is the question text?"))
                .setQuestionHelpText(LocalizedStrings.empty())
                .build());

    QuestionDefinition actual = builder.build();

    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void getBuilder_withQdConstructor_returnsCompleteBuilder() throws Exception {
    NameQuestionDefinition originalQd =
        new NameQuestionDefinition(
            QuestionDefinitionConfig.builder()
                .setName("name")
                .setDescription("description")
                .setQuestionText(LocalizedStrings.of(Locale.US, "What is the question text?"))
                .setQuestionHelpText(LocalizedStrings.empty())
                .build());

    NameQuestionForm form = new NameQuestionForm(originalQd);
    QuestionDefinitionBuilder builder = form.getBuilder();

    QuestionDefinition actual = builder.build();

    assertThat(actual).isEqualTo(originalQd);
  }
}
