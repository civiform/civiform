package controllers.api;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.AccountNonexistentException;
import auth.ApiKeyGrants;
import auth.ProfileUtils;
import auth.UnauthorizedApiRequestException;
import controllers.CiviFormController;
import play.mvc.Http;

/** Base class for controllers that handle API requests */
public class CiviFormApiController extends CiviFormController {

  protected final ProfileUtils profileUtils;

  public CiviFormApiController(ProfileUtils profileUtils) {
    this.profileUtils = checkNotNull(profileUtils);
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
}
