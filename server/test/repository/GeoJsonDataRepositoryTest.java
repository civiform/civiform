package repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import models.GeoJsonDataModel;
import org.junit.Before;
import org.junit.Test;
import services.geojson.Feature;
import services.geojson.FeatureCollection;
import services.geojson.Geometry;

public class GeoJsonDataRepositoryTest extends ResetPostgres {
  private GeoJsonDataRepository geoJsonDataRepository;
  private DatabaseExecutionContext dbExecutionContext;

  private final String endpoint = "http://example.com/geo.json";
  private static final FeatureCollection testFeatureCollection1 =
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

  private static final FeatureCollection testFeatureCollection2 =
      new FeatureCollection(
          "FeatureCollection",
          List.of(
              new Feature(
                  "Feature",
                  new Geometry("Point", List.of(1.0, 1.0)),
                  Map.of("name", "Feature 2.1", "prop1", "value1", "prop2", "value2"),
                  "1"),
              new Feature(
                  "Feature",
                  new Geometry("Point", List.of(1.0, 2.0)),
                  Map.of("name", "Feature 2.2", "prop1", "value1", "prop2", "value2"),
                  "2"),
              new Feature(
                  "Feature",
                  new Geometry("Point", List.of(2.0, 3.0)),
                  Map.of("name", "Feature 2.3", "prop1", "value1", "prop2", "value2"),
                  "3")));

  @Before
  public void setup() {
    geoJsonDataRepository = instanceOf(GeoJsonDataRepository.class);
    dbExecutionContext = instanceOf(DatabaseExecutionContext.class);

    // Create a database record
    GeoJsonDataModel firstEntry = new GeoJsonDataModel();
    firstEntry.setEndpoint(endpoint);
    firstEntry.setGeoJson(testFeatureCollection1);
    firstEntry.setConfirmTime(Instant.ofEpochSecond(1685047575)); // May 25, 2023 4:46 pm EDT
    CompletableFuture.runAsync(firstEntry::save, dbExecutionContext).join();
  }

  @Test
  public void getMostRecentGeoJsonDataRowForEndpoint_success() {
    // Create a second record in the database
    GeoJsonDataModel secondEntry = new GeoJsonDataModel();
    secondEntry.setEndpoint(endpoint);
    secondEntry.setGeoJson(testFeatureCollection2);
    secondEntry.setConfirmTime(Instant.ofEpochSecond(1685133975)); // May 26, 2023 4:46 pm EDT
    CompletableFuture.runAsync(secondEntry::save, dbExecutionContext).join();

    Optional<GeoJsonDataModel> result =
        geoJsonDataRepository
            .getMostRecentGeoJsonDataRowForEndpoint(endpoint)
            .toCompletableFuture()
            .join();

    // Confirm that we get the second record
    assertThat(result).isNotNull();
    assertThat(result.isPresent()).isTrue();
    assertThat(result.get().getGeoJson()).isNotNull();
    assertThat(result.get().getGeoJson()).isEqualTo(testFeatureCollection2);
  }

  @Test
  public void saveGeoJson_saveNewGeoJson_success() {
    geoJsonDataRepository.saveGeoJson(endpoint, testFeatureCollection2);

    Optional<GeoJsonDataModel> model =
        geoJsonDataRepository
            .getMostRecentGeoJsonDataRowForEndpoint(endpoint)
            .toCompletableFuture()
            .join();
    assertTrue(model.isPresent() && model.get().getGeoJson().equals(testFeatureCollection2));
  }

  @Test
  public void saveGeoJson_emptyRow_saveNewGeoJson_success() {
    String newEndpoint = "http://example.com/geo-new.json";

    Optional<GeoJsonDataModel> maybeOldGeoJson =
        geoJsonDataRepository
            .getMostRecentGeoJsonDataRowForEndpoint(newEndpoint)
            .toCompletableFuture()
            .join();
    assertTrue(maybeOldGeoJson.isEmpty());

    // Try to save GeoJson at a new endpoint that isn't in the database yet
    geoJsonDataRepository.saveGeoJson(newEndpoint, testFeatureCollection2);

    Optional<GeoJsonDataModel> maybeNewGeoJson =
        geoJsonDataRepository
            .getMostRecentGeoJsonDataRowForEndpoint(newEndpoint)
            .toCompletableFuture()
            .join();
    assertTrue(
        maybeNewGeoJson.isPresent()
            && maybeNewGeoJson.get().getGeoJson().equals(testFeatureCollection2));
  }

  @Test
  public void saveGeoJson_updateOldGeoJsonConfirmTime_success() {
    Optional<GeoJsonDataModel> maybeOldGeoJson =
        geoJsonDataRepository
            .getMostRecentGeoJsonDataRowForEndpoint(endpoint)
            .toCompletableFuture()
            .join();
    assertTrue(maybeOldGeoJson.isPresent());

    Instant oldConfirmTime = maybeOldGeoJson.get().getConfirmTime();
    FeatureCollection oldGeoJson = maybeOldGeoJson.get().getGeoJson();
    geoJsonDataRepository.saveGeoJson(endpoint, testFeatureCollection1);

    Optional<GeoJsonDataModel> maybeNewGeoJson =
        geoJsonDataRepository
            .getMostRecentGeoJsonDataRowForEndpoint(endpoint)
            .toCompletableFuture()
            .join();
    assertTrue(maybeNewGeoJson.isPresent());
    Instant newConfirmTime = maybeNewGeoJson.get().getConfirmTime();
    FeatureCollection newGeoJson = maybeNewGeoJson.get().getGeoJson();

    assertEquals(testFeatureCollection1, oldGeoJson);
    assertEquals(testFeatureCollection1, newGeoJson);
    assertNotEquals(oldConfirmTime, newConfirmTime);
  }
}
