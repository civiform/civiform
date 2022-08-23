package controllers.dev;

import auth.Authorizers;
import com.typesafe.config.Config;
import javax.inject.Inject;
import org.pac4j.play.java.Secure;
import play.Environment;
import play.mvc.Http.Request;
import play.mvc.Result;

/** Allows for overriding of feature flags by the CiviForm Admin via HTTP request. */
public final class FeatureFlagOverrideController extends DevController {

  @Inject
  public FeatureFlagOverrideController(Environment environment, Config configuration) {
    super(environment, configuration);
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result enable(Request request, String flagName) {
    if (!isDevOrStagingEnvironment()) {
      return notFound();
    }
    String redirectTo = request.getHeaders().get("referer").orElse("/");

    return redirect(redirectTo).addingToSession(request, flagName, "true");
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result disable(Request request, String flagName) {
    if (!isDevOrStagingEnvironment()) {
      return notFound();
    }
    String redirectTo = request.getHeaders().get("referer").orElse("/");
    return redirect(redirectTo).addingToSession(request, flagName, "false");
  }
}
