package services.question.types;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalInt;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;
import services.CiviFormError;
import services.LocalizedStrings;
import services.TranslationNotFoundException;
import services.question.LocalizedQuestionOption;
import services.question.QuestionOption;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.MultiOptionQuestionDefinition.MultiOptionQuestionType;
import services.question.types.MultiOptionQuestionDefinition.MultiOptionValidationPredicates;

@RunWith(JUnitParamsRunner.class)
public class MultiOptionQuestionDefinitionTest {

  @Test
  public void buildMultiSelectQuestion() throws UnsupportedQuestionTypeException {
    ImmutableList<QuestionOption> options =
        ImmutableList.of(
            QuestionOption.create(1L, "opt1", LocalizedStrings.of(Locale.US, "option 1")),
            QuestionOption.create(2L, "opt1", LocalizedStrings.of(Locale.US, "option 2")));

    QuestionDefinition definition =
        new QuestionDefinitionBuilder()
            .setQuestionType(QuestionType.DROPDOWN)
            .setName("")
            .setDescription("")
            .setQuestionText(LocalizedStrings.of())
            .setQuestionHelpText(LocalizedStrings.empty())
            .setQuestionOptions(options)
            .build();

    MultiOptionQuestionDefinition multiOption = (MultiOptionQuestionDefinition) definition;

    assertThat(multiOption.getOptions()).isEqualTo(options);
  }

  @Test
  public void getSupportedLocales_onlyIncludesLocalesSupportedByQuestionTextAndOptions()
      throws UnsupportedQuestionTypeException {
    QuestionDefinition definition =
        new QuestionDefinitionBuilder()
            .setQuestionType(QuestionType.DROPDOWN)
            .setName("")
            .setDescription("")
            .setQuestionText(LocalizedStrings.of(Locale.US, "test", Locale.FRANCE, "test"))
            .setQuestionHelpText(LocalizedStrings.of(Locale.US, "test", Locale.FRANCE, "test"))
            .setQuestionOptions(
                ImmutableList.of(
                    QuestionOption.create(1L, "opt1", LocalizedStrings.of(Locale.US, "option 1"))))
            .build();

    assertThat(definition.getSupportedLocales()).containsExactly(Locale.US);
  }

  @Test
  public void getSupportedLocales_selectsSmallestSetOfLocalesFromOptions()
      throws UnsupportedQuestionTypeException {
    QuestionDefinition definition =
        new QuestionDefinitionBuilder()
            .setQuestionType(QuestionType.DROPDOWN)
            .setName("")
            .setDescription("")
            .setQuestionText(
                LocalizedStrings.of(Locale.US, "test", Locale.FRANCE, "test", Locale.UK, "test"))
            .setQuestionHelpText(
                LocalizedStrings.of(Locale.US, "test", Locale.FRANCE, "test", Locale.UK, "test"))
            .setQuestionOptions(
                ImmutableList.of(
                    QuestionOption.create(
                        1L,
                        "opt1",
                        LocalizedStrings.of(Locale.US, "1", Locale.FRANCE, "1", Locale.UK, "1")),
                    QuestionOption.create(
                        1L, "opt2", LocalizedStrings.of(Locale.US, "2", Locale.FRANCE, "2"))))
            .build();

    assertThat(definition.getSupportedLocales()).containsExactly(Locale.US, Locale.FRANCE);
  }

  @Test
  public void getOptionsForLocale_failsForMissingLocale() throws UnsupportedQuestionTypeException {
    QuestionDefinition definition =
        new QuestionDefinitionBuilder()
            .setQuestionType(QuestionType.DROPDOWN)
            .setName("")
            .setDescription("")
            .setQuestionText(LocalizedStrings.of())
            .setQuestionHelpText(LocalizedStrings.empty())
            .setQuestionOptions(
                ImmutableList.of(
                    QuestionOption.create(1L, "ay", LocalizedStrings.of(Locale.US, "option 1"))))
            .build();

    MultiOptionQuestionDefinition multiOption = (MultiOptionQuestionDefinition) definition;
    Throwable thrown = catchThrowable(() -> multiOption.getOptionsForLocale(Locale.CANADA_FRENCH));

    assertThat(thrown).isInstanceOf(TranslationNotFoundException.class);
    assertThat(thrown).hasMessageContaining("ca");
  }

