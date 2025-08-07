package repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import models.GeoJsonDataModel;
import org.junit.Test;
import services.geojson.Feature;
import services.geojson.FeatureCollection;
import services.geojson.Geometry;

public class GeoJsonDataRepositoryTest extends ResetPostgres {
  private final GeoJsonDataRepository geoJsonDataRepository =
      instanceOf(GeoJsonDataRepository.class);
  private final String endpoint = "http://example.com/geo.json";
  private static final FeatureCollection testFeatureCollection1 =
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

  private static final FeatureCollection testFeatureCollection2 =
      new FeatureCollection(
          "FeatureCollection",
          List.of(
              new Feature(
                  "Feature",
                  new Geometry("Point", List.of(1.0, 1.0)),
                  Map.of("name", "Feature 2.1"),
                  "1"),
              new Feature(
                  "Feature",
                  new Geometry("Point", List.of(1.0, 2.0)),
                  Map.of("name", "Feature 2.2"),
                  "2"),
              new Feature(
                  "Feature",
                  new Geometry("Point", List.of(2.0, 3.0)),
                  Map.of("name", "Feature 2.3"),
                  "3")));

  @Test
  public void getMostRecentGeoJsonDataRowForEndpoint_success() {
    createInitialDbRecord();

    // Create a second record in the database
    GeoJsonDataModel secondEntry = new GeoJsonDataModel();
    secondEntry.setEndpoint(endpoint);
    secondEntry.setGeoJson(testFeatureCollection2);
    secondEntry.setConfirmTime(Instant.ofEpochSecond(1685133975)); // May 26, 2023 4:46 pm EDT
    secondEntry.save();

    Optional<GeoJsonDataModel> result =
        geoJsonDataRepository
            .getMostRecentGeoJsonDataRowForEndpoint(endpoint)
            .toCompletableFuture()
            .join();

    // Confirm that we get the second record
    assertTrue(result.isPresent() && result.get().getGeoJson().equals(testFeatureCollection2));
  }

  @Test
  public void saveGeoJson_saveNewGeoJson_success() {
    createInitialDbRecord();

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

    createInitialDbRecord();

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
    createInitialDbRecord();

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

  private void createInitialDbRecord() {
    // Create a database record
    GeoJsonDataModel firstEntry = new GeoJsonDataModel();
    firstEntry.setEndpoint(endpoint);
    firstEntry.setGeoJson(testFeatureCollection1);
    firstEntry.setConfirmTime(Instant.ofEpochSecond(1685047575)); // May 25, 2023 4:46 pm EDT
    firstEntry.save();
  }
}
