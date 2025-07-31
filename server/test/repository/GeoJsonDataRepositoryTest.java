package repository;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import models.GeoJsonDataModel;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import play.test.WithApplication;
import services.geojson.Feature;
import services.geojson.FeatureCollection;
import services.geojson.Geometry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class GeoJsonDataRepositoryTest extends WithApplication {
  private GeoJsonDataRepository geoJsonDataRepository;
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

  @Before
  public void setup() {
    geoJsonDataRepository = new GeoJsonDataRepository();

    // Create a database record
    GeoJsonDataModel firstEntry = new GeoJsonDataModel();
    firstEntry.setEndpoint(endpoint);
    firstEntry.setGeoJson(testFeatureCollection1);
    firstEntry.setConfirmTime(Instant.ofEpochSecond(1685047575)); // May 25, 2023 4:46 pm EDT
    firstEntry.save();
  }

  @Test
  public void getMostRecentGeoJsonDataRowForEndpoint_success() {
    // Create a second record in the database
    GeoJsonDataModel secondEntry = new GeoJsonDataModel();
    secondEntry.setEndpoint(endpoint);
    secondEntry.setGeoJson(testFeatureCollection2);
    secondEntry.setConfirmTime(Instant.ofEpochSecond(1685133975)); // May 26, 2023 4:46 pm EDT
    secondEntry.save();

    Optional<GeoJsonDataModel> result = geoJsonDataRepository.getMostRecentGeoJsonDataRowForEndpoint(endpoint);

    // Confirm that we get the second record
    assertTrue(result.isPresent() && result.get().getGeoJson().equals(testFeatureCollection2));
  }

  @Test
  public void saveGeoJson_saveNewGeoJson_success() {
    geoJsonDataRepository.saveGeoJson(endpoint, testFeatureCollection2);

    Optional<GeoJsonDataModel> model = geoJsonDataRepository.getMostRecentGeoJsonDataRowForEndpoint(endpoint);
    assertTrue(model.isPresent() && model.get().getGeoJson().equals(testFeatureCollection2));
  }

  @Test
  public void saveGeoJson_emptyRow_saveNewGeoJson_success() {
    String newEndpoint = "http://example.com/geo-new.json";

    Optional<GeoJsonDataModel> maybeOldGeoJson = geoJsonDataRepository.getMostRecentGeoJsonDataRowForEndpoint(newEndpoint);
    assertTrue(maybeOldGeoJson.isEmpty());

    // Try to save GeoJson at a new endpoint that isn't in the database yet
    geoJsonDataRepository.saveGeoJson(newEndpoint, testFeatureCollection2);

    Optional<GeoJsonDataModel> maybeNewGeoJson = geoJsonDataRepository.getMostRecentGeoJsonDataRowForEndpoint(newEndpoint);
    assertTrue(maybeNewGeoJson.isPresent() && maybeNewGeoJson.get().getGeoJson().equals(testFeatureCollection2));
  }

  @Test
  public void saveGeoJson_updateOldGeoJsonConfirmTime_success() {
    Optional<GeoJsonDataModel> maybeOldGeoJson = geoJsonDataRepository.getMostRecentGeoJsonDataRowForEndpoint(endpoint);
    assertTrue(maybeOldGeoJson.isPresent());

    Instant oldConfirmTime = maybeOldGeoJson.get().getConfirmTime();
    FeatureCollection oldGeoJson = maybeOldGeoJson.get().getGeoJson();
    geoJsonDataRepository.saveGeoJson(endpoint, testFeatureCollection1);

    Optional<GeoJsonDataModel> maybeNewGeoJson = geoJsonDataRepository.getMostRecentGeoJsonDataRowForEndpoint(endpoint);
    assertTrue(maybeNewGeoJson.isPresent());
    Instant newConfirmTime = maybeNewGeoJson.get().getConfirmTime();
    FeatureCollection newGeoJson = maybeNewGeoJson.get().getGeoJson();

    assertEquals(testFeatureCollection1, oldGeoJson);
    assertEquals(testFeatureCollection1, newGeoJson);
    assertNotEquals(oldConfirmTime, newConfirmTime);
  }
}
