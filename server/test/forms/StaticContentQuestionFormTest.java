package forms;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import org.junit.Test;
import services.LocalizedStrings;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionConfig;
import services.question.types.StaticContentQuestionDefinition;

public class StaticContentQuestionFormTest {
  @Test
  public void getBuilder_returnsCompleteBuilder() throws Exception {
    StaticContentQuestionForm form = new StaticContentQuestionForm();
    form.setQuestionName("name");
    form.setQuestionDescription("description");
    form.setQuestionText("Some text. Not an actual question.");
    form.setQuestionHelpText("");
    QuestionDefinition actual = form.getBuilder().build();

    StaticContentQuestionDefinition expected =
        new StaticContentQuestionDefinition(
            QuestionDefinitionConfig.builder()
                .setName("name")
                .setDescription("description")
                .setQuestionText(
                    LocalizedStrings.of(Locale.US, "Some text. Not an actual question."))
                .setQuestionHelpText(LocalizedStrings.empty())
                .build());

    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void getBuilder_withQdConstructor_returnsCompleteBuilder() throws Exception {
    StaticContentQuestionDefinition originalQd =
        new StaticContentQuestionDefinition(
            QuestionDefinitionConfig.builder()
                .setName("name")
                .setDescription("description")
                .setQuestionText(
                    LocalizedStrings.of(Locale.US, "Some text. Not an actual question."))
                .setQuestionHelpText(LocalizedStrings.empty())
                .build());

    StaticContentQuestionForm form = new StaticContentQuestionForm(originalQd);
    QuestionDefinition actual = form.getBuilder().build();

    assertThat(actual).isEqualTo(originalQd);
  }
}
