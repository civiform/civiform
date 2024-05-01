package controllers.api;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.AccountNonexistentException;
import auth.ApiKeyGrants;
import auth.ProfileUtils;
import auth.UnauthorizedApiRequestException;
import controllers.CiviFormController;
import javax.inject.Inject;
import models.ApiKeyModel;
import play.mvc.Http;
import play.mvc.Result;
import repository.VersionRepository;

/**
 * Base class for controllers that handle API requests. Requests that reach an API controller have
 * been authenticated, but they have not yet been authorized. It is the responsibility of every
 * controller action to authorize its own requests, e.g. by calling assertHasProgramReadPermission.
 */
public class CiviFormApiController extends CiviFormController {

  protected final ApiPaginationTokenSerializer apiPaginationTokenSerializer;
  protected final ApiPayloadWrapper apiPayloadWrapper;

  @Inject
  public CiviFormApiController(
      ApiPaginationTokenSerializer apiPaginationTokenSerializer,
      ApiPayloadWrapper apiPayloadWrapper,
      ProfileUtils profileUtils,
      VersionRepository versionRepository) {
    super(profileUtils, versionRepository);
    this.apiPaginationTokenSerializer = checkNotNull(apiPaginationTokenSerializer);
    this.apiPayloadWrapper = checkNotNull(apiPayloadWrapper);
  }

  /**
   * This action always returns 200 if a request reaches it. Used for checking validity of an API
   * key.
   */
  public Result checkAuth() {
    return ok();
  }

  protected void assertHasProgramReadPermission(Http.Request request, String programSlug) {
    ApiKeyModel apiKey =
        profileUtils
            .currentApiKey(request)
            .orElseThrow(() -> new AccountNonexistentException("No API key found for profile"));

    if (!apiKey.getGrants().hasProgramPermission(programSlug, ApiKeyGrants.Permission.READ)) {
      throw new UnauthorizedApiRequestException(apiKey, programSlug);
    }
  }
}
