package forms;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import java.util.Locale;
import org.junit.Test;
import services.LocalizedStrings;
import services.question.QuestionOption;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.MultiOptionQuestionDefinition;
import services.question.types.MultiOptionQuestionDefinition.MultiOptionQuestionType;
import services.question.types.MultiOptionQuestionDefinition.MultiOptionValidationPredicates;
import services.question.types.QuestionDefinitionBuilder;
import services.question.types.QuestionDefinitionConfig;

public class RadioButtonQuestionFormTest {

  @Test
  public void getBuilder_returnsCompleteBuilder() throws UnsupportedQuestionTypeException {
    RadioButtonQuestionForm form = new RadioButtonQuestionForm();
    form.setQuestionName("name");
    form.setQuestionDescription("description");
    form.setQuestionText("What is the question text?");
    form.setQuestionHelpText("help text");
    // Unique field
    form.setOptions(ImmutableList.of("cat", "dog", "rabbit"));
    form.setOptionIds(ImmutableList.of(1L, 2L, 3L));
    QuestionDefinitionBuilder builder = form.getBuilder();

    QuestionDefinitionConfig config =
        QuestionDefinitionConfig.builder()
            .setName("name")
            .setDescription("description")
            .setQuestionText(LocalizedStrings.of(Locale.US, "What is the question text?"))
            .setQuestionHelpText(LocalizedStrings.of(Locale.US, "help text"))
            .setValidationPredicates(MultiOptionValidationPredicates.create())
            .build();

    ImmutableList<QuestionOption> questionOptions =
        ImmutableList.of(
            QuestionOption.create(1L, LocalizedStrings.of(Locale.US, "cat")),
            QuestionOption.create(2L, LocalizedStrings.of(Locale.US, "dog")),
            QuestionOption.create(3L, LocalizedStrings.of(Locale.US, "rabbit")));
    MultiOptionQuestionDefinition expected =
        new MultiOptionQuestionDefinition(
            config, questionOptions, MultiOptionQuestionType.RADIO_BUTTON);

    assertThat(builder.build()).isEqualTo(expected);
  }

  @Test
  public void getBuilder_withQdConstructor_returnsCompleteBuilder() throws Exception {
    QuestionDefinitionConfig config =
        QuestionDefinitionConfig.builder()
            .setName("name")
            .setDescription("description")
            .setQuestionText(LocalizedStrings.of(Locale.US, "What is the question text?"))
            .setQuestionHelpText(LocalizedStrings.of(Locale.US, "help text"))
            .setValidationPredicates(MultiOptionValidationPredicates.create())
            .build();

    ImmutableList<QuestionOption> questionOptions =
        ImmutableList.of(
            QuestionOption.create(1L, LocalizedStrings.of(Locale.US, "hello")),
            QuestionOption.create(1L, LocalizedStrings.of(Locale.US, "world")));

    MultiOptionQuestionDefinition originalQd =
        new MultiOptionQuestionDefinition(
            config, questionOptions, MultiOptionQuestionType.RADIO_BUTTON);

    RadioButtonQuestionForm form = new RadioButtonQuestionForm(originalQd);
    QuestionDefinitionBuilder builder = form.getBuilder();

    assertThat(builder.build()).isEqualTo(originalQd);
  }
}
