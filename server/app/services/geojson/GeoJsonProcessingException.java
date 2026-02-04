package services.geojson;

/**
 * Exception thrown when CiviForm fails to process GeoJSON from endpoint provided by CiviForm Admin
 */
public final class GeoJsonProcessingException extends RuntimeException {
  public GeoJsonProcessingException(String message) {
    super(message);
  }
}
