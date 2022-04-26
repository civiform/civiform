package controllers.dev;

import static com.google.common.base.Preconditions.checkNotNull;

import com.typesafe.config.Config;
import java.net.URI;
import play.Environment;
import play.mvc.Controller;

/** Superclass for dev controllers. */
public class DevController extends Controller {
  private final Environment environment;
  private final String hostName;
  private final String stagingHostname;

  public DevController(Environment environment, Config configuration) {
    this.environment = checkNotNull(environment);
    stagingHostname = checkNotNull(configuration).getString("staging_hostname");

    String baseUrl = checkNotNull(configuration).getString("base_url");
    this.hostName = URI.create(baseUrl).getHost();
  }

  public boolean isDevEnvironment() {
    if (environment.isDev()) {
      return true;
    }
    // Allow staging to use some dev features for testing purpose.
    if (isStaging()) {
      return true;
    }
    return false;
  }

  public boolean isStaging() {
    return hostName.equals(stagingHostname);
  }
}
