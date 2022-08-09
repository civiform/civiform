package controllers;

import annotations.FeatureFlagOverrides;
import auth.Authorizers;
import javax.inject.Inject;
import org.pac4j.play.java.Secure;
import play.mvc.Http.Request;
import play.mvc.Result;

/**
 * Allows for overriding of feature flags by the CiviForm Admin via HTTP request.
 */
public class FeatureFlagOverrideController extends CiviFormController {
  private final FeatureFlagOverrides overrides;

  @Inject
  public FeatureFlagOverrideController(FeatureFlagOverrides overrides) {
    this.overrides = overrides;
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result enable(Request request, String flagName) {
    overrides.setOverride(flagName, "true");
    return ok();
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result disable(Request request, String flagName) {
    overrides.setOverride(flagName, "false");
    return ok();
  }
}
