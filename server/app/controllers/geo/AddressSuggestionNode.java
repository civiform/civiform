package controllers.geo;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents the contents of an address suggestion returned from a user session in an intermeidate
 * serialization step.
 */
public final class AddressSuggestionNode {
  private final String serializedPayload;
  private final String signature;

  public AddressSuggestionNode(
      @JsonProperty("serializedPayload") String serializedPayload,
      @JsonProperty("signature") String signature) {
    this.serializedPayload = serializedPayload;
    this.signature = signature;
  }

  public String getSerializedPayload() {
    return serializedPayload;
  }

  public String getSignature() {
    return signature;
  }
}
