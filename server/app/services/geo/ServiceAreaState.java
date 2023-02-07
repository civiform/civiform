package services.geo;

import static com.google.common.base.Preconditions.checkNotNull;

/** The result of checking an address for membership in a service area. */
public enum ServiceAreaState {
  // The address is in the service area.
  IN_AREA("InArea"),
  // The address is not in the service area.
  NOT_IN_AREA("NotInArea"),
  // The check failed for technical reasons.
  FAILED("Failed");

  private final String serializationFormat;

  ServiceAreaState(String serializationFormat) {
    this.serializationFormat = checkNotNull(serializationFormat);
  }

  /** The string representation of this state for storage in the database. */
  public String getSerializationFormat() {
    return this.serializationFormat;
  }
}
