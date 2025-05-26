package services.apibridge;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Data Transfer Objects for CiviForm Integration Service API. Generated from OpenAPI specification
 * version 0.9.9.
 */
public final class ApiBridgeServiceDto {
  /** Compatibility level number for an endpoint. */
  public enum CompatibilityLevel {
    @JsonProperty("v1")
    V1
  }

  /** Validation error message details. */
  public record ValidationError(
      @JsonProperty("name") String name, @JsonProperty("message") String message) {}

  /** An RFC 9457 problem object. */
  public record ProblemDetail(
      @JsonProperty("type") String type,
      @JsonProperty("title") String title,
      @JsonProperty("status") Integer status,
      @JsonProperty("detail") String detail,
      @JsonProperty("instance") String instance) {}

  /** Extended Problem Detail for validation errors. */
  public record ValidationProblemDetail(
      @JsonProperty("type") String type,
      @JsonProperty("title") String title,
      @JsonProperty("status") Integer status,
      @JsonProperty("detail") String detail,
      @JsonProperty("instance") String instance,
      @JsonProperty("validation_errors") ImmutableList<ValidationError> validationErrors) {
    public ValidationProblemDetail {
      validationErrors =
          validationErrors != null ? ImmutableList.copyOf(validationErrors) : ImmutableList.of();
    }
  }

  /** Endpoint definition for an integration. */
  public record Endpoint(
      @JsonProperty("compatibility_level") CompatibilityLevel compatibilityLevel,
      @JsonProperty("description") String description,
      @JsonProperty("uri") String uri,
      @JsonProperty("request_schema") JsonNode requestSchema,
      @JsonProperty("response_schema") JsonNode responseSchema) {
    public String requestSchemaChecksum() {
      try {
        return HexFormat.of()
            .formatHex(
                MessageDigest.getInstance("SHA-256").digest(requestSchema.toString().getBytes()));
      } catch (NoSuchAlgorithmException e) {
        throw new RuntimeException(e);
      }
    }

    public String responseSchemaChecksum() {
      try {
        return HexFormat.of()
            .formatHex(
                MessageDigest.getInstance("SHA-256").digest(responseSchema.toString().getBytes()));
      } catch (NoSuchAlgorithmException e) {
        throw new RuntimeException(e);
      }
    }
  }

  /** Response object for the health-check endpoint. */
  public record HealthcheckResponse(@JsonProperty("timestamp") long timestamp) {}

  /** Response object for the discovery endpoint. */
  public record DiscoveryResponse(
      @JsonProperty("endpoints") ImmutableMap<String, Endpoint> endpoints) {
    public DiscoveryResponse {
      endpoints = endpoints != null ? ImmutableMap.copyOf(endpoints) : ImmutableMap.of();
    }
  }

  /** Request object for the bridge endpoint. */
  public record BridgeRequest(@JsonProperty("payload") ImmutableMap<String, Object> payload) {
    public BridgeRequest {
      payload = payload != null ? ImmutableMap.copyOf(payload) : ImmutableMap.of();
    }
  }

  /** Response object for the bridge endpoint. */
  public record BridgeResponse(
      @JsonProperty("compatibility_level") CompatibilityLevel compatibilityLevel,
      @JsonProperty("payload") ImmutableMap<String, Object> payload) {
    public BridgeResponse {
      payload = payload != null ? ImmutableMap.copyOf(payload) : ImmutableMap.of();
    }
  }
}
