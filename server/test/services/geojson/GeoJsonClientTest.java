package services.geojson;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;
import play.libs.ws.WSResponse;
import play.test.WithApplication;
import repository.GeoJsonDataRepository;

@RunWith(JUnitParamsRunner.class)
public class GeoJsonClientTest extends WithApplication {
  private WSResponse wsResponse;
  private GeoJsonClient geoJsonClient;

  private final String endpoint = "http://example.com/geo.json";

  @Before
  public void setUp() throws Exception {
    WSClient wsClient = mock(WSClient.class);
    GeoJsonDataRepository geoJsonDataRepository = mock(GeoJsonDataRepository.class);
    ObjectMapper objectMapper = new ObjectMapper();

    geoJsonClient = new GeoJsonClient(wsClient, geoJsonDataRepository, objectMapper);

    wsResponse = mock(WSResponse.class);
    WSRequest wsRequest = mock(WSRequest.class);

    when(wsClient.url(endpoint)).thenReturn(wsRequest);
    when(wsRequest.get()).thenReturn(CompletableFuture.completedFuture(wsResponse));
  }

  @Test
  public void fetchGeoJson_invalidEndpoint() {
    RuntimeException e =
        assertThrows(
            CompletionException.class,
            () -> geoJsonClient.fetchGeoJson("test").toCompletableFuture().join());
    assertTrue(e.getCause() instanceof GeoJsonProcessingException);
    assertEquals("Invalid GeoJSON endpoint", e.getCause().getMessage());
  }

  @Test
  public void fetchGeoJson_apiErrorResponse() {
    when(wsResponse.getStatus()).thenReturn(403);

    RuntimeException e =
        assertThrows(
            CompletionException.class,
            () -> geoJsonClient.fetchGeoJson(endpoint).toCompletableFuture().join());

    assertTrue(e.getCause() instanceof GeoJsonProcessingException);
    assertEquals("Failed to fetch GeoJSON", e.getCause().getMessage());
  }

  @Test
  public void fetchGeoJson_emptyResponseBody() {
    when(wsResponse.getStatus()).thenReturn(200);
    when(wsResponse.getBody()).thenReturn("");

    RuntimeException e =
        assertThrows(
            CompletionException.class,
            () -> {
              geoJsonClient.fetchGeoJson(endpoint).toCompletableFuture().join();
            });

    assertTrue(e.getCause() instanceof GeoJsonProcessingException);
    assertEquals("Empty GeoJSON response", e.getCause().getMessage());
  }

  @Test
  @Parameters(method = "invalidGeoJsons")
  public void fetchGeoJson_invalidGeoJSON(String testGeoJson) {
    when(wsResponse.getStatus()).thenReturn(200);
    when(wsResponse.getBody()).thenReturn(testGeoJson);

    RuntimeException e =
        assertThrows(
            CompletionException.class,
            () -> {
              geoJsonClient.fetchGeoJson(endpoint).toCompletableFuture().join();
            });

    assertTrue(e.getCause() instanceof GeoJsonProcessingException);
    assertEquals("Invalid GeoJSON format", e.getCause().getMessage());
  }

  private Object[] invalidGeoJsons() {
    return new Object[] {
      new Object[] {
        """
        {
          "type": "FeatureCollection",
          "features": [
            {
              "type": "Feature",
              "geometry": {
                "type": "Point",
                "coordinates": [102.0, 0.5]
              },
              "properties": {
                "name": "Example Point"
              }
            }
        """
      },
      new Object[] {
        """
        {
          "type": "FeatureCollection",
          "features": [
            {
              "type": "Feature"
            }
          ]
        }
        """
      },
      new Object[] {
        """
        {
          "type": "FeatureCollection",
          "features": []
        }
        """
      }
    };
  }
}
