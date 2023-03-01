package controllers.geo;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents the contents of an address suggestion returned from a user session in an intermediate
 * serialization step.
 */
public final class AddressSuggestionNode {
  private final String serializedPayload;
  private final String signature;

  public AddressSuggestionNode(
      @JsonProperty("serializedPayload") String serializedPayload,
      @JsonProperty("signature") String signature) {
    this.serializedPayload = checkNotNull(serializedPayload);
    this.signature = checkNotNull(signature);
  }

  /** Serialized json payload containing the data stored in the user session */
  public String getSerializedPayload() {
    return serializedPayload;
  }

  /** Cryptographic signature used to verify the integrity of the serialized json payload */
  public String getSignature() {
    return signature;
  }
}
