package forms;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import org.junit.Test;
import services.LocalizedStrings;
import services.question.types.NumberQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionBuilder;
import services.question.types.QuestionDefinitionConfig;

public class NumberQuestionFormTest {

  @Test
  public void getBuilder_returnsCompleteBuilder() throws Exception {
    NumberQuestionForm form = new NumberQuestionForm();
    form.setQuestionName("name");
    form.setQuestionDescription("description");
    form.setQuestionText("What is the question text?");
    form.setQuestionHelpText("");
    form.setMin("2");
    form.setMax("8");
    QuestionDefinitionBuilder builder = form.getBuilder();

    NumberQuestionDefinition expected =
        new NumberQuestionDefinition(
            QuestionDefinitionConfig.builder()
                .setName("name")
                .setDescription("description")
                .setQuestionText(LocalizedStrings.of(Locale.US, "What is the question text?"))
                .setQuestionHelpText(LocalizedStrings.empty())
                .setValidationPredicates(
                    NumberQuestionDefinition.NumberValidationPredicates.create(2, 8))
                .build());

    QuestionDefinition actual = builder.build();

    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void getBuilder_withQdConstructor_returnsCompleteBuilder() throws Exception {
    NumberQuestionDefinition originalQd =
        new NumberQuestionDefinition(
            QuestionDefinitionConfig.builder()
                .setName("name")
                .setDescription("description")
                .setQuestionText(LocalizedStrings.of(Locale.US, "What is the question text?"))
                .setQuestionHelpText(LocalizedStrings.empty())
                .setValidationPredicates(
                    NumberQuestionDefinition.NumberValidationPredicates.create(2, 8))
                .build());

    NumberQuestionForm form = new NumberQuestionForm(originalQd);
    QuestionDefinitionBuilder builder = form.getBuilder();

    QuestionDefinition actual = builder.build();

    assertThat(actual).isEqualTo(originalQd);
  }

  @Test
  public void getBuilder_emptyStringMinMax_noPredicateSet() throws Exception {
    NumberQuestionForm form = new NumberQuestionForm();
    form.setQuestionName("name");
    form.setQuestionDescription("description");
    form.setQuestionText("What is the question text?");
    form.setQuestionHelpText("");
    form.setMin("");
    form.setMax("");
    QuestionDefinitionBuilder builder = form.getBuilder();

    NumberQuestionDefinition expected =
        new NumberQuestionDefinition(
            QuestionDefinitionConfig.builder()
                .setName("name")
                .setDescription("description")
                .setQuestionText(LocalizedStrings.of(Locale.US, "What is the question text?"))
                .setQuestionHelpText(LocalizedStrings.empty())
                .build());

    QuestionDefinition actual = builder.build();

    assertThat(actual).isEqualTo(expected);
  }
}
