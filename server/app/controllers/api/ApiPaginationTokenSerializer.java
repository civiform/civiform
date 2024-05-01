package controllers.api;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.inject.Inject;
import com.typesafe.config.Config;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import services.CryptographicUtils;

/**
 * Handles serializing and deserializing {@link ApiPaginationToken}s for paginated API endpoints.
 *
 * <p>Pagination token semantic contents are held in {@link ApiPaginationTokenPayload} instances.
 * The contents of a payload class is what a paginating API handler needs to provide the next page
 * of results of a paginated request. Before serialization, the payload is packaged in a {@link
 * ApiPaginationToken} with an HMAC-SHA-256 signature of itself to ensure that the API consumer does
 * not modify the pagination token which could cause unexpected behavior.
 *
 * <p>The steps for serializing a payload are:
 *
 * <ol>
 *   <li>Serialize the payload into a JSON string
 *   <li>Compute the HMAC-SHA-256 signature of the payload JSON string using the application secret
 *       as a signing key
 *   <li>Create a {@link ApiPaginationToken} with the JSON string and the signature
 *   <li>Serialize the complete pagination token into a JSON string
 *   <li>Base-64 encode the token JSON string into a UTF-8 string
 * </ol>
 *
 * <p>Deserializing a string is the same process in reverse, with a check to ensure the signature of
 * the provided token matches the freshly computed signature of the provided payload.
 */
public final class ApiPaginationTokenSerializer {
  private static final ObjectMapper mapper =
      new ObjectMapper().registerModule(new GuavaModule()).registerModule(new Jdk8Module());

  private final String signingSecret;

  @Inject
  public ApiPaginationTokenSerializer(Config appConfig) {
    this.signingSecret = checkNotNull(appConfig).getString("play.http.secret.key");
  }

  /**
   * Serializes the provided payload into a string suitable for returning to API callers as a
   * pagination token.
   */
  public String serialize(ApiPaginationTokenPayload apiPaginationTokenPayload) {
    String serializedPayload = serializeJson(apiPaginationTokenPayload);
    String signature = CryptographicUtils.sign(serializedPayload, signingSecret);

    ApiPaginationToken token = new ApiPaginationToken(serializedPayload, signature);
    String serializedToken = serializeJson(token);

    return Base64.getEncoder().encodeToString(serializedToken.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Deserializes the provided pagination token, throwing a {@link BadApiRequestException} if
   * deserialization fails or the token's signature is invalid.
   */
  public ApiPaginationTokenPayload deserialize(String serializedPageToken) {
    String tokenJson =
        new String(Base64.getDecoder().decode(serializedPageToken), StandardCharsets.UTF_8);
    ApiPaginationToken token = deserializeJson(tokenJson, ApiPaginationToken.class);

    var computedPayloadSignature =
        CryptographicUtils.sign(token.getSerializedPayload(), signingSecret);

    if (!computedPayloadSignature.equals(token.getSignature())) {
      throw new BadApiRequestException("Page token signature invalid");
    }

    return deserializeJson(token.getSerializedPayload(), ApiPaginationTokenPayload.class);
  }

  private <T> T deserializeJson(String json, Class<T> clazz) {
    try {
      return mapper.readValue(json, clazz);
    } catch (JsonProcessingException e) {
      throw new BadApiRequestException("Error deserializing " + clazz.toString());
    }
  }

  private String serializeJson(Object object) {
    try {
      return mapper.writeValueAsString(object);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}
