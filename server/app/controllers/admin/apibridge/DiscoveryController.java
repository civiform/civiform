package controllers.admin.apibridge;

import static autovalue.shaded.com.google.common.base.Preconditions.checkNotNull;

import auth.Authorizers;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.pac4j.play.java.Secure;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import services.settings.SettingsManifest;

/** This controller deals with the discovery and adding of api bridge endpoints. */
@Slf4j
public class DiscoveryController extends Controller {
  private final SettingsManifest settingsManifest;

  @Inject
  public DiscoveryController(SettingsManifest settingsManifest) {

    this.settingsManifest = checkNotNull(settingsManifest);
  }

  /** Show the initial discovery page */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result discovery(Http.Request request) {
    if (!settingsManifest.getApiBridgeEnabled(request)) {
      return notFound();
    }

    throw new NotImplementedException();
  }

  /** Displays the results of api bridge endpoints found by the discovery process. */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public CompletionStage<Result> hxDiscoveryPopulate(Http.Request request) {
    if (!settingsManifest.getApiBridgeEnabled(request)) {
      return CompletableFuture.completedFuture(notFound());
    }

    throw new NotImplementedException();
  }

  /** Save the selected endpoint */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public CompletionStage<Result> hxAdd(Http.Request request) {
    if (!settingsManifest.getApiBridgeEnabled(request)) {
      return CompletableFuture.completedFuture(notFound());
    }

    throw new NotImplementedException();
  }
}
