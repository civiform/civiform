package services;

public enum AddressField {
  STREET("street"),
  LINE2("line2"),
  CITY("city"),
  STATE("state"),
  ZIP("zip");

  private final String addressFieldString;

  AddressField(String addressFieldString) {
    this.addressFieldString = addressFieldString;
  }

  public String getValue() {
    return addressFieldString;
  }
}
