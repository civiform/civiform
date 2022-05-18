package controllers.api;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.inject.Inject;
import com.typesafe.config.Config;
import services.EncryptionUtils;

public class ApiPaginationTokenSerializer {
  private static final ObjectMapper mapper =
      new ObjectMapper().registerModule(new GuavaModule()).registerModule(new Jdk8Module());

  private final String signingSecret;

  @Inject
  public ApiPaginationTokenSerializer(Config appConfig) {
    this.signingSecret = checkNotNull(appConfig).getString("play.http.secret.key");
  }

  public ApiPaginationTokenPayload deserialize(String serializedPageToken) {
    ApiPaginationToken token = deserializeJson(serializedPageToken, ApiPaginationToken.class);

    var computedPayloadSignature =
        EncryptionUtils.sign(token.getSerializedPayload(), signingSecret);

    if (computedPayloadSignature.equals(token.getSignature())) {
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
}
