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
import javax.inject.Inject;
import play.mvc.Http;
import play.mvc.Result;

/** Base class for controllers that handle API requests */
public class CiviFormApiController extends CiviFormController {

  protected final ApiPaginationTokenSerializer apiPaginationTokenSerializer;
  protected final ProfileUtils profileUtils;

  @Inject
  public CiviFormApiController(
      ApiPaginationTokenSerializer apiPaginationTokenSerializer, ProfileUtils profileUtils) {
    this.apiPaginationTokenSerializer = checkNotNull(apiPaginationTokenSerializer);
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
    var apiKey =
        profileUtils
            .currentApiKey(request)
            .orElseThrow(() -> new AccountNonexistentException("Invalid API key ID cached"));

    if (!apiKey.getGrants().hasProgramPermission(programSlug, ApiKeyGrants.Permission.READ)) {
      throw new UnauthorizedApiRequestException(apiKey, programSlug);
    }
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
