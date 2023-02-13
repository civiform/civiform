package controllers.dev;

import com.typesafe.config.Config;
import featureflags.FeatureFlags;
import javax.inject.Inject;
import play.Environment;
import play.mvc.Http.HeaderNames;
import play.mvc.Http.Request;
import play.mvc.Result;
import views.dev.FeatureFlagView;

/**
 * Allows for overriding of feature flags by an Admin via HTTP request.
 *
 * <p>Overrides are stored in the session cookie and used by {@link FeatureFlags} to control system
 * behavior
 */
public final class FeatureFlagOverrideController extends DevController {

  private final FeatureFlagView featureFlagView;

  @Inject
  public FeatureFlagOverrideController(
      Environment environment, Config configuration, FeatureFlagView featureFlagView) {
    super(environment, configuration);
    this.featureFlagView = featureFlagView;
  }

  public Result index(Request request) {
    return ok(featureFlagView.render(request, isDevOrStagingEnvironment()));
  }

  public Result enable(Request request, String flagName) {
    if (!isDevOrStagingEnvironment()) {
      return notFound();
    }
    String redirectTo = request.getHeaders().get(HeaderNames.REFERER).orElse("/");

    return redirect(redirectTo).addingToSession(request, flagName, "true");
  }

  public Result disable(Request request, String flagName) {
    if (!isDevOrStagingEnvironment()) {
      return notFound();
    }
    String redirectTo = request.getHeaders().get(HeaderNames.REFERER).orElse("/");
    return redirect(redirectTo).addingToSession(request, flagName, "false");
  }
}
