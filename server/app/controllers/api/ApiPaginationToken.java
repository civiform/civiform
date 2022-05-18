package controllers.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ApiPaginationToken {
  private String serializedPayload;
  private String signature;

  public ApiPaginationToken(
      @JsonProperty("serializedPayload") String serializedPayload,
      @JsonProperty("signature") String signature) {
    this.serializedPayload = serializedPayload;
    this.signature = signature;
  }

  public String getSerializedPayload() {
    return serializedPayload;
  }

  public void setSerializedPayload(String serializedPayload) {
    this.serializedPayload = serializedPayload;
  }

  public String getSignature() {
    return signature;
  }

  public void setSignature(String signature) {
    this.signature = signature;
  }
}
