package services;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.VisibleForTesting;
import com.typesafe.config.Config;
import java.net.URI;
import javax.inject.Inject;
import javax.inject.Singleton;
import play.Environment;

/** DeploymentType provides a method to tell if this is a staging deployment or not. */
@Singleton
public final class DeploymentType {
  private final boolean isDev;
  private final boolean isStaging;

  @Inject
  public DeploymentType(Environment env, Config config) {
    checkNotNull(env);
    checkNotNull(config);

    this.isDev = env.isDev();

    String stagingHostname = config.getString("staging_hostname");
    String baseUrl = config.getString("base_url");
    this.isStaging = URI.create(baseUrl).getHost().equals(stagingHostname);
  }

  @VisibleForTesting
  public DeploymentType(boolean isDev, boolean isStaging) {
    this.isDev = isDev;
    this.isStaging = isStaging;
  }

  public boolean isDev() {
    return isDev;
  }

  public boolean isStaging() {
    return isStaging;
  }

  public boolean isDevOrStaging() {
    return isDev || isStaging;
  }
}
