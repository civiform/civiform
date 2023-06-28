package forms;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import java.util.Locale;
import java.util.OptionalLong;
import org.junit.Test;
import services.LocalizedStrings;
import services.question.QuestionOption;
import services.question.types.MultiOptionQuestionDefinition;
import services.question.types.MultiOptionQuestionDefinition.MultiOptionQuestionType;
import services.question.types.MultiOptionQuestionDefinition.MultiOptionValidationPredicates;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionBuilder;
import services.question.types.QuestionDefinitionConfig;

public class MultiOptionQuestionFormTest {

  @Test
  public void getBuilder_returnsCompleteBuilder() throws Exception {
    MultiOptionQuestionForm form = new CheckboxQuestionForm();
    form.setQuestionName("name");
    form.setQuestionDescription("description");
    form.setQuestionText("What is the question text?");
    form.setQuestionHelpText("help text");
    form.setMinChoicesRequired("1");
    form.setMaxChoicesAllowed("10");
    form.setOptions(ImmutableList.of("one", "two"));
    form.setOptionIds(ImmutableList.of(4L, 1L));
    QuestionDefinitionBuilder builder = form.getBuilder();

    QuestionDefinitionConfig config =
        QuestionDefinitionConfig.builder()
            .setName("name")
            .setDescription("description")
            .setQuestionText(LocalizedStrings.of(Locale.US, "What is the question text?"))
            .setQuestionHelpText(LocalizedStrings.of(Locale.US, "help text"))
            .setValidationPredicates(MultiOptionValidationPredicates.create(1, 10))
            .build();
    MultiOptionQuestionDefinition expected =
        new MultiOptionQuestionDefinition(
            config,
            ImmutableList.of(
                QuestionOption.create(1L, LocalizedStrings.of(Locale.US, "option one"))),
            MultiOptionQuestionType.CHECKBOX);

    QuestionDefinition actual = builder.build();

    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void getBuilder_withQdConstructor_returnsCompleteBuilder() throws Exception {
    QuestionDefinitionConfig config =
        QuestionDefinitionConfig.builder()
            .setName("name")
            .setDescription("description")
            .setQuestionText(LocalizedStrings.of(Locale.US, "What is the question text?"))
            .setQuestionHelpText(LocalizedStrings.of(Locale.US, "help text"))
            .setValidationPredicates(MultiOptionValidationPredicates.create(1, 10))
            .build();
    MultiOptionQuestionDefinition originalQd =
        new MultiOptionQuestionDefinition(
            config,
            ImmutableList.of(QuestionOption.create(1L, LocalizedStrings.of(Locale.US, "option 1"))),
            MultiOptionQuestionType.CHECKBOX);

    MultiOptionQuestionForm form = new CheckboxQuestionForm(originalQd);
    QuestionDefinitionBuilder builder = form.getBuilder();

    QuestionDefinition actual = builder.build();

    assertThat(actual).isEqualTo(originalQd);
  }

  @Test
  public void getBuilder_emptyStringMinMax_noPredicateSet() throws Exception {
    MultiOptionQuestionForm form = new CheckboxQuestionForm();
    form.setQuestionName("name");
    form.setQuestionDescription("description");
    form.setQuestionText("What is the question text?");
    form.setQuestionHelpText("help text");
    form.setMinChoicesRequired("");
    form.setMaxChoicesAllowed("");
    form.setOptions(ImmutableList.of("one", "two"));
    form.setOptionIds(ImmutableList.of(4L, 1L));
    QuestionDefinitionBuilder builder = form.getBuilder();

    QuestionDefinitionConfig config =
        QuestionDefinitionConfig.builder()
            .setName("name")
            .setDescription("description")
            .setQuestionText(LocalizedStrings.of(Locale.US, "What is the question text?"))
            .setQuestionHelpText(LocalizedStrings.of(Locale.US, "help text"))
            .setValidationPredicates(MultiOptionValidationPredicates.create())
            .build();
    MultiOptionQuestionDefinition expected =
        new MultiOptionQuestionDefinition(
            config,
            ImmutableList.of(
                QuestionOption.create(1L, LocalizedStrings.of(Locale.US, "option one"))),
            MultiOptionQuestionType.CHECKBOX);

    QuestionDefinition actual = builder.build();

    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void getBuilder_setsNextIdCorrectly_initialOptions() throws Exception {
    MultiOptionQuestionForm form = new DropdownQuestionForm();
    form.setQuestionName("name");
    form.setQuestionDescription("description");
    form.setQuestionText("What is the question text?");
    form.setQuestionHelpText("help text");
    form.setMinChoicesRequired("");
    form.setMaxChoicesAllowed("");
    form.setOptions(ImmutableList.of("one", "two"));
    form.setOptionIds(ImmutableList.of(4L, 1L));

    form.getBuilder();

    assertThat(form.getNextAvailableId()).isEqualTo(OptionalLong.of(5L));
  }

  @Test
  public void getBuilder_addNewOptions_setsNextIdCorrectly() throws Exception {
    MultiOptionQuestionForm form = new DropdownQuestionForm();
    form.setQuestionName("name");
    form.setQuestionDescription("description");
    form.setQuestionText("What is the question text?");
    form.setQuestionHelpText("help text");
    form.setMinChoicesRequired("");
    form.setMaxChoicesAllowed("");
    // Add two existing options with IDs 1 and 2
    form.setOptions(ImmutableList.of("one", "two"));
    form.setOptionIds(ImmutableList.of(1L, 2L));
    form.setNewOptions(ImmutableList.of("three", "four"));

    form.getBuilder();

    assertThat(form.getNextAvailableId()).isEqualTo(OptionalLong.of(5));
  }
}
