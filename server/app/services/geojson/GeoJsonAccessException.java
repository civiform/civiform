package services.geojson;

/**
 * Exception thrown when CiviForm fails to process GeoJSON from endpoint provided by CiviForm Admin
 */
public final class GeoJsonAccessException extends RuntimeException {
  public GeoJsonAccessException(String message) {
    super(message);
  }
}
