package services.geojson;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
    RuntimeException e = assertThrows(RuntimeException.class, () -> geoJsonClient.fetchGeoJson("test"));
    assertEquals("Invalid GeoJSON endpoint.", e.getMessage());
  }

  @Test
  public void fetchGeoJson_apiErrorResponse() {
    when(wsResponse.getStatus()).thenReturn(403);

    RuntimeException e = assertThrows(RuntimeException.class, () ->
      geoJsonClient.fetchGeoJson(endpoint).toCompletableFuture().join()
    );

    assertEquals("java.lang.RuntimeException: Failed to fetch GeoJSON: 403", e.getMessage());
  }

  @Test
  public void fetchGeoJson_emptyResponseBody() {
    when(wsResponse.getStatus()).thenReturn(200);
    when(wsResponse.getBody()).thenReturn("");

    RuntimeException e = assertThrows(RuntimeException.class, () -> {
      geoJsonClient.fetchGeoJson(endpoint).toCompletableFuture().join();
    });

    assertEquals("java.lang.RuntimeException: Empty GeoJSON response", e.getMessage());
  }

  @Test
  @Parameters(method = "invalidGeoJsons")
  public void fetchGeoJson_invalidGeoJSON(String testGeoJson, String expectedMessage) {
    when(wsResponse.getStatus()).thenReturn(200);
    when(wsResponse.getBody()).thenReturn(testGeoJson);

    RuntimeException e = assertThrows(RuntimeException.class, () -> {
      geoJsonClient.fetchGeoJson(endpoint).toCompletableFuture().join();
    });

    assertEquals("java.lang.RuntimeException: " + expectedMessage, e.getMessage());
  }

  private Object[] invalidGeoJsons() {
    return new Object[]{
      new Object[]{
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
        """,
        "Invalid GeoJSON format"
      },
      new Object[]{
        """
        {
          "type": "FeatureCollection",
          "features": [
            {
              "type": "Feature"
            }
          ]
        }
        """,
        "Feature is missing geometry or properties."
      },
      new Object[]{
        """
        {
          "type": "FeatureCollection",
          "features": []
        }
        """,
        "GeoJSON has no features."
      }
    };
  }
}
