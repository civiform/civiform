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
import services.question.MapSettingType;
import services.question.QuestionSetting;
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
                  Map.of("name", "Feature 1.1", "prop1", "value1", "prop2", "value2"),
                  "1"),
              new Feature(
                  "Feature",
                  new Geometry("Point", List.of(1.0, 2.0)),
                  Map.of("name", "Feature 1.2", "prop1", "value1", "prop2", "value2"),
                  "2"),
              new Feature(
                  "Feature",
                  new Geometry("Point", List.of(2.0, 3.0)),
                  Map.of("name", "Feature 1.3", "prop1", "value1", "prop2", "value2"),
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
  public void validate_withoutGeoJson_withoutMaxLocationSelections_returnsErrors() {
    QuestionDefinitionConfig config =
        QuestionDefinitionConfig.builder()
            .setName("test")
            .setDescription("test")
            .setQuestionText(LocalizedStrings.withDefaultValue("test"))
            .setQuestionHelpText(LocalizedStrings.empty())
            .build();
    QuestionDefinition question = new MapQuestionDefinition(config);
    assertThat(question.validate())
        .contains(CiviFormError.of("Map question must have valid GeoJSON"));
  }

  @SuppressWarnings("unused") // Is used via reflection by the @Parameters annotation below
  private static ImmutableList<Object[]> getMaxLocationsTestData() {
    return ImmutableList.of(
        // Valid cases.
        new Object[] {OptionalInt.of(1), Optional.<String>empty()},
        new Object[] {OptionalInt.of(2), Optional.<String>empty()},
        new Object[] {OptionalInt.empty(), Optional.<String>empty()},

        // Error cases.
        new Object[] {
          OptionalInt.of(0), Optional.of("Maximum location selections cannot be less than 1")
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
            .setQuestionSettings(
                ImmutableSet.of(
                    QuestionSetting.create("name", MapSettingType.LOCATION_NAME_GEO_JSON_KEY),
                    QuestionSetting.create("address", MapSettingType.LOCATION_ADDRESS_GEO_JSON_KEY),
                    QuestionSetting.create(
                        "details_url", MapSettingType.LOCATION_DETAILS_URL_GEO_JSON_KEY)))
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

  @SuppressWarnings("unused")
  private static ImmutableList<Object[]> getSettingsTestData() {
    return ImmutableList.of(
        // Valid cases
        new Object[] {
          ImmutableSet.of(
              QuestionSetting.create("name", MapSettingType.LOCATION_NAME_GEO_JSON_KEY),
              QuestionSetting.create("address", MapSettingType.LOCATION_ADDRESS_GEO_JSON_KEY),
              QuestionSetting.create(
                  "details_url", MapSettingType.LOCATION_DETAILS_URL_GEO_JSON_KEY)),
          ImmutableSet.of()
        },
        new Object[] {
          ImmutableSet.of(
              QuestionSetting.create("name", MapSettingType.LOCATION_NAME_GEO_JSON_KEY),
              QuestionSetting.create("address", MapSettingType.LOCATION_ADDRESS_GEO_JSON_KEY),
              QuestionSetting.create(
                  "details_url", MapSettingType.LOCATION_DETAILS_URL_GEO_JSON_KEY),
              QuestionSetting.create(
                  "filter1",
                  MapSettingType.LOCATION_FILTER_GEO_JSON_KEY,
                  Optional.of(LocalizedStrings.of(Locale.US, "Filter 1"))),
              QuestionSetting.create(
                  "filter2",
                  MapSettingType.LOCATION_FILTER_GEO_JSON_KEY,
                  Optional.of(LocalizedStrings.of(Locale.US, "Filter 2"))),
              QuestionSetting.create(
                  "filter3",
                  MapSettingType.LOCATION_FILTER_GEO_JSON_KEY,
                  Optional.of(LocalizedStrings.of(Locale.US, "Filter 3"))),
              QuestionSetting.create(
                  "filter4",
                  MapSettingType.LOCATION_FILTER_GEO_JSON_KEY,
                  Optional.of(LocalizedStrings.of(Locale.US, "Filter 4"))),
              QuestionSetting.create(
                  "filter5",
                  MapSettingType.LOCATION_FILTER_GEO_JSON_KEY,
                  Optional.of(LocalizedStrings.of(Locale.US, "Filter 5"))),
              QuestionSetting.create(
                  "filter6",
                  MapSettingType.LOCATION_FILTER_GEO_JSON_KEY,
                  Optional.of(LocalizedStrings.of(Locale.US, "Filter 6")))),
          ImmutableSet.of()
        },

        // Error cases
        new Object[] {
          ImmutableSet.<QuestionSetting>of(),
          ImmutableSet.of(
              CiviFormError.of("Name key cannot be empty"),
              CiviFormError.of("Address key cannot be empty"),
              CiviFormError.of("View more details URL key cannot be empty"))
        },
        new Object[] {
          ImmutableSet.of(
              QuestionSetting.create("address", MapSettingType.LOCATION_ADDRESS_GEO_JSON_KEY),
              QuestionSetting.create(
                  "details_url", MapSettingType.LOCATION_DETAILS_URL_GEO_JSON_KEY)),
          ImmutableSet.of(CiviFormError.of("Name key cannot be empty"))
        },
        new Object[] {
          ImmutableSet.of(
              QuestionSetting.create("name", MapSettingType.LOCATION_NAME_GEO_JSON_KEY),
              QuestionSetting.create("", MapSettingType.LOCATION_ADDRESS_GEO_JSON_KEY),
              QuestionSetting.create(
                  "details_url", MapSettingType.LOCATION_DETAILS_URL_GEO_JSON_KEY)),
          ImmutableSet.of(CiviFormError.of("Address key cannot be empty"))
        },
        new Object[] {
          ImmutableSet.of(
              QuestionSetting.create("name", MapSettingType.LOCATION_NAME_GEO_JSON_KEY),
              QuestionSetting.create("address", MapSettingType.LOCATION_ADDRESS_GEO_JSON_KEY),
              QuestionSetting.create(
                  "details_url", MapSettingType.LOCATION_DETAILS_URL_GEO_JSON_KEY),
              QuestionSetting.create(
                  "",
                  MapSettingType.LOCATION_FILTER_GEO_JSON_KEY,
                  Optional.of(LocalizedStrings.of(Locale.US, "Filter")))),
          ImmutableSet.of(CiviFormError.of("Filter key cannot be empty"))
        },
        new Object[] {
          ImmutableSet.of(
              QuestionSetting.create("name", MapSettingType.LOCATION_NAME_GEO_JSON_KEY),
              QuestionSetting.create("address", MapSettingType.LOCATION_ADDRESS_GEO_JSON_KEY),
              QuestionSetting.create(
                  "details_url", MapSettingType.LOCATION_DETAILS_URL_GEO_JSON_KEY),
              QuestionSetting.create(
                  "filter1",
                  MapSettingType.LOCATION_FILTER_GEO_JSON_KEY,
                  Optional.of(LocalizedStrings.of(Locale.US, "")))),
          ImmutableSet.of(CiviFormError.of("Filter display name cannot be empty"))
        },
        new Object[] {
          ImmutableSet.of(
              QuestionSetting.create("name", MapSettingType.LOCATION_NAME_GEO_JSON_KEY),
              QuestionSetting.create("address", MapSettingType.LOCATION_ADDRESS_GEO_JSON_KEY),
              QuestionSetting.create(
                  "details_url", MapSettingType.LOCATION_DETAILS_URL_GEO_JSON_KEY),
              QuestionSetting.create(
                  "filter1",
                  MapSettingType.LOCATION_FILTER_GEO_JSON_KEY,
                  Optional.of(LocalizedStrings.of(Locale.US, "Test Filter 1"))),
              QuestionSetting.create(
                  "filter2",
                  MapSettingType.LOCATION_FILTER_GEO_JSON_KEY,
                  Optional.of(LocalizedStrings.of(Locale.US, "Test Filter 2"))),
              QuestionSetting.create(
                  "filter3",
                  MapSettingType.LOCATION_FILTER_GEO_JSON_KEY,
                  Optional.of(LocalizedStrings.of(Locale.US, "Test Filter 3"))),
              QuestionSetting.create(
                  "filter4",
                  MapSettingType.LOCATION_FILTER_GEO_JSON_KEY,
                  Optional.of(LocalizedStrings.of(Locale.US, "Test Filter 4"))),
              QuestionSetting.create(
                  "filter5",
                  MapSettingType.LOCATION_FILTER_GEO_JSON_KEY,
                  Optional.of(LocalizedStrings.of(Locale.US, "Test Filter 5"))),
              QuestionSetting.create(
                  "filter6",
                  MapSettingType.LOCATION_FILTER_GEO_JSON_KEY,
                  Optional.of(LocalizedStrings.of(Locale.US, "Test Filter 6"))),
              QuestionSetting.create(
                  "filter7",
                  MapSettingType.LOCATION_FILTER_GEO_JSON_KEY,
                  Optional.of(LocalizedStrings.of(Locale.US, "Test Filter 7")))),
          ImmutableSet.of(CiviFormError.of("Question cannot have more than six filters"))
        },
        new Object[] {
          ImmutableSet.of(
              QuestionSetting.create("name", MapSettingType.LOCATION_NAME_GEO_JSON_KEY),
              QuestionSetting.create("address", MapSettingType.LOCATION_ADDRESS_GEO_JSON_KEY),
              QuestionSetting.create(
                  "details_url", MapSettingType.LOCATION_DETAILS_URL_GEO_JSON_KEY),
              QuestionSetting.create(
                  "",
                  MapSettingType.LOCATION_TAG_GEO_JSON_KEY,
                  Optional.of(LocalizedStrings.of(Locale.US, "Test Tag")),
                  Optional.of("value"),
                  Optional.of(LocalizedStrings.of(Locale.US, "Test Text")))),
          ImmutableSet.of(CiviFormError.of("Tag key cannot be empty"))
        },
        new Object[] {
          ImmutableSet.of(
              QuestionSetting.create("name", MapSettingType.LOCATION_NAME_GEO_JSON_KEY),
              QuestionSetting.create("address", MapSettingType.LOCATION_ADDRESS_GEO_JSON_KEY),
              QuestionSetting.create(
                  "details_url", MapSettingType.LOCATION_DETAILS_URL_GEO_JSON_KEY),
              QuestionSetting.create(
                  "tag",
                  MapSettingType.LOCATION_TAG_GEO_JSON_KEY,
                  Optional.of(LocalizedStrings.of(Locale.US, "")),
                  Optional.of("value"),
                  Optional.of(LocalizedStrings.of(Locale.US, "Test Text")))),
          ImmutableSet.of(CiviFormError.of("Tag display name cannot be empty"))
        });
  }

  @Test
  @Parameters(method = "getSettingsTestData")
  public void validate_mapSettings(
      ImmutableSet<QuestionSetting> questionSettings, ImmutableSet<CiviFormError> expectedErrors) {
    QuestionDefinitionConfig config =
        makeConfigBuilder()
            .setValidationPredicates(
                MapValidationPredicates.builder()
                    .setGeoJsonEndpoint(endpoint)
                    .setMaxLocationSelections(10)
                    .build())
            .setQuestionSettings(questionSettings)
            .build();

    QuestionDefinition question = new MapQuestionDefinition(config);

    ImmutableSet<CiviFormError> errors = question.validate();

    assertThat(errors).isEqualTo(expectedErrors);
  }

  private QuestionDefinitionConfig.Builder makeConfigBuilder() {
    return QuestionDefinitionConfig.builder()
        .setName("name")
        .setDescription("description")
        .setQuestionText(LocalizedStrings.of(Locale.US, "question?"));
  }
}
