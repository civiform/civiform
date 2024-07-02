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
import services.question.types.TextQuestionDefinition.TextValidationPredicates;

@RunWith(JUnitParamsRunner.class)
public class TextQuestionDefinitionTest {
  @SuppressWarnings("unused") // Is used via reflection by the @Parameters annotation below
  private static ImmutableList<Object[]> getValidationTestData() {
    return ImmutableList.of(
        new Object[] {OptionalInt.empty(), OptionalInt.empty(), Optional.<String>empty()},
        new Object[] {OptionalInt.of(1), OptionalInt.empty(), Optional.<String>empty()},
        new Object[] {OptionalInt.empty(), OptionalInt.of(1), Optional.<String>empty()},
        new Object[] {OptionalInt.of(1), OptionalInt.of(2), Optional.<String>empty()},
        new Object[] {OptionalInt.of(1), OptionalInt.of(1), Optional.<String>empty()},
        new Object[] {
          OptionalInt.of(-1), OptionalInt.empty(), Optional.of("Minimum length cannot be negative")
        },
        new Object[] {
          OptionalInt.empty(),
          OptionalInt.of(0),
          Optional.of("Maximum length cannot be less than 1")
        },
        new Object[] {
          OptionalInt.of(2),
          OptionalInt.of(1),
          Optional.of("Minimum length must be less than or equal to the maximum length")
        });
  }

  @Test
  @Parameters(method = "getValidationTestData")
  public void validate_settingConstraints(
      OptionalInt minLength, OptionalInt maxLength, Optional<String> expectedErrorMessage) {
    QuestionDefinitionConfig config =
        makeConfigBuilder()
            .setValidationPredicates(
                TextValidationPredicates.builder()
                    .setMinLength(minLength)
                    .setMaxLength(maxLength)
                    .build())
            .build();
    QuestionDefinition question = new TextQuestionDefinition(config);

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
