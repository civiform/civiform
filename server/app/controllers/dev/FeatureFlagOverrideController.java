package controllers.dev;

import featureflags.FeatureFlag;
import featureflags.FeatureFlags;
import javax.inject.Inject;
import play.mvc.Controller;
import play.mvc.Http.HeaderNames;
import play.mvc.Http.Request;
import play.mvc.Result;
import services.DeploymentType;
import views.dev.FeatureFlagView;

/**
 * Allows for overriding of feature flags by an Admin via HTTP request.
 *
 * <p>Overrides are stored in the session cookie and used by {@link FeatureFlags} to control system
 * behavior
 */
public final class FeatureFlagOverrideController extends Controller {

  private final FeatureFlags featureFlags;
  private final FeatureFlagView featureFlagView;
  private final boolean isDevOrStaging;

  @Inject
  public FeatureFlagOverrideController(
      FeatureFlags featureFlags, FeatureFlagView featureFlagView, DeploymentType deploymentType) {
    this.featureFlags = featureFlags;
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

    return redirect(redirectTo).addingToSession(request, flagName, "true");
  }

  public Result disable(Request request, String flagName) {
    if (!isDevOrStaging) {
      return notFound();
    }
    String redirectTo = request.getHeaders().get(HeaderNames.REFERER).orElse("/");
    return redirect(redirectTo).addingToSession(request, flagName, "false");
  }

  /** Returns the status of a feature flag. */
  public Result status(Request request, String FlagName) {
    return ok(
        featureFlags.getFlagEnabled(request, FeatureFlag.getByName(FlagName)) ? "true" : "false");
  }
}
