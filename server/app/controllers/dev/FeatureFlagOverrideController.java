package controllers.dev;

import annotations.FeatureFlagOverrides;
import auth.Authorizers;
import com.typesafe.config.Config;
import javax.inject.Inject;
import org.pac4j.play.java.Secure;
import play.Environment;
import play.mvc.Http.Request;
import play.mvc.Result;

/** Allows for overriding of feature flags by the CiviForm Admin via HTTP request. */
public final class FeatureFlagOverrideController extends DevController {
  private final FeatureFlagOverrides overrides;

  @Inject
  public FeatureFlagOverrideController(
      FeatureFlagOverrides overrides, Environment environment, Config configuration) {
    super(environment, configuration);
    this.overrides = overrides;
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result enable(Request request, String flagName) {
    if (!isDevOrStagingEnvironment()) {
      return notFound();
    }
    overrides.setOverride(flagName, "true");
    return ok();
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result disable(Request request, String flagName) {
    if (!isDevOrStagingEnvironment()) {
      return notFound();
    }
    overrides.setOverride(flagName, "false");
    return ok();
  }
}
