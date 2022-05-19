package controllers.api;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.inject.Inject;
import com.typesafe.config.Config;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import services.CryptographicUtils;

public class ApiPaginationTokenSerializer {
  private static final ObjectMapper mapper =
      new ObjectMapper().registerModule(new GuavaModule()).registerModule(new Jdk8Module());

  private final String signingSecret;

  @Inject
  public ApiPaginationTokenSerializer(Config appConfig) {
    this.signingSecret = checkNotNull(appConfig).getString("play.http.secret.key");
  }

  public String serialize(ApiPaginationTokenPayload apiPaginationTokenPayload) {
    String serializedPayload = serializeJson(apiPaginationTokenPayload);
    String signature = CryptographicUtils.sign(serializedPayload, signingSecret);

    ApiPaginationToken token = new ApiPaginationToken(serializedPayload, signature);
    String serializedToken = serializeJson(token);

    return Base64.getEncoder().encodeToString(serializedToken.getBytes(StandardCharsets.UTF_8));
  }

  public ApiPaginationTokenPayload deserialize(String serializedPageToken) {
    String tokenJson;

    try {
      tokenJson = new String(Base64.getDecoder().decode(serializedPageToken), "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new BadApiRequestException("Page token encoding invalid");
    }

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