  @Test
  public void getOptionsForLocale_returnsAllTranslations() throws Exception {
    ImmutableList<QuestionOption> options =
        ImmutableList.of(
            QuestionOption.create(
                1L, "ay", LocalizedStrings.of(Locale.US, "one", Locale.GERMAN, "eins")),
            QuestionOption.create(
                2L, "bee", LocalizedStrings.of(Locale.US, "two", Locale.GERMAN, "zwei")));
    QuestionDefinition definition =
        new QuestionDefinitionBuilder()
            .setQuestionType(QuestionType.DROPDOWN)
            .setName("")
            .setDescription("")
            .setQuestionText(LocalizedStrings.of())
            .setQuestionHelpText(LocalizedStrings.empty())
            .setQuestionOptions(options)
            .build();

    MultiOptionQuestionDefinition multiOption = (MultiOptionQuestionDefinition) definition;

    assertThat(multiOption.getOptionsForLocale(Locale.US))
        .containsExactly(
            LocalizedQuestionOption.create(1L, 1L, "ay", "one", Locale.US),
            LocalizedQuestionOption.create(2L, 2L, "bee", "two", Locale.US));
  }

  @Test
  public void getOptionsForLocaleOrDefault_returnsBothLocalizedAndDefault() throws Exception {
    ImmutableList<QuestionOption> options =
        ImmutableList.of(
            QuestionOption.create(
                1L, "ay", LocalizedStrings.of(Locale.US, "one", Locale.GERMAN, "eins")),
            QuestionOption.create(
                2L, "bee", LocalizedStrings.of(Locale.US, "two", Locale.GERMAN, "zwei")),
            QuestionOption.create(3L, "see", LocalizedStrings.of(Locale.US, "three")));
    QuestionDefinition definition =
        new QuestionDefinitionBuilder()
            .setQuestionType(QuestionType.DROPDOWN)
            .setName("")
            .setDescription("")
            .setQuestionText(LocalizedStrings.of())
            .setQuestionHelpText(LocalizedStrings.empty())
            .setQuestionOptions(options)
            .build();

    MultiOptionQuestionDefinition multiOption = (MultiOptionQuestionDefinition) definition;

    assertThat(multiOption.getOptionsForLocaleOrDefault(Locale.GERMAN))
        .containsExactly(
            LocalizedQuestionOption.create(1L, 1L, "ay", "eins", Locale.GERMAN),
            LocalizedQuestionOption.create(2L, 2L, "bee", "zwei", Locale.GERMAN),
            LocalizedQuestionOption.create(3L, 3L, "see", "three", Locale.US));
  }

  @Test
  public void getOptionAdminNames_returnsAdminNames() throws UnsupportedQuestionTypeException {
    ImmutableList<QuestionOption> options =
        ImmutableList.of(
            QuestionOption.create(1L, "opt1", LocalizedStrings.of(Locale.US, "option 1")),
            QuestionOption.create(2L, "opt2", LocalizedStrings.of(Locale.US, "option 2")));

    QuestionDefinition definition =
        new QuestionDefinitionBuilder()
            .setQuestionType(QuestionType.DROPDOWN)
            .setName("")
            .setDescription("")
            .setQuestionText(LocalizedStrings.of())
            .setQuestionHelpText(LocalizedStrings.empty())
            .setQuestionOptions(options)
            .build();

    MultiOptionQuestionDefinition multiOption = (MultiOptionQuestionDefinition) definition;

    assertThat(multiOption.getOptionAdminNames()).containsExactly("opt1", "opt2");
  }

  @Test
  public void validate_withoutOptions_returnsError() {
    QuestionDefinitionConfig config =
        QuestionDefinitionConfig.builder()
            .setName("test")
            .setDescription("test")
            .setQuestionText(LocalizedStrings.withDefaultValue("test"))
            .setQuestionHelpText(LocalizedStrings.empty())
            .build();
    QuestionDefinition question =
        new MultiOptionQuestionDefinition(
            config, /* questionOptions */ ImmutableList.of(), MultiOptionQuestionType.CHECKBOX);
    assertThat(question.validate())
        .containsOnly(CiviFormError.of("Multi-option questions must have at least one option"));
  }

  @Test
  public void validate_withBlankOption_returnsError() {
    QuestionDefinitionConfig config = makeConfigBuilder().build();
    QuestionDefinition question =
        new MultiOptionQuestionDefinition(
            config,
            ImmutableList.of(
                QuestionOption.create(1L, "opt1", LocalizedStrings.withDefaultValue(""))),
            MultiOptionQuestionType.CHECKBOX);
    assertThat(question.validate())
        .containsOnly(CiviFormError.of("Multi-option questions cannot have blank options"));
  }

