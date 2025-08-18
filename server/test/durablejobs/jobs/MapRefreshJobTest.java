package durablejobs.jobs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
  private GeoJsonDataModel testGeoJsonData1;
  private FeatureCollection testGeoJsonData2;
  private GeoJsonClient mockClient;

  @Before
  public void setup() throws IOException {
    dbExecutionContext = instanceOf(DatabaseExecutionContext.class);
    testGeoJsonData1 = resourceCreator.insertGeoJsonData();
    mockClient = mock(GeoJsonClient.class);

    testGeoJsonData2 =
        new FeatureCollection(
            "FeatureCollection",
            List.of(
                new Feature(
                    "Feature",
                    new Geometry("Point", List.of(-122.0, 37.0)),
                    Map.of("name", "Test Location"),
                    "test-1")));
  }

  @Test
  public void run_mapRefreshJobIfDataUpdated() throws IOException {
    int originalCount = getGeoJsonDataCount();
    when(mockClient.fetchGeoJson(anyString()))
        .thenReturn(CompletableFuture.completedFuture(testGeoJsonData2));
    runJob(mockClient);
    testGeoJsonData1.refresh();

    int newCount = getGeoJsonDataCount();
    assertThat(newCount).isEqualTo(originalCount + 1);
  }

  @Test
  public void run_mapRefreshJobIfDataNotUpdated() throws IOException {
    Instant originalConfirmTime = testGeoJsonData1.getConfirmTime();
    int originalCount = getGeoJsonDataCount();
    when(mockClient.fetchGeoJson(anyString()))
        .thenReturn(CompletableFuture.completedFuture(testGeoJsonData1.getGeoJson()));

    // Add small delay to ensure timestamp difference
    try {
      Thread.sleep(10);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    runJob(mockClient);
    testGeoJsonData1.refresh();

    int newCount = getGeoJsonDataCount();
    assertThat(newCount).isEqualTo(originalCount);
    assertThat(testGeoJsonData1.getConfirmTime()).isAfter(originalConfirmTime);
  }

  private int getGeoJsonDataCount() {
    return CompletableFuture.supplyAsync(
            () -> DB.find(GeoJsonDataModel.class).findCount(), dbExecutionContext)
        .join();
  }

  private void runJob(GeoJsonClient mockClient) {
    MapRefreshJob job =
        new MapRefreshJob(
            new PersistedDurableJobModel("fake-job", JobType.RUN_ONCE, Instant.now()),
            instanceOf(GeoJsonDataRepository.class),
            mockClient);
    job.run();
  }
}
