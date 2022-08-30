package controllers.dev;

import auth.Authorizers.Labels;
import com.typesafe.config.Config;
import featureflags.FeatureFlags;
import javax.inject.Inject;
import org.pac4j.play.java.Secure;
import play.Environment;
import play.mvc.Http.HeaderNames;
import play.mvc.Http.Request;
import play.mvc.Result;

/**
 * Allows for overriding of feature flags by an Admin via HTTP request.
 *
 * <p>Overrides are stored in the session cookie and used by {@link FeatureFlags} to control system
 * behavior
 */
public final class FeatureFlagOverrideController extends DevController {

  private final FeatureFlags featureFlags;

  @Inject
  public FeatureFlagOverrideController(
      Environment environment, Config configuration, FeatureFlags featureFlags) {
    super(environment, configuration);
    this.featureFlags = featureFlags;
  }

  @Secure(authorizers = Labels.CIVIFORM_ADMIN)
  public Result index(Request request) {
    return ok(
        String.format(
            "Overrides are allowed if all are true:\n"
                + "Server environment: %s\n"
                + "Configuration: %s",
            isDevOrStagingEnvironment(), featureFlags.areOverridesEnabled()));
  }

  @Secure(authorizers = Labels.ANY_ADMIN)
  public Result enable(Request request, String flagName) {
    if (!isDevOrStagingEnvironment()) {
      return notFound();
    }
    String redirectTo = request.getHeaders().get(HeaderNames.REFERER).orElse("/");

    return redirect(redirectTo).addingToSession(request, flagName, "true");
  }

  @Secure(authorizers = Labels.ANY_ADMIN)
  public Result disable(Request request, String flagName) {
    if (!isDevOrStagingEnvironment()) {
      return notFound();
    }
    String redirectTo = request.getHeaders().get(HeaderNames.REFERER).orElse("/");
    return redirect(redirectTo).addingToSession(request, flagName, "false");
  }
}
