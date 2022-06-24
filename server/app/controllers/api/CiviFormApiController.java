package controllers.api;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.AccountNonexistentException;
import auth.ApiKeyGrants;
import auth.ProfileUtils;
import auth.UnauthorizedApiRequestException;
import com.fasterxml.jackson.core.JsonFactory;
import controllers.CiviFormController;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import models.ApiKey;
import play.mvc.Http;
import play.mvc.Result;
import repository.ApiKeyRepository;

/**
 * Base class for controllers that handle API requests. Requests that reach an API controller have
 * been authenticated, but they have not yet been authorized. It is the responsibility of every
 * controller action to authorize its own requests, e.g. by calling assertHasProgramReadPermission.
 */
public class CiviFormApiController extends CiviFormController {

  protected final ApiPaginationTokenSerializer apiPaginationTokenSerializer;
  protected final ApiKeyRepository apiKeyRepository;
  protected final ProfileUtils profileUtils;

  @Inject
  public CiviFormApiController(
      ApiPaginationTokenSerializer apiPaginationTokenSerializer,
      ApiKeyRepository apiKeyRepository,
      ProfileUtils profileUtils) {
    this.apiPaginationTokenSerializer = checkNotNull(apiPaginationTokenSerializer);
    this.apiKeyRepository = checkNotNull(apiKeyRepository);
    this.profileUtils = checkNotNull(profileUtils);
  }

  /**
   * This action always returns 200 if a request reaches it. Used for checking validity of an API
   * key.
   */
  public Result checkAuth() {
    return ok();
  }

  protected void assertHasProgramReadPermission(Http.Request request, String programSlug) {
    ApiKey apiKey = getApiKey(request);

    if (!apiKey.getGrants().hasProgramPermission(programSlug, ApiKeyGrants.Permission.READ)) {
      throw new UnauthorizedApiRequestException(apiKey, programSlug);
    }
  }

  protected ApiKey getApiKey(Http.Request request) {
    return profileUtils
        .currentApiKey(request)
        .orElseThrow(() -> new AccountNonexistentException("No API key found for profile"));
  }

  protected CompletionStage<ApiKey> recordApiKeyUsage(Http.Request request) {
    return apiKeyRepository.recordApiKeyUsage(
        getApiKey(request).getKeyId(), request.remoteAddress());
  }

  protected String getResponseJson(
      String payload, Optional<ApiPaginationTokenPayload> paginationTokenPayload) {
    var writer = new StringWriter();

    try {
      var jsonGenerator = new JsonFactory().createGenerator(writer);
      jsonGenerator.writeStartObject();
      jsonGenerator.writeFieldName("payload");
      jsonGenerator.writeRawValue(payload);

      jsonGenerator.writeFieldName("nextPageToken");
      if (paginationTokenPayload.isPresent()) {
        jsonGenerator.writeString(
            apiPaginationTokenSerializer.serialize(paginationTokenPayload.get()));
      } else {
        jsonGenerator.writeNull();
      }

      jsonGenerator.writeEndObject();
      jsonGenerator.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    return writer.toString();
  }
}
