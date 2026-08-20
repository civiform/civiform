package forms.questions;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import org.junit.Test;
import services.CiviFormError;
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

  private static MultiOptionQuestionDefinition definitionWithScoredOptions() {
    QuestionDefinitionConfig config =
        QuestionDefinitionConfig.builder()
            .setName("name")
            .setDescription("description")
            .setQuestionText(LocalizedStrings.of(Locale.US, "What is the question text?"))
            .setQuestionHelpText(LocalizedStrings.of(Locale.US, "help text"))
            .setConcurrencyToken(UUID.randomUUID())
            .build();
    return new MultiOptionQuestionDefinition(
        config,
        ImmutableList.of(
            QuestionOption.create(
                /* id= */ 1L,
                /* displayOrder= */ 0L,
                /* adminName= */ "one admin",
                /* optionText= */ LocalizedStrings.of(Locale.US, "option 1"),
                /* displayInAnswerOptions= */ Optional.of(true),
                /* score= */ Optional.of(3.5)),
            QuestionOption.create(
                /* id= */ 2L,
                /* displayOrder= */ 1L,
                /* adminName= */ "two admin",
                /* optionText= */ LocalizedStrings.of(Locale.US, "option 2"),
                /* displayInAnswerOptions= */ Optional.of(true),
                /* score= */ Optional.of(4.0)),
            QuestionOption.create(
                /* id= */ 5L,
                /* displayOrder= */ 2L,
                /* adminName= */ "five admin",
                /* optionText= */ LocalizedStrings.of(Locale.US, "option 5"),
                /* displayInAnswerOptions= */ Optional.of(true),
                /* score= */ Optional.of(-2.0))),
        MultiOptionQuestionType.CHECKBOX);
  }

  @Test
  public void constructor_withQd_populatesOptionScoresInDisplayOrder() {
    MultiOptionQuestionForm form = new CheckboxQuestionForm(definitionWithScoredOptions());

    assertThat(form.getOptions()).containsExactly("option 1", "option 2", "option 5");
    // Whole values populate without a trailing .0 (formatScore).
    assertThat(form.getOptionScores()).containsExactly("3.5", "4", "-2");
  }

  @Test
  public void getBuilder_scoringEnabled_appliesScores() throws Exception {
    MultiOptionQuestionForm form = new CheckboxQuestionForm();
    form.setQuestionName("name");
    form.setQuestionDescription("description");
    form.setQuestionText("What is the question text?");
    form.setQuestionHelpText("help text");
    form.setMinChoicesRequired("");
    form.setMaxChoicesAllowed("");
    form.setOptions(ImmutableList.of("one", "two", "three"));
    form.setOptionAdminNames(ImmutableList.of("one admin", "two admin", "three admin"));
    form.setOptionIds(ImmutableList.of(1L, 2L, 3L));
    form.setOptionScores(ImmutableList.of("4", "2", "0"));
    form.setNewOptions(ImmutableList.of("four"));
    form.setNewOptionAdminNames(ImmutableList.of("four admin"));
    form.setNewOptionScores(ImmutableList.of("-7.25"));

    MultiOptionQuestionDefinition questionDefinition =
        (MultiOptionQuestionDefinition) form.getBuilder(/* scoringEnabled= */ true).build();

    assertThat(questionDefinition.getOptions().stream().map(QuestionOption::score))
        .containsExactly(Optional.of(4.0), Optional.of(2.0), Optional.of(0.0), Optional.of(-7.25));
  }

  @Test
  public void getBuilder_scoringEnabled_allowsAllBlankScores() throws Exception {
    MultiOptionQuestionForm form = new CheckboxQuestionForm();
    form.setQuestionName("name");
    form.setQuestionDescription("description");
    form.setQuestionText("What is the question text?");
    form.setQuestionHelpText("help text");
    form.setMinChoicesRequired("");
    form.setMaxChoicesAllowed("");
    form.setOptions(ImmutableList.of("one", "two", "three"));
    form.setOptionAdminNames(ImmutableList.of("one admin", "two admin", "three admin"));
    form.setOptionIds(ImmutableList.of(1L, 2L, 3L));
    form.setOptionScores(ImmutableList.of("", "", ""));
    form.setNewOptions(ImmutableList.of("four"));
    form.setNewOptionAdminNames(ImmutableList.of("four admin"));
    form.setNewOptionScores(ImmutableList.of(""));

    MultiOptionQuestionDefinition questionDefinition =
        (MultiOptionQuestionDefinition) form.getBuilder(/* scoringEnabled= */ true).build();

    assertThat(questionDefinition.getOptions().stream().map(QuestionOption::score))
        .containsExactly(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
  }

  @Test
  public void getBuilder_scoringDisabled_discardsSubmittedScores() throws Exception {
    MultiOptionQuestionForm form = new CheckboxQuestionForm();
    form.setQuestionName("name");
    form.setQuestionDescription("description");
    form.setQuestionText("What is the question text?");
    form.setQuestionHelpText("help text");
    form.setMinChoicesRequired("");
    form.setMaxChoicesAllowed("");
    form.setOptions(ImmutableList.of("one", "two"));
    form.setOptionAdminNames(ImmutableList.of("one admin", "two admin"));
    form.setOptionIds(ImmutableList.of(1L, 2L));
    form.setOptionScores(ImmutableList.of("4", "5"));

    MultiOptionQuestionDefinition viaDefaultOverload =
        (MultiOptionQuestionDefinition) form.getBuilder().build();
    MultiOptionQuestionDefinition viaExplicitFalse =
        (MultiOptionQuestionDefinition) form.getBuilder(/* scoringEnabled= */ false).build();

    assertThat(viaDefaultOverload.getOptions().stream().map(QuestionOption::score))
        .containsExactly(Optional.empty(), Optional.empty());
    assertThat(viaExplicitFalse.getOptions().stream().map(QuestionOption::score))
        .containsExactly(Optional.empty(), Optional.empty());
  }

  @Test
  public void getOptionScoreErrors_mismatchedScoreCount_returnsError() {
    MultiOptionQuestionForm form = new CheckboxQuestionForm();
    form.setQuestionName("name");
    form.setQuestionDescription("description");
    form.setQuestionText("What is the question text?");
    form.setQuestionHelpText("help text");
    form.setMinChoicesRequired("");
    form.setMaxChoicesAllowed("");
    form.setOptions(ImmutableList.of("one", "two"));
    form.setOptionAdminNames(ImmutableList.of("one admin", "two admin"));
    form.setOptionIds(ImmutableList.of(1L, 2L));
    form.setOptionScores(ImmutableList.of("1", "2", ""));

    assertThat(form.getOptionScoreErrors())
        .extracting(CiviFormError::message)
        .containsExactly(
            "When creating a scored question, all options must include scores",
            "The number of option scores must match the number of options");
  }

  @Test
  public void getOptionScoreErrors_missingScoreLists_returnsError() {
    // Score validation only runs when the score inputs were rendered, so a post with options but
    // no score fields at all is a crafted post; it must error rather than silently build unscored
    // options, which for an existing question would wipe its stored scores.
    MultiOptionQuestionForm form = new CheckboxQuestionForm();
    form.setQuestionName("name");
    form.setQuestionDescription("description");
    form.setQuestionText("What is the question text?");
    form.setQuestionHelpText("help text");
    form.setMinChoicesRequired("");
    form.setMaxChoicesAllowed("");
    form.setOptions(ImmutableList.of("one", "two"));
    form.setOptionAdminNames(ImmutableList.of("one admin", "two admin"));
    form.setOptionIds(ImmutableList.of(1L, 2L));

    assertThat(form.getOptionScoreErrors())
        .extracting(CiviFormError::message)
        .containsExactly("The number of option scores must match the number of options");
  }

  @Test
  public void getOptionScoreErrors_noOptionsAndNoScores_empty() {
    // Zero options with zero scores is parallel, not a mismatch.
    MultiOptionQuestionForm form = new CheckboxQuestionForm();
    form.setNewOptions(ImmutableList.of("one"));
    form.setNewOptionAdminNames(ImmutableList.of("one admin"));
    form.setNewOptionScores(ImmutableList.of("2.5"));

    assertThat(form.getOptionScoreErrors()).isEmpty();
  }

  @Test
  public void getBuilder_yesNoForm_neverAppliesScores() throws Exception {
    MultiOptionQuestionForm form = new YesNoQuestionForm();
    form.setQuestionName("name");
    form.setQuestionDescription("description");
    form.setQuestionText("What is the question text?");
    form.setQuestionHelpText("help text");
    form.setOptions(ImmutableList.of("Yes", "No"));
    form.setOptionAdminNames(ImmutableList.of("yes", "no"));
    form.setOptionIds(ImmutableList.of(1L, 0L));
    form.setDisplayedOptionIds(ImmutableList.of(1L, 0L));
    // Crafted scores on a Yes/No question must be discarded even with the flag on.
    form.setOptionScores(ImmutableList.of("4", "5"));

    MultiOptionQuestionDefinition questionDefinition =
        (MultiOptionQuestionDefinition) form.getBuilder(/* scoringEnabled= */ true).build();

    assertThat(questionDefinition.getOptions().stream().map(QuestionOption::score))
        .containsExactly(Optional.empty(), Optional.empty());
  }

  @Test
  public void getOptionScoreErrors_validScores_empty() {
    MultiOptionQuestionForm form = new CheckboxQuestionForm();
    form.setOptions(ImmutableList.of("one", "two", "three"));
    form.setOptionAdminNames(ImmutableList.of("one admin", "two admin", "three admin"));
    form.setOptionIds(ImmutableList.of(1L, 2L, 3L));
    // Fractional, exponent-notation, and beyond-32-bit values are all valid decimals; blank
    // stays "unscored".
    form.setOptionScores(ImmutableList.of("1.5", "-3", "2147483648"));
    form.setNewOptions(ImmutableList.of("four"));
    form.setNewOptionAdminNames(ImmutableList.of("four admin"));
    form.setNewOptionScores(ImmutableList.of("-0.125"));

    assertThat(form.getOptionScoreErrors()).isEmpty();
  }

  @Test
  public void getOptionScoreErrors_invalidScores_returnsErrorsNotExceptions() {
    MultiOptionQuestionForm form = new CheckboxQuestionForm();
    form.setOptions(ImmutableList.of("one", "two", "three", "four"));
    form.setOptionAdminNames(
        ImmutableList.of("one admin", "two admin", "three admin", "four admin"));
    form.setOptionIds(ImmutableList.of(1L, 2L, 3L, 4L));
    // Junk, NaN, Infinity, and a double-overflowing exponent are all rejected.
    form.setOptionScores(ImmutableList.of("junk", "NaN", "Infinity", "1e999"));

    assertThat(form.getOptionScoreErrors())
        .hasSize(4)
        .allSatisfy(error -> assertThat(error.message()).contains("must be a number"));
  }

  @Test
  public void getOptionScoreErrors_cardinalityMismatch_returnsError() {
    MultiOptionQuestionForm form = new CheckboxQuestionForm();
    form.setOptions(ImmutableList.of("one", "two"));
    form.setOptionAdminNames(ImmutableList.of("one admin", "two admin"));
    form.setOptionIds(ImmutableList.of(1L, 2L));
    form.setOptionScores(ImmutableList.of("4"));
    form.setNewOptions(ImmutableList.of("three"));
    form.setNewOptionAdminNames(ImmutableList.of("three admin"));
    form.setNewOptionScores(ImmutableList.of("1", "2"));

    assertThat(form.getOptionScoreErrors())
        .extracting(CiviFormError::message)
        .containsExactlyInAnyOrder(
            "The number of option scores must match the number of options",
            "The number of new option scores must match the number of new options");
  }
}
