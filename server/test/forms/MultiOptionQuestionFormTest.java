package forms;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import java.util.Locale;
import java.util.OptionalLong;
import java.util.UUID;
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
    UUID initialToken = UUID.randomUUID();
    MultiOptionQuestionForm form = new CheckboxQuestionForm();
    form.setQuestionName("name");
    form.setQuestionDescription("description");
    form.setQuestionText("What is the question text?");
    form.setQuestionHelpText("help text");
    form.setConcurrencyToken(initialToken);
    form.setMinChoicesRequired("1");
    form.setMaxChoicesAllowed("10");
    form.setOptions(ImmutableList.of("one", "two"));
    form.setOptionAdminNames(ImmutableList.of("one admin", "two admin"));
    form.setOptionIds(ImmutableList.of(4L, 1L));
    QuestionDefinitionBuilder builder = form.getBuilder();

    QuestionDefinitionConfig config =
        QuestionDefinitionConfig.builder()
            .setName("name")
            .setDescription("description")
            .setQuestionText(LocalizedStrings.of(Locale.US, "What is the question text?"))
            .setQuestionHelpText(LocalizedStrings.of(Locale.US, "help text"))
            .setConcurrencyToken(initialToken)
            .setValidationPredicates(MultiOptionValidationPredicates.create(1, 10))
            .build();
    MultiOptionQuestionDefinition expected =
        new MultiOptionQuestionDefinition(
            config,
            ImmutableList.of(
                QuestionOption.create(
                    1L, "opt1 admin", LocalizedStrings.of(Locale.US, "option one"))),
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
            .setConcurrencyToken(UUID.randomUUID())
            .setValidationPredicates(MultiOptionValidationPredicates.create(1, 10))
            .build();
    MultiOptionQuestionDefinition expectedQd =
        new MultiOptionQuestionDefinition(
            config,
            ImmutableList.of(
                QuestionOption.create(1L, "one admin", LocalizedStrings.of(Locale.US, "option 1"))),
            MultiOptionQuestionType.CHECKBOX);

    MultiOptionQuestionForm form = new CheckboxQuestionForm(expectedQd);
    QuestionDefinitionBuilder builder = form.getBuilder();
    QuestionDefinition actualQd = builder.build();

    assertThat(actualQd).isEqualTo(expectedQd);
  }

  @Test
  public void constructor_withQd_returnsCompleteForm() throws Exception {
    QuestionDefinitionConfig config =
        QuestionDefinitionConfig.builder()
            .setName("name")
            .setDescription("description")
            .setQuestionText(LocalizedStrings.of(Locale.US, "What is the question text?"))
            .setQuestionHelpText(LocalizedStrings.of(Locale.US, "help text"))
            .setConcurrencyToken(UUID.randomUUID())
            .setValidationPredicates(MultiOptionValidationPredicates.create(1, 10))
            .build();
    MultiOptionQuestionDefinition expectedQd =
        new MultiOptionQuestionDefinition(
            config,
            ImmutableList.of(
                QuestionOption.create(1L, "one admin", LocalizedStrings.of(Locale.US, "option 1")),
                QuestionOption.create(2L, "two admin", LocalizedStrings.of(Locale.US, "option 2")),
                QuestionOption.create(
                    5L, "five admin", LocalizedStrings.of(Locale.US, "option 5"))),
            MultiOptionQuestionType.CHECKBOX);

    MultiOptionQuestionForm form = new CheckboxQuestionForm(expectedQd);
    assertThat(form.getNextAvailableId().getAsLong()).isEqualTo(6L);
    assertThat(form.getOptions()).containsExactly("option 1", "option 2", "option 5");
    assertThat(form.getOptionAdminNames()).containsExactly("one admin", "two admin", "five admin");
    assertThat(form.getOptionIds()).containsExactly(1L, 2L, 5L);
  }

  @Test
  public void getBuilder_emptyStringMinMax_noPredicateSet() throws Exception {
    UUID initialToken = UUID.randomUUID();
    MultiOptionQuestionForm form = new CheckboxQuestionForm();
    form.setQuestionName("name");
    form.setQuestionDescription("description");
    form.setQuestionText("What is the question text?");
    form.setQuestionHelpText("help text");
    form.setConcurrencyToken(initialToken);
    form.setMinChoicesRequired("");
    form.setMaxChoicesAllowed("");
    form.setOptions(ImmutableList.of("one", "two"));
    form.setOptionAdminNames(ImmutableList.of("one admin", "two admin"));
    form.setOptionIds(ImmutableList.of(4L, 1L));
    QuestionDefinitionBuilder builder = form.getBuilder();

    QuestionDefinitionConfig config =
        QuestionDefinitionConfig.builder()
            .setName("name")
            .setDescription("description")
            .setQuestionText(LocalizedStrings.of(Locale.US, "What is the question text?"))
            .setQuestionHelpText(LocalizedStrings.of(Locale.US, "help text"))
            .setConcurrencyToken(initialToken)
            .build();
    MultiOptionQuestionDefinition expected =
        new MultiOptionQuestionDefinition(
            config,
            ImmutableList.of(
                QuestionOption.create(
                    1L, "one admin", LocalizedStrings.of(Locale.US, "option one"))),
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
    form.setOptionAdminNames(ImmutableList.of("one admin", "two admin"));
    form.setOptionIds(ImmutableList.of(4L, 1L));

    form.getBuilder();

    assertThat(form.getNextAvailableId()).isEqualTo(OptionalLong.of(5L));
  }

  @Test
  public void getBuilder_addNewOptions_setsIdsCorrectly() throws Exception {
    MultiOptionQuestionForm form = new DropdownQuestionForm();
    form.setQuestionName("name");
    form.setQuestionDescription("description");
    form.setQuestionText("What is the question text?");
    form.setQuestionHelpText("help text");
    form.setMinChoicesRequired("");
    form.setMaxChoicesAllowed("");
    // Add two existing options with IDs 1 and 2
    form.setOptions(ImmutableList.of("one", "two"));
    form.setOptionAdminNames(ImmutableList.of("one admin", "two admin"));
    form.setOptionIds(ImmutableList.of(1L, 2L));
    form.setNewOptions(ImmutableList.of("three", "four"));
    form.setNewOptionAdminNames(ImmutableList.of("three admin", "four admin"));
    form.setNextAvailableId(7L);

    MultiOptionQuestionDefinition questionDefinition =
        (MultiOptionQuestionDefinition) form.getBuilder().build();

    assertThat(form.getNextAvailableId()).isEqualTo(OptionalLong.of(9));
    assertThat(questionDefinition.getOptions().stream().map(QuestionOption::id))
        .containsExactly(1L, 2L, 7L, 8L);
  }

  @Test
  public void getBuilder_addNewOptions_setsAdminNamesCorrectly() throws Exception {
    MultiOptionQuestionForm form = new DropdownQuestionForm();
    form.setQuestionName("name");
    form.setQuestionDescription("description");
    form.setQuestionText("What is the question text?");
    form.setQuestionHelpText("help text");
    form.setMinChoicesRequired("");
    form.setMaxChoicesAllowed("");
    // Add two existing options with IDs 1 and 2
    form.setOptions(ImmutableList.of("one", "two"));
    form.setOptionAdminNames(ImmutableList.of("one admin", "two admin"));
    form.setOptionIds(ImmutableList.of(1L, 2L));
    form.setNewOptions(ImmutableList.of("three", "four"));
    form.setNewOptionAdminNames(ImmutableList.of("three admin", "four admin"));

    MultiOptionQuestionDefinition questionDefinition =
        (MultiOptionQuestionDefinition) form.getBuilder().build();

    assertThat(questionDefinition.getOptionAdminNames())
        .containsExactly("one admin", "two admin", "three admin", "four admin");
  }
}