  @Test
  public void validate_withBlankOptionAdminNames_returnsError() {
    QuestionDefinitionConfig config = makeConfigBuilder().build();
    QuestionDefinition question =
        new MultiOptionQuestionDefinition(
            config,
            ImmutableList.of(QuestionOption.create(1L, "", LocalizedStrings.withDefaultValue("a"))),
            MultiOptionQuestionType.CHECKBOX);
    assertThat(question.validate())
        .containsExactlyInAnyOrder(
            CiviFormError.of("Multi-option questions cannot have blank admin names"),
            CiviFormError.of(
                "Multi-option admin names can only contain lowercase letters, numbers, underscores,"
                    + " and dashes"));
  }

  @Test
  public void validate_withDuplicateOptions_returnsError() {
    QuestionDefinitionConfig config = makeConfigBuilder().build();
    ImmutableList<QuestionOption> questionOptions =
        ImmutableList.of(
            QuestionOption.create(1L, "opt1", LocalizedStrings.withDefaultValue("a")),
            QuestionOption.create(2L, "opt2", LocalizedStrings.withDefaultValue("a")));
    QuestionDefinition question =
        new MultiOptionQuestionDefinition(
            config, questionOptions, MultiOptionQuestionType.CHECKBOX);
    assertThat(question.validate())
        .containsOnly(CiviFormError.of("Multi-option question options must be unique"));
  }

  @Test
  public void validate_withDuplicateOptionsWithDifferentCase_returnsError() {
    QuestionDefinitionConfig config = makeConfigBuilder().build();
    ImmutableList<QuestionOption> questionOptions =
        ImmutableList.of(
            QuestionOption.create(
                1L, "Parks_and_Recreation", LocalizedStrings.withDefaultValue("a1")),
            QuestionOption.create(
                2L, "Parks_and_recreation", LocalizedStrings.withDefaultValue("a2")));
    QuestionDefinition question =
        new MultiOptionQuestionDefinition(
            config, questionOptions, MultiOptionQuestionType.CHECKBOX);
    assertThat(question.validate())
        .contains(CiviFormError.of("Multi-option question admin names must be unique"));
  }

  @Test
  public void validate_withDuplicateOptionAdminNames_returnsError() {
    QuestionDefinitionConfig config = makeConfigBuilder().build();
    ImmutableList<QuestionOption> questionOptions =
        ImmutableList.of(
            QuestionOption.create(1L, "opt1", LocalizedStrings.withDefaultValue("a")),
            QuestionOption.create(2L, "opt1", LocalizedStrings.withDefaultValue("b")));
    QuestionDefinition question =
        new MultiOptionQuestionDefinition(
            config, questionOptions, MultiOptionQuestionType.CHECKBOX);
    assertThat(question.validate())
        .containsOnly(CiviFormError.of("Multi-option question admin names must be unique"));
  }

  @Test
  public void validate_withUniqueOptionAdminNames_doesNotReturnError() {
    QuestionDefinitionConfig config = makeConfigBuilder().build();
    ImmutableList<QuestionOption> questionOptions =
        ImmutableList.of(
            QuestionOption.create(1L, "a_one-1", LocalizedStrings.withDefaultValue("a")),
            QuestionOption.create(2L, "b_two-2", LocalizedStrings.withDefaultValue("b")));
    QuestionDefinition question =
        new MultiOptionQuestionDefinition(
            config, questionOptions, MultiOptionQuestionType.CHECKBOX);
    assertThat(question.validate()).isEmpty();
  }

  @Test
  public void validate_withInvalidOptionAdminNames_returnsError() {
    QuestionDefinitionConfig config =
        QuestionDefinitionConfig.builder()
            .setName("test")
            .setDescription("test")
            .setQuestionText(LocalizedStrings.withDefaultValue("test"))
            .setQuestionHelpText(LocalizedStrings.empty())
            .build();
    ImmutableList<QuestionOption> questionOptions =
        ImmutableList.of(
            QuestionOption.create(1L, "a' invalid", LocalizedStrings.withDefaultValue("a")),
            QuestionOption.create(2L, "b_valid", LocalizedStrings.withDefaultValue("b")));
    QuestionDefinition question =
        new MultiOptionQuestionDefinition(
            config, questionOptions, MultiOptionQuestionType.CHECKBOX);
    assertThat(question.validate())
        .containsOnly(
            CiviFormError.of(
                "Multi-option admin names can only contain lowercase letters, numbers, underscores,"
                    + " and dashes"));
  }

