package services.geo.esri;

/** Raised when an invalid address is passed to FakeEsriClient */
public class InvalidFakeAddressException extends RuntimeException {
  InvalidFakeAddressException(String address) {
    super(
        String.format(
            "Address passed to FakeEsriClient should be one of 'Legit Address', 'Bogus Address',"
                + " or 'Error Address'. Address used: %s",
            address));
  }
}
