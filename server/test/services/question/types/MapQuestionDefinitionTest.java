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
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.MapQuestionDefinition.MapValidationPredicates;

@RunWith(JUnitParamsRunner.class)
public class MapQuestionDefinitionTest {

  @Test
  public void buildMapQuestion() throws UnsupportedQuestionTypeException {
    String name = "Test Name";
    String description = "Test Description";
    QuestionDefinition definition =
        new QuestionDefinitionBuilder()
            .setQuestionType(QuestionType.MAP)
            .setName(name)
            .setDescription(description)
            .setQuestionText(LocalizedStrings.of())
            .setValidationPredicates(
                MapValidationPredicates.builder().setGeoJsonEndpoint("test endpoint").build())
            .setQuestionHelpText(LocalizedStrings.empty())
            .build();

    MapQuestionDefinition mapQuestionDefinition = (MapQuestionDefinition) definition;

    assertThat(mapQuestionDefinition.getName()).isEqualTo(name);
    assertThat(mapQuestionDefinition.getDescription()).isEqualTo(description);
  }

  @Test
  public void validate_withoutGeoJsonEndpoint_returnsError() {
    QuestionDefinitionConfig config =
        QuestionDefinitionConfig.builder()
            .setName("test")
            .setDescription("test")
            .setQuestionText(LocalizedStrings.withDefaultValue("test"))
            .setQuestionHelpText(LocalizedStrings.empty())
            .build();
    QuestionDefinition question = new MapQuestionDefinition(config);
    assertThat(question.validate())
        .containsOnly(CiviFormError.of("Map question must have a GeoJSON endpoint"));
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
        });
  }

  @Test
  @Parameters(method = "getValidationTestData")
  public void validate_settingConstraints(
      OptionalInt maxLocationSelections,
      Optional<String> expectedErrorMessage) {
    QuestionDefinitionConfig config =
        makeConfigBuilder()
            .setValidationPredicates(
                MapValidationPredicates.builder()
                    .setGeoJsonEndpoint("test endpoint")
                    .setMaxLocationSelections(maxLocationSelections)
                    .build())
            .build();

    QuestionDefinition question = new MapQuestionDefinition(config);

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