  @Test
  public void
      validate_validateQuestionOptionAdminNamesFalse_invalidOptionAdminNames_doesNotReturnError() {
    QuestionDefinitionConfig config =
        QuestionDefinitionConfig.builder()
            .setName("test")
            .setDescription("test")
            .setQuestionText(LocalizedStrings.withDefaultValue("test"))
            .setQuestionHelpText(LocalizedStrings.empty())
            .build();
    ImmutableList<QuestionOption> questionOptions =
        ImmutableList.of(
            QuestionOption.create(1L, "a' invalid", LocalizedStrings.withDefaultValue("a")),
            QuestionOption.create(2L, "b_valid", LocalizedStrings.withDefaultValue("b")));
    MultiOptionQuestionDefinition question =
        new MultiOptionQuestionDefinition(
            config, questionOptions, MultiOptionQuestionType.CHECKBOX);
    question.setValidateQuestionOptionAdminNames(false);
    assertThat(question.validate()).isEmpty();
  }

  @Test
  public void validate_withCapitalLetterInOptionAdminNames_returnsError() {
    QuestionDefinitionConfig config =
        QuestionDefinitionConfig.builder()
            .setName("test")
            .setDescription("test")
            .setQuestionText(LocalizedStrings.withDefaultValue("test"))
            .setQuestionHelpText(LocalizedStrings.empty())
            .build();
    ImmutableList<QuestionOption> questionOptions =
        ImmutableList.of(
            QuestionOption.create(1L, "A_invalid", LocalizedStrings.withDefaultValue("a")),
            QuestionOption.create(2L, "b_valid", LocalizedStrings.withDefaultValue("b")));
    QuestionDefinition question =
        new MultiOptionQuestionDefinition(
            config, questionOptions, MultiOptionQuestionType.CHECKBOX);
    assertThat(question.validate())
        .containsOnly(
            CiviFormError.of(
                "Multi-option admin names can only contain lowercase letters, numbers, underscores,"
                    + " and dashes"));
  }

  @Test
  public void validate_withInvalidOptionAdminNameInPreviousDefinition_doesNotReturnError() {
    QuestionDefinitionConfig config =
        QuestionDefinitionConfig.builder()
            .setName("test")
            .setDescription("test")
            .setQuestionText(LocalizedStrings.withDefaultValue("test"))
            .setQuestionHelpText(LocalizedStrings.empty())
            .build();
    ImmutableList<QuestionOption> previousQuestionOptions =
        ImmutableList.of(
            QuestionOption.create(1L, "a' invalid", LocalizedStrings.withDefaultValue("a")),
            QuestionOption.create(2L, "b_valid", LocalizedStrings.withDefaultValue("b")));
    QuestionDefinition previousQuestion =
        new MultiOptionQuestionDefinition(
            config, previousQuestionOptions, MultiOptionQuestionType.CHECKBOX);

    ImmutableList<QuestionOption> updatedQuestionOptions =
        ImmutableList.<QuestionOption>builder()
            .addAll(previousQuestionOptions)
            .add(QuestionOption.create(2L, "c_valid", LocalizedStrings.withDefaultValue("c")))
            .build();
    QuestionDefinition updatedQuestion =
        new MultiOptionQuestionDefinition(
            config, updatedQuestionOptions, MultiOptionQuestionType.CHECKBOX);

    assertThat(updatedQuestion.validate(Optional.of(previousQuestion))).isEmpty();
  }

  @Test
  public void validate_withValidOptionAdminNameInPreviousAndDuplicateNameInUpdate_returnsError() {
    QuestionDefinitionConfig config =
        QuestionDefinitionConfig.builder()
            .setName("test")
            .setDescription("test")
            .setQuestionText(LocalizedStrings.withDefaultValue("test"))
            .setQuestionHelpText(LocalizedStrings.empty())
            .build();
    ImmutableList<QuestionOption> previousQuestionOptions =
        ImmutableList.of(
            QuestionOption.create(1L, "a_valid", LocalizedStrings.withDefaultValue("a")),
            QuestionOption.create(2L, "b_valid", LocalizedStrings.withDefaultValue("b")));
    QuestionDefinition previousQuestion =
        new MultiOptionQuestionDefinition(
            config, previousQuestionOptions, MultiOptionQuestionType.CHECKBOX);

    ImmutableList<QuestionOption> updatedQuestionOptions =
        ImmutableList.<QuestionOption>builder()
            .addAll(previousQuestionOptions)
            .add(QuestionOption.create(2L, "A_valid", LocalizedStrings.withDefaultValue("c")))
            .build();
    QuestionDefinition updatedQuestion =
        new MultiOptionQuestionDefinition(
            config, updatedQuestionOptions, MultiOptionQuestionType.CHECKBOX);

    assertThat(updatedQuestion.validate(Optional.of(previousQuestion)))
        .containsOnly(CiviFormError.of("Multi-option question admin names must be unique"));
  }

