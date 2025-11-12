package services.geojson;

/**
 * Exception thrown when CiviForm fails to process GeoJSON from endpoint provided by CiviForm Admin
 * due to no file being available at the endpoint.
 */
public final class GeoJsonNotFoundException extends RuntimeException {
  public GeoJsonNotFoundException(String message) {
    super(message);
  }
}
