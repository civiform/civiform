package forms;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import java.util.Locale;
import java.util.UUID;
import org.junit.Test;
import services.LocalizedStrings;
import services.question.QuestionOption;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.MultiOptionQuestionDefinition;
import services.question.types.MultiOptionQuestionDefinition.MultiOptionQuestionType;
import services.question.types.QuestionDefinitionBuilder;
import services.question.types.QuestionDefinitionConfig;

public class CheckboxQuestionFormTest {

  @Test
  public void getBuilder_returnsCompleteBuilder() throws UnsupportedQuestionTypeException {
    UUID initialToken = UUID.randomUUID();
    CheckboxQuestionForm form = new CheckboxQuestionForm();
    form.setQuestionName("name");
    form.setQuestionDescription("description");
    form.setQuestionText("What is the question text?");
    form.setQuestionHelpText("help text");
    // Unique field
    form.setOptions(ImmutableList.of("cat", "dog", "rabbit"));
    form.setOptionAdminNames(ImmutableList.of("cat admin", "dog admin", "rabbit admin"));
    form.setOptionIds(ImmutableList.of(1L, 2L, 3L));
    form.setConcurrencyToken(initialToken);
    QuestionDefinitionBuilder builder = form.getBuilder();

    QuestionDefinitionConfig config =
        QuestionDefinitionConfig.builder()
            .setName("name")
            .setDescription("description")
            .setQuestionText(LocalizedStrings.of(Locale.US, "What is the question text?"))
            .setQuestionHelpText(LocalizedStrings.of(Locale.US, "help text"))
            .setConcurrencyToken(initialToken)
            .build();

    ImmutableList<QuestionOption> questionOptions =
        ImmutableList.of(
            QuestionOption.create(1L, "cat admin", LocalizedStrings.of(Locale.US, "cat")),
            QuestionOption.create(2L, "dog admin", LocalizedStrings.of(Locale.US, "dog")),
            QuestionOption.create(3L, "rabbit admin", LocalizedStrings.of(Locale.US, "rabbit")));
    MultiOptionQuestionDefinition expected =
        new MultiOptionQuestionDefinition(
            config, questionOptions, MultiOptionQuestionType.CHECKBOX);

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
            .setConcurrencyToken(UUID.randomUUID())
            .build();

    ImmutableList<QuestionOption> questionOptions =
        ImmutableList.of(
            QuestionOption.create(1L, "hello admin", LocalizedStrings.of(Locale.US, "hello")),
            QuestionOption.create(2L, "world admin", LocalizedStrings.of(Locale.US, "world")));

    MultiOptionQuestionDefinition originalQd =
        new MultiOptionQuestionDefinition(
            config, questionOptions, MultiOptionQuestionType.CHECKBOX);

    CheckboxQuestionForm form = new CheckboxQuestionForm(originalQd);
    QuestionDefinitionBuilder builder = form.getBuilder();

    assertThat(builder.build()).isEqualTo(originalQd);
  }
}