  @Test
  public void validate_withInvalidOptionAdminNameInPreviousAndUpdatedDefinition_returnsError() {
    QuestionDefinitionConfig config =
        QuestionDefinitionConfig.builder()
            .setName("test")
            .setDescription("test")
            .setQuestionText(LocalizedStrings.withDefaultValue("test"))
            .setQuestionHelpText(LocalizedStrings.empty())
            .build();
    ImmutableList<QuestionOption> previousQuestionOptions =
        ImmutableList.of(
            QuestionOption.create(1L, "a' invalid", LocalizedStrings.withDefaultValue("a")),
            QuestionOption.create(2L, "b_valid", LocalizedStrings.withDefaultValue("b")));
    QuestionDefinition previousQuestion =
        new MultiOptionQuestionDefinition(
            config, previousQuestionOptions, MultiOptionQuestionType.CHECKBOX);

    ImmutableList<QuestionOption> updatedQuestionOptions =
        ImmutableList.<QuestionOption>builder()
            .addAll(previousQuestionOptions)
            .add(QuestionOption.create(2L, "c invalid", LocalizedStrings.withDefaultValue("c")))
            .build();
    QuestionDefinition updatedQuestion =
        new MultiOptionQuestionDefinition(
            config, updatedQuestionOptions, MultiOptionQuestionType.CHECKBOX);

    assertThat(updatedQuestion.validate(Optional.of(previousQuestion)))
        .containsOnly(
            CiviFormError.of(
                "Multi-option admin names can only contain lowercase letters, numbers, underscores,"
                    + " and dashes"));
  }

  @SuppressWarnings("unused") // Is used via reflection by the @Parameters annotation below
  private static ImmutableList<Object[]> getValidationTestData() {
    return ImmutableList.of(
        // Valid cases.
        new Object[] {OptionalInt.empty(), OptionalInt.empty(), Optional.<String>empty()},
        new Object[] {OptionalInt.of(1), OptionalInt.empty(), Optional.<String>empty()},
        new Object[] {OptionalInt.empty(), OptionalInt.of(1), Optional.<String>empty()},
        new Object[] {OptionalInt.of(1), OptionalInt.of(2), Optional.<String>empty()},
        new Object[] {OptionalInt.of(1), OptionalInt.of(1), Optional.<String>empty()},

        // Edge cases.
        new Object[] {
          OptionalInt.of(-1),
          OptionalInt.empty(),
          Optional.of("Minimum number of choices required cannot be negative")
        },
        new Object[] {
          OptionalInt.empty(),
          OptionalInt.of(0),
          Optional.of("Maximum number of choices allowed cannot be less than 1")
        },
        new Object[] {
          OptionalInt.of(2),
          OptionalInt.of(1),
          Optional.of(
              "Minimum number of choices required must be less than or equal to the maximum"
                  + " choices allowed")
        },
        // Note: In the test code, we configure two options.
        new Object[] {
          OptionalInt.empty(),
          OptionalInt.of(3),
          Optional.of("Maximum number of choices allowed cannot exceed the number of options")
        },
        new Object[] {
          OptionalInt.of(3),
          OptionalInt.empty(),
          Optional.of("Minimum number of choices required cannot exceed the number of options")
        });
  }

  @Test
  @Parameters(method = "getValidationTestData")
  public void validate_settingConstraints(
      OptionalInt minChoicesRequired,
      OptionalInt maxChoicesAllowed,
      Optional<String> expectedErrorMessage) {
    QuestionDefinitionConfig config =
        makeConfigBuilder()
            .setValidationPredicates(
                MultiOptionValidationPredicates.builder()
                    .setMinChoicesRequired(minChoicesRequired)
                    .setMaxChoicesAllowed(maxChoicesAllowed)
                    .build())
            .build();
    ImmutableList<QuestionOption> questionOptions =
        ImmutableList.of(
            QuestionOption.create(1L, "opt1", LocalizedStrings.withDefaultValue("a")),
            QuestionOption.create(2L, "opt2", LocalizedStrings.withDefaultValue("b")));

    QuestionDefinition question =
        new MultiOptionQuestionDefinition(
            config, questionOptions, MultiOptionQuestionType.CHECKBOX);

    ImmutableSet<CiviFormError> errors = question.validate();

    assertThat(errors)
        .isEqualTo(
            expectedErrorMessage
                .map(CiviFormError::of)
                .map(ImmutableSet::of)
                .orElse(ImmutableSet.of()));
  }

  private QuestionDefinitionConfig.Builder makeConfigBuilder() {
    return QuestionDefinitionConfig.builder()
        .setName("name")
        .setDescription("description")
        .setQuestionText(LocalizedStrings.of(Locale.US, "question?"));
  }
}
