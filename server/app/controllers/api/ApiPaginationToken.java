package controllers.api;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents the contents of an API pagination token in an intermediate serialization step. See
 * {@link ApiPaginationTokenSerializer} for more information.
 */
public class ApiPaginationToken {
  private final String serializedPayload;
  private final String signature;

  public ApiPaginationToken(
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
