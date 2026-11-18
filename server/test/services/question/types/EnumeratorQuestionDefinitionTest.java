package services.question.types;

import static org.assertj.core.api.Assertions.assertThat;

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
import services.question.types.EnumeratorQuestionDefinition.EnumeratorValidationPredicates;

@RunWith(JUnitParamsRunner.class)
public class EnumeratorQuestionDefinitionTest {
  @Test
  public void validate_withEmptyEntityString_returnsError() throws Exception {
    QuestionDefinition question =
        new EnumeratorQuestionDefinition(
            makeConfigBuilder().build(), LocalizedStrings.withDefaultValue(""));

    assertThat(question.validate())
        .containsOnly(CiviFormError.of("Enumerator question must have specified entity type"));
  }

  @Test
  public void validate_withEmptyEntityStringAndEnumeratorImprovementsEnabled_returnsListSetError() {
    QuestionDefinition question =
        new EnumeratorQuestionDefinition(
            makeConfigBuilder().build(), LocalizedStrings.withDefaultValue(""));

    assertThat(
            question.validate(
                /* previousDefinition= */ Optional.empty(),
                /* enumeratorImprovementsEnabled= */ true))
        .containsOnly(CiviFormError.of("Enter an answer for \"What is being listed?\""));
  }

  @Test
  public void
      validate_withBlankQuestionTextAndAdminIdAndEnumeratorImprovementsDisabled_returnsDefaultErrors() {
    QuestionDefinition question =
        new EnumeratorQuestionDefinition(
            QuestionDefinitionConfig.builder()
                .setName("")
                .setDescription("description")
                .setQuestionText(LocalizedStrings.of())
                .build(),
            LocalizedStrings.withDefaultValue("foo"));

    assertThat(
            question.validate(
                /* previousDefinition= */ Optional.empty(),
                /* enumeratorImprovementsEnabled= */ false))
        .containsOnly(
            CiviFormError.of("Administrative identifier cannot be blank"),
            CiviFormError.of("Question text cannot be blank"));
  }

  @Test
  public void
      validate_withBlankQuestionTextAndAdminIdAndEnumeratorImprovementsEnabled_returnsListSetErrors() {
    QuestionDefinition question =
        new EnumeratorQuestionDefinition(
            QuestionDefinitionConfig.builder()
                .setName("")
                .setDescription("description")
                .setQuestionText(LocalizedStrings.of())
                .build(),
            LocalizedStrings.withDefaultValue("foo"));

    assertThat(
            question.validate(
                /* previousDefinition= */ Optional.empty(),
                /* enumeratorImprovementsEnabled= */ true))
        .containsOnly(
            CiviFormError.of("Enter list set admin ID"), CiviFormError.of("Enter prompt text"));
  }

  @SuppressWarnings("unused") // Is used via reflection by the @Parameters annotation below
  private static ImmutableList<Object[]> getValidationTestData() {
    return ImmutableList.of(
        new Object[] {OptionalInt.empty(), OptionalInt.empty(), Optional.<String>empty()},
        new Object[] {OptionalInt.of(1), OptionalInt.empty(), Optional.<String>empty()},
        new Object[] {OptionalInt.empty(), OptionalInt.of(1), Optional.<String>empty()},
        new Object[] {OptionalInt.of(1), OptionalInt.of(2), Optional.<String>empty()},
        new Object[] {OptionalInt.of(1), OptionalInt.of(1), Optional.<String>empty()},
        new Object[] {
          OptionalInt.of(-1),
          OptionalInt.empty(),
          Optional.of("Minimum entity count cannot be negative")
        },
        new Object[] {
          OptionalInt.empty(),
          OptionalInt.of(0),
          Optional.of("Maximum entity count cannot be less than 1")
        },
        new Object[] {
          OptionalInt.of(2),
          OptionalInt.of(1),
          Optional.of("Minimum entity count must be less than or equal to the maximum entity count")
        });
  }

  @Test
  @Parameters(method = "getValidationTestData")
  public void validate_settingConstraints(
      OptionalInt minEntities, OptionalInt maxEntities, Optional<String> expectedErrorMessage) {
    QuestionDefinitionConfig config =
        makeConfigBuilder()
            .setValidationPredicates(
                EnumeratorValidationPredicates.builder()
                    .setMinEntities(minEntities)
                    .setMaxEntities(maxEntities)
                    .build())
            .build();
    QuestionDefinition question =
        new EnumeratorQuestionDefinition(config, LocalizedStrings.withDefaultValue("foo"));

    ImmutableSet<CiviFormError> errors =
        question.validate(
            /* previousDefinition= */ Optional.empty(), /* enumeratorImprovementsEnabled= */ false);

    assertThat(errors)
        .isEqualTo(
            expectedErrorMessage
                .map(CiviFormError::of)
                .map(ImmutableSet::of)
                .orElse(ImmutableSet.of()));
  }

  @SuppressWarnings("unused") // Is used via reflection by the @Parameters annotation below
  private static ImmutableList<Object[]> getValidationWithEnumeratorImprovementsTestData() {
    return ImmutableList.of(
        new Object[] {
          OptionalInt.of(-1), OptionalInt.empty(), "Enter a minimum list count of 0 or more"
        },
        new Object[] {
          OptionalInt.empty(), OptionalInt.of(0), "Enter a maximum list count of 1 or more"
        },
        new Object[] {
          OptionalInt.of(2),
          OptionalInt.of(1),
          "Enter a minimum list count that is less than or equal to the maximum list count"
        });
  }

  @Test
  @Parameters(method = "getValidationWithEnumeratorImprovementsTestData")
  public void validate_withEnumeratorImprovementsEnabled_usesListSetWording(
      OptionalInt minEntities, OptionalInt maxEntities, String expectedErrorMessage) {
    QuestionDefinitionConfig config =
        makeConfigBuilder()
            .setValidationPredicates(
                EnumeratorValidationPredicates.builder()
                    .setMinEntities(minEntities)
                    .setMaxEntities(maxEntities)
                    .build())
            .build();
    QuestionDefinition question =
        new EnumeratorQuestionDefinition(config, LocalizedStrings.withDefaultValue("foo"));

    ImmutableSet<CiviFormError> errors =
        question.validate(
            /* previousDefinition= */ Optional.empty(), /* enumeratorImprovementsEnabled= */ true);

    assertThat(errors).containsOnly(CiviFormError.of(expectedErrorMessage));
  }

  private QuestionDefinitionConfig.Builder makeConfigBuilder() {
    return QuestionDefinitionConfig.builder()
        .setName("name")
        .setDescription("description")
        .setQuestionText(LocalizedStrings.of(Locale.US, "question?"));
  }
}
