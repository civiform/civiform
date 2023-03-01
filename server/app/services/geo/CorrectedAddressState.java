package services.geo;

import static com.google.common.base.Preconditions.checkNotNull;

/** The result of correcting an address. */
public enum CorrectedAddressState {
  CORRECTED("Corrected"),
  FAILED("Failed"),
  AS_ENTERED_BY_USER("AsEnteredByUser");

  private final String serializationFormat;

  CorrectedAddressState(String serializationFormat) {
    this.serializationFormat = checkNotNull(serializationFormat);
  }

  /** The string representation of this state for storage in the database. */
  public String getSerializationFormat() {
    return this.serializationFormat;
  }
}
