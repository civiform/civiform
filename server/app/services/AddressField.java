package services;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Defines address fields supported by {@link Address}
 */
public enum AddressField {
  STREET("street"),
  LINE2("line2"),
  CITY("city"),
  STATE("state"),
  ZIP("zip");

  private final String addressFieldString;

  AddressField(String addressFieldString) {
    this.addressFieldString = checkNotNull(addressFieldString);
  }

  public String getValue() {
    return addressFieldString;
  }
}
