package services.geo.esri;

/** Raised for failed calls to Esri service */
public class EsriClientRequestException extends RuntimeException {
  public EsriClientRequestException(String message) {
    super(message);
  }
}
