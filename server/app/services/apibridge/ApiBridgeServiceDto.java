package services.apibridge;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Data Transfer Objects for CiviForm API Bridge. */
public final class ApiBridgeServiceDto {
  /** Compatibility level number for an endpoint. */
  public enum CompatibilityLevel {
    @JsonProperty("v1")
    V1
  }

  /**
   * Validation error message details.
   *
   * @param name Name of the field that failed validation.
   * @param message Message describing the validation error.
   */
  public record ValidationError(
      @JsonProperty("name") String name, @JsonProperty("message") String message) {}

  /**
   * Common interface for RFC 9457 problem details. Implemented by {@link ProblemDetail} and {@link
   * ValidationProblemDetail}.
   */
  public interface IProblemDetail {
    /** A URI reference that identifies the problem type. */
    String type();

    /** A short, human-readable summary of the problem type. */
    String title();

    /** The HTTP status code for this occurrence of the problem. */
    Integer status();

    /** A human-readable explanation specific to this occurrence of the problem. */
    String detail();

    /** A URI reference that identifies the specific occurrence of the problem. */
    String instance();
  }

  /**
   * An RFC 9457 problem object.
   *
   * @param type A URI reference that identifies the problem type.
   * @param title A short, human-readable summary of the problem type.
   * @param status The HTTP status code for this occurrence of the problem.
   * @param detail A human-readable explanation specific to this occurrence of the problem.
   * @param instance A URI reference that identifies the specific occurrence of the problem.
   */
  public record ProblemDetail(
      @JsonProperty("type") String type,
      @JsonProperty("title") String title,
      @JsonProperty("status") Integer status,
      @JsonProperty("detail") String detail,
      @JsonProperty("instance") String instance)
      implements IProblemDetail {}

  /**
   * Extended Problem Detail for validation errors.
   *
   * @param type A URI reference that identifies the problem type.
   * @param title A short, human-readable summary of the problem type.
   * @param status The HTTP status code for this occurrence of the problem.
   * @param detail A human-readable explanation specific to this occurrence of the problem.
   * @param instance A URI reference that identifies the specific occurrence of the problem.
   * @param validationErrors A list of validation errors providing details about the failure.
   */
  public record ValidationProblemDetail(
      @JsonProperty("type") String type,
      @JsonProperty("title") String title,
      @JsonProperty("status") Integer status,
      @JsonProperty("detail") String detail,
      @JsonProperty("instance") String instance,
      @JsonProperty("validation_errors") ImmutableList<ValidationError> validationErrors)
      implements IProblemDetail {
    public ValidationProblemDetail {
      validationErrors =
          validationErrors != null ? ImmutableList.copyOf(validationErrors) : ImmutableList.of();
    }
  }

  /**
   * Endpoint definition for an integration.
   *
   * @param compatibilityLevel Compatibility level of the endpoint.
   * @param description Description of what the endpoint is used for.
   * @param uri URI path to the bridge endpoint.
   * @param requestSchema JSON schema for validating requests to the endpoint.
   * @param responseSchema JSON schema for validating responses from the endpoint.
   */
  public record Endpoint(
      @JsonProperty("compatibility_level") CompatibilityLevel compatibilityLevel,
      @JsonProperty("description") String description,
      @JsonProperty("uri") String uri,
      @JsonProperty("request_schema") JsonNode requestSchema,
      @JsonProperty("response_schema") JsonNode responseSchema) {

    /** Returns a SHA-256 string of the requestSchema for use as a checksum */
    public String requestSchemaChecksum() {
      try {
        return HexFormat.of()
            .formatHex(
                MessageDigest.getInstance("SHA-256")
                    .digest(requestSchema.toString().getBytes(StandardCharsets.UTF_8)));
      } catch (NoSuchAlgorithmException e) {
        throw new RuntimeException(e);
      }
    }

    /** Returns a SHA-256 string of the responseSchema for use as a checksum */
    public String responseSchemaChecksum() {
      try {
        return HexFormat.of()
            .formatHex(
                MessageDigest.getInstance("SHA-256")
                    .digest(responseSchema.toString().getBytes(StandardCharsets.UTF_8)));
      } catch (NoSuchAlgorithmException e) {
        throw new RuntimeException(e);
      }
    }
  }

  /**
   * Response object for the health-check endpoint.
   *
   * @param timestamp Unix timestamp of the health check response.
   */
  public record HealthcheckResponse(@JsonProperty("timestamp") long timestamp) {}

  /**
   * Response object for the discovery endpoint.
   *
   * @param endpoints Map of endpoint slugs to their respective endpoint metadata.
   */
  public record DiscoveryResponse(
      @JsonProperty("endpoints") ImmutableMap<String, Endpoint> endpoints) {
    public DiscoveryResponse {
      endpoints = endpoints != null ? ImmutableMap.copyOf(endpoints) : ImmutableMap.of();
    }
  }

  /**
   * Request object for the bridge endpoint.
   *
   * @param payload Arbitrary payload to send to the bridge endpoint.
   */
  public record BridgeRequest(@JsonProperty("payload") ImmutableMap<String, Object> payload) {
    public BridgeRequest {
      payload = payload != null ? ImmutableMap.copyOf(payload) : ImmutableMap.of();
    }
  }

  /**
   * Response object for the bridge endpoint.
   *
   * @param compatibilityLevel Compatibility level of the response.
   * @param payload Arbitrary payload returned from the bridge endpoint.
   */
  public record BridgeResponse(
      @JsonProperty("compatibility_level") CompatibilityLevel compatibilityLevel,
      @JsonProperty("payload") ImmutableMap<String, Object> payload) {
    public BridgeResponse {
      payload = payload != null ? ImmutableMap.copyOf(payload) : ImmutableMap.of();
    }
  }
}
