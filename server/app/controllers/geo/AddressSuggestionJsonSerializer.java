package controllers.geo;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.typesafe.config.Config;
import controllers.BadRequestException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import services.CryptographicUtils;
import services.geo.AddressSuggestion;

/**
 * Handles serializing and deserializing {@link AddressSuggestionNode} to pass to and submitted from
 * the UI.
 *
 * <p>Before serialization, the list of {@link AddressSuggestion}s is packaged in a {@link
 * AddressSuggesionNode} with an HMAC-SHA-256 signature of itself to ensure that the session user
 * does not modify the node which could cause unexpected behavior.
 *
 * <p>The steps for serializing address suggestions are:
 *
 * <ol>
 *   <li>Serialize the suggestions into a JSON string
 *   <li>Compute the HMAC-SHA-256 signature of the JSON string using the application secret as a
 *       signing key
 *   <li>Create a {@link AddressSuggestionNode} with the JSON string and the signature
 *   <li>Serialize the complete node into a JSON string
 *   <li>Base-64 encode the token JSON string into a UTF-8 string
 * </ol>
 *
 * <p>Deserializing a string is the same process in reverse, with a check to ensure the signature of
 * the provided token matches the freshly computed signature of the provided payload.
 */
public final class AddressSuggestionJsonSerializer {
  private static final ObjectMapper mapper =
      new ObjectMapper().registerModule(new GuavaModule()).registerModule(new Jdk8Module());

  private final String signingSecret;

  @Inject
  public AddressSuggestionJsonSerializer(Config appConfig) {
    this.signingSecret = checkNotNull(appConfig).getString("play.http.secret.key");
  }

  /**
   * Serializes the provided address suggestions into a string suitable for adding to a user's
   * session.
   */
  public String serialize(ImmutableList<AddressSuggestion> suggestions) {
    String serializedSuggestions = serializeJson(suggestions);
    String signature = CryptographicUtils.sign(serializedSuggestions, signingSecret);

    AddressSuggestionNode node = new AddressSuggestionNode(serializedSuggestions, signature);

    String serializedNode = serializeJson(node);

    return Base64.getEncoder().encodeToString(serializedNode.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Deserializes the provided address suggestion JSON, throwing a {@link
   * controllers.api.BadApiRequestException} if the JSON's signature is invalid.
   */
  public ImmutableList<AddressSuggestion> deserialize(String serializedJson) {
    String addressJson =
        new String(Base64.getDecoder().decode(serializedJson), StandardCharsets.UTF_8);
    AddressSuggestionNode node = deserializeJson(addressJson, AddressSuggestionNode.class);

    var computedPayloadSignature =
        CryptographicUtils.sign(node.getSerializedPayload(), signingSecret);

    if (!computedPayloadSignature.equals(node.getSignature())) {
      throw new BadRequestException("Address suggestion node signature invalid");
    }

    AddressSuggestion[] suggestions =
        deserializeJson(node.getSerializedPayload(), AddressSuggestion[].class);

    return ImmutableList.copyOf(suggestions);
  }

  private <T> T deserializeJson(String json, Class<T> clazz) {
    try {
      return mapper.readValue(json, clazz);
    } catch (JsonProcessingException e) {
      throw new BadRequestException("Error deserializing " + clazz.toString());
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
