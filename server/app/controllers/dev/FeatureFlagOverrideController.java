package controllers.dev;

import java.util.Locale;
import javax.inject.Inject;
import play.mvc.Controller;
import play.mvc.Http.HeaderNames;
import play.mvc.Http.Request;
import play.mvc.Result;
import services.DeploymentType;
import services.settings.SettingsManifest;
import views.dev.FeatureFlagView;

/**
 * Allows for overriding of feature flags by an Admin via HTTP request.
 *
 * <p>Overrides are stored in the session cookie and used by {@link SettingsManifest} to control
 * system behavior
 */
public final class FeatureFlagOverrideController extends Controller {

  private final FeatureFlagView featureFlagView;
  private final SettingsManifest settingsManifest;
  private final boolean isDevOrStaging;

  @Inject
  public FeatureFlagOverrideController(
      SettingsManifest settingsManifest,
      FeatureFlagView featureFlagView,
      DeploymentType deploymentType) {
    this.settingsManifest = settingsManifest;
    this.featureFlagView = featureFlagView;
    this.isDevOrStaging = deploymentType.isDevOrStaging();
  }

  public Result index(Request request) {
    return ok(featureFlagView.render(request, isDevOrStaging));
  }

  public Result enable(Request request, String flagName) {
    if (!isDevOrStaging) {
      return notFound();
    }
    String redirectTo = request.getHeaders().get(HeaderNames.REFERER).orElse("/");

    return redirect(redirectTo).addingToSession(request, flagName.toUpperCase(Locale.ROOT), "true");
  }

  public Result disable(Request request, String flagName) {
    if (!isDevOrStaging) {
      return notFound();
    }
    String redirectTo = request.getHeaders().get(HeaderNames.REFERER).orElse("/");
    return redirect(redirectTo)
        .addingToSession(request, flagName.toUpperCase(Locale.ROOT), "false");
  }

  /** Returns the status of a feature flag. */
  public Result status(Request request, String flagName) {
    return ok(
        settingsManifest.getBool(flagName.toUpperCase(Locale.ROOT), request) ? "true" : "false");
  }
}
