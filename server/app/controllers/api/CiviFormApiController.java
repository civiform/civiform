package controllers.api;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.AccountNonexistentException;
import auth.ApiKeyGrants;
import auth.ProfileUtils;
import auth.UnauthorizedApiRequestException;
import controllers.CiviFormController;
import models.ApiKey;
import play.mvc.Http;

/**
 * Base class for controllers that handle API requests. Requests that reach an API controller have
 * been authenticated, but they have not yet been authorized. It is the responsibility of every
 * controller action to authorize its own requests, e.g. by calling assertHasProgramReadPermission.
 */
public class CiviFormApiController extends CiviFormController {

  protected ProfileUtils profileUtils;

  protected CiviFormApiController(ProfileUtils profileUtils) {
    this.profileUtils = checkNotNull(profileUtils);
  }

  protected void assertHasProgramReadPermission(Http.Request request, String programSlug) {
    ApiKey apiKey =
        profileUtils
            .currentApiKey(request)
            .orElseThrow(() -> new AccountNonexistentException("No API key found for profile"));

    if (!apiKey.getGrants().hasProgramPermission(programSlug, ApiKeyGrants.Permission.READ)) {
      throw new UnauthorizedApiRequestException(apiKey, programSlug);
    }
  }
}
