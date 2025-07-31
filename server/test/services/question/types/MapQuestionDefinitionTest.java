package services.question.types;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import models.GeoJsonDataModel;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import play.test.WithApplication;
import services.CiviFormError;
import services.LocalizedStrings;
import services.geojson.Feature;
import services.geojson.FeatureCollection;
import services.geojson.Geometry;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.MapQuestionDefinition.MapValidationPredicates;

@RunWith(JUnitParamsRunner.class)
public class MapQuestionDefinitionTest extends WithApplication {
  private final FeatureCollection testFeatureCollection =
      new FeatureCollection(
          "FeatureCollection",
          List.of(
              new Feature(
                  "Feature",
                  new Geometry("Point", List.of(1.0, 1.0)),
                  Map.of("name", "Feature 1.1"),
                  "1"),
              new Feature(
                  "Feature",
                  new Geometry("Point", List.of(1.0, 2.0)),
                  Map.of("name", "Feature 1.2"),
                  "2"),
              new Feature(
                  "Feature",
                  new Geometry("Point", List.of(2.0, 3.0)),
                  Map.of("name", "Feature 1.3"),
                  "3")));
  private final String endpoint = "http://example.com/geo.json";

  @Before
  public void setup() {
    GeoJsonDataModel geoJsonData = new GeoJsonDataModel();
    geoJsonData.setGeoJson(testFeatureCollection);
    geoJsonData.setEndpoint(endpoint);
    geoJsonData.setConfirmTime(Instant.now());
    geoJsonData.save();
  }

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
                MapValidationPredicates.builder().setGeoJsonEndpoint(endpoint).build())
            .setQuestionHelpText(LocalizedStrings.empty())
            .build();

    MapQuestionDefinition mapQuestionDefinition = (MapQuestionDefinition) definition;

    assertThat(mapQuestionDefinition.getName()).isEqualTo(name);
    assertThat(mapQuestionDefinition.getDescription()).isEqualTo(description);
  }

  @Test
  public void validate_withoutGeoJson_returnsError() {
    QuestionDefinitionConfig config =
        QuestionDefinitionConfig.builder()
            .setName("test")
            .setDescription("test")
            .setQuestionText(LocalizedStrings.withDefaultValue("test"))
            .setQuestionHelpText(LocalizedStrings.empty())
            .build();
    QuestionDefinition question = new MapQuestionDefinition(config);
    assertThat(question.validate())
        .containsOnly(CiviFormError.of("Map question must have a valid GeoJSON"));
  }

  @SuppressWarnings("unused") // Is used via reflection by the @Parameters annotation below
  private static ImmutableList<Object[]> getMaxLocationsTestData() {
    return ImmutableList.of(
        // Valid cases.
        new Object[] {OptionalInt.empty(), Optional.<String>empty()},
        new Object[] {OptionalInt.of(1), Optional.<String>empty()},
        new Object[] {OptionalInt.of(2), Optional.<String>empty()},

        // Edge cases.
        new Object[] {
          OptionalInt.of(0), Optional.of("Max location selections cannot be less than 1")
        });
  }

  @Test
  @Parameters(method = "getMaxLocationsTestData")
  public void validate_settingMaxLocationsConstraint(
      OptionalInt maxLocationSelections, Optional<String> expectedErrorMessage) {
    QuestionDefinitionConfig config =
        makeConfigBuilder()
            .setValidationPredicates(
                MapValidationPredicates.builder()
                    .setGeoJsonEndpoint(endpoint)
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
