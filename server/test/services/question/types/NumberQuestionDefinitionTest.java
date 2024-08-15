package services.question.types;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;
import services.CiviFormError;
import services.LocalizedStrings;
import services.question.types.NumberQuestionDefinition.NumberValidationPredicates;

@RunWith(JUnitParamsRunner.class)
public class NumberQuestionDefinitionTest {
  @SuppressWarnings("unused") // Is used via reflection by the @Parameters annotation below
  private static ImmutableList<Object[]> getValidationTestData() {
    return ImmutableList.of(
        new Object[] {OptionalLong.empty(), OptionalLong.empty(), Optional.<String>empty()},
        new Object[] {OptionalLong.of(1), OptionalLong.empty(), Optional.<String>empty()},
        new Object[] {OptionalLong.empty(), OptionalLong.of(1), Optional.<String>empty()},
        new Object[] {OptionalLong.of(1), OptionalLong.of(2), Optional.<String>empty()},
        new Object[] {OptionalLong.of(1), OptionalLong.of(1), Optional.<String>empty()},
        new Object[] {
          OptionalLong.of(-1), OptionalLong.empty(), Optional.of("Minimum value cannot be negative")
        },
        new Object[] {
          OptionalLong.empty(), OptionalLong.of(-1), Optional.of("Maximum value cannot be negative")
        },
        new Object[] {
          OptionalLong.of(2),
          OptionalLong.of(1),
          Optional.of("Minimum value must be less than or equal to the maximum value")
        });
  }

  @Test
  @Parameters(method = "getValidationTestData")
  public void validate_settingConstraints(
      OptionalLong min, OptionalLong max, Optional<String> expectedErrorMessage) {
    QuestionDefinitionConfig config =
        makeConfigBuilder()
            .setValidationPredicates(
                NumberValidationPredicates.builder().setMin(min).setMax(max).build())
            .build();
    QuestionDefinition question = new NumberQuestionDefinition(config);

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
