package services.geo.esri;

/** Raised when the Esri Service Area Validation Config is invalid */
public final class InvalidEsriServiceAreaValidationConfigException extends RuntimeException {
  InvalidEsriServiceAreaValidationConfigException(String message) {
    super(message);
  }
}
