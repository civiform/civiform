package forms;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import java.util.Locale;
import org.junit.Test;
import services.LocalizedStrings;
import services.question.QuestionOption;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.MultiOptionQuestionDefinition;
import services.question.types.MultiOptionQuestionDefinitionConfig;
import services.question.types.MultiOptionQuestionDefinitionConfig.MultiOptionQuestionType;
import services.question.types.QuestionDefinitionBuilder;

public class CheckboxQuestionFormTest {

  @Test
  public void getBuilder_returnsCompleteBuilder() throws UnsupportedQuestionTypeException {
    CheckboxQuestionForm form = new CheckboxQuestionForm();
    form.setQuestionName("name");
    form.setQuestionDescription("description");
    form.setQuestionText("What is the question text?");
    form.setQuestionHelpText("help text");
    // Unique field
    form.setOptions(ImmutableList.of("cat", "dog", "rabbit"));
    form.setOptionIds(ImmutableList.of(1L, 2L, 3L));
    QuestionDefinitionBuilder builder = form.getBuilder();

    MultiOptionQuestionDefinitionConfig config =
        MultiOptionQuestionDefinitionConfig.builder()
            .setMultiOptionQuestionType(MultiOptionQuestionType.CHECKBOX)
            .setName("name")
            .setDescription("description")
            .setQuestionText(LocalizedStrings.of(Locale.US, "What is the question text?"))
            .setQuestionHelpText(LocalizedStrings.of(Locale.US, "help text"))
            .setQuestionOptions(
                ImmutableList.of(
                    QuestionOption.create(1L, LocalizedStrings.of(Locale.US, "cat")),
                    QuestionOption.create(2L, LocalizedStrings.of(Locale.US, "dog")),
                    QuestionOption.create(3L, LocalizedStrings.of(Locale.US, "rabbit"))))
            .build();
    MultiOptionQuestionDefinition expected = new MultiOptionQuestionDefinition(config);

    assertThat(builder.build()).isEqualTo(expected);
  }

  @Test
  public void getBuilder_withQdConstructor_returnsCompleteBuilder() throws Exception {
    MultiOptionQuestionDefinitionConfig config =
        MultiOptionQuestionDefinitionConfig.builder()
            .setMultiOptionQuestionType(MultiOptionQuestionType.CHECKBOX)
            .setName("name")
            .setDescription("description")
            .setQuestionText(LocalizedStrings.of(Locale.US, "What is the question text?"))
            .setQuestionHelpText(LocalizedStrings.of(Locale.US, "help text"))
            .setQuestionOptions(
                ImmutableList.of(
                    QuestionOption.create(1L, LocalizedStrings.of(Locale.US, "hello")),
                    QuestionOption.create(2L, LocalizedStrings.of(Locale.US, "world"))))
            .build();
    MultiOptionQuestionDefinition originalQd = new MultiOptionQuestionDefinition(config);

    CheckboxQuestionForm form = new CheckboxQuestionForm(originalQd);
    QuestionDefinitionBuilder builder = form.getBuilder();

    assertThat(builder.build()).isEqualTo(originalQd);
  }
}
