package durablejobs.jobs;

import static org.assertj.core.api.Assertions.assertThat;

import io.ebean.DB;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import models.GeoJsonDataModel;
import models.JobType;
import models.PersistedDurableJobModel;
import org.junit.Before;
import org.junit.Test;
import repository.DatabaseExecutionContext;
import repository.GeoJsonDataRepository;
import repository.ResetPostgres;
import services.geojson.Feature;
import services.geojson.FeatureCollection;
import services.geojson.GeoJsonClient;
import services.geojson.Geometry;

public class MapRefreshJobTest extends ResetPostgres {
  private DatabaseExecutionContext dbExecutionContext;
  private GeoJsonDataModel geoJsonData;

  @Before
  public void setup() throws IOException {
    dbExecutionContext = instanceOf(DatabaseExecutionContext.class);
    geoJsonData = resourceCreator.insertGeoJsonData(dbExecutionContext);
  }

  @Test
  public void run_mapRefreshJobIfDataUpdated() throws IOException {
    // Change the stored data to be different from what mock service will return
    FeatureCollection differentData =
        new FeatureCollection(
            "FeatureCollection",
            List.of(
                new Feature(
                    "Feature",
                    new Geometry("Point", List.of(-122.0, 37.0)),
                    Map.of("name", "Different Test Location", "prop1", "value1", "prop2", "value2"),
                    "test-different")));

    geoJsonData.setGeoJson(differentData);
    CompletableFuture.runAsync(geoJsonData::save, dbExecutionContext).join();

    int originalCount = getGeoJsonDataCount();

    runJob();
    geoJsonData.refresh();

    int newCount = getGeoJsonDataCount();
    assertThat(newCount).isEqualTo(originalCount + 1);
  }

  @Test
  public void run_mapRefreshJobIfDataNotUpdated() throws IOException {
    Instant originalConfirmTime = geoJsonData.getConfirmTime();
    int originalCount = getGeoJsonDataCount();

    // Add small delay to ensure timestamp difference
    try {
      Thread.sleep(10);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    runJob();

    geoJsonData.refresh();
    int newCount = getGeoJsonDataCount();

    assertThat(newCount).isEqualTo(originalCount);
    assertThat(geoJsonData.getConfirmTime()).isAfter(originalConfirmTime);
  }

  private int getGeoJsonDataCount() {
    return CompletableFuture.supplyAsync(
            () -> DB.find(GeoJsonDataModel.class).findCount(), dbExecutionContext)
        .join();
  }

  private void runJob() {
    MapRefreshJob job =
        new MapRefreshJob(
            new PersistedDurableJobModel("fake-job", JobType.RUN_ONCE, Instant.now()),
            instanceOf(GeoJsonDataRepository.class),
            instanceOf(GeoJsonClient.class));
    job.run();
  }
}
