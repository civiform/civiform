package controllers.dev;

import auth.Authorizers.Labels;
import com.typesafe.config.Config;
import featureflags.FeatureFlags;
import javax.inject.Inject;
import org.pac4j.play.java.Secure;
import play.Environment;
import play.mvc.Http.Request;
import play.mvc.Result;

/**
 * Allows for overriding of feature flags by an Admin via HTTP request.
 *
 * <p>Overrides are stored in the session cookie and used by {@link FeatureFlags} to control system
 * behavior
 */
public final class FeatureFlagOverrideController extends DevController {

  @Inject
  public FeatureFlagOverrideController(Environment environment, Config configuration) {
    super(environment, configuration);
  }

  @Secure(authorizers = Labels.ANY_ADMIN)
  public Result enable(Request request, String flagName) {
    if (!isDevOrStagingEnvironment()) {
      return notFound();
    }
    String redirectTo = request.getHeaders().get("referer").orElse("/");

    return redirect(redirectTo).addingToSession(request, flagName, "true");
  }

  @Secure(authorizers = Labels.ANY_ADMIN)
  public Result disable(Request request, String flagName) {
    if (!isDevOrStagingEnvironment()) {
      return notFound();
    }
    String redirectTo = request.getHeaders().get("referer").orElse("/");
    return redirect(redirectTo).addingToSession(request, flagName, "false");
  }
}
