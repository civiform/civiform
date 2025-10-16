package services.geojson;

/**
 * Exception thrown when CiviForm fails to access a GeoJSON due to either an authentication or
 * access issue. Civiform currently provides no way to attach credentials in our geoJSON request, so
 * this indicates the either need a different url or they need to change settings on their end.
 */
public final class GeoJsonAccessException extends RuntimeException {
  public GeoJsonAccessException(String message) {
    super(message);
  }
}
