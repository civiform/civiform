package services.geo;

/**
 * Interface for working with address correction APIs
 */
public interface AddressCorrectionClient {
  /**
   * Returns a wkid and an array of candidates by best match
   * @param address The single line address to correct
   */
  AddressCandidates getAddressCandidates(String address);
}
