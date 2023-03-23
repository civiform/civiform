package modules;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.typesafe.config.Config;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Environment;
import services.geo.esri.EsriClient;

/**
 * Configures the class used for the Esri client. When running in a dev or CI environment, the fake
 * client is used to simulate responses from an Esri service.
 */
public class EsriModule extends AbstractModule {
  private static final String FAKE_ESRI_CLIENT_CLASS_NAME = "services.geo.esri.FakeEsriClient";
  private static final String REAL_ESRI_CLIENT_CLASS_NAME = "services.geo.esri.RealEsriClient";
  private static final ImmutableSet<String> ACCEPTED_HOSTS =
      ImmutableSet.of("localhost", "civiform");
  private static final Logger logger = LoggerFactory.getLogger(EsriModule.class);

  private final Environment environment;
  private final Config config;

  public EsriModule(Environment environment, Config config) {
    this.environment = checkNotNull(environment);
    this.config = checkNotNull(config);
  }

  @Override
  protected void configure() {
    String host = URI.create(config.getString("base_url")).getHost();
    boolean useFakeEsriClient =
        ACCEPTED_HOSTS.stream()
            .anyMatch(
                acceptedHost -> host.equals(acceptedHost) || host.startsWith(acceptedHost + ":"));
    String className =
        useFakeEsriClient ? FAKE_ESRI_CLIENT_CLASS_NAME : REAL_ESRI_CLIENT_CLASS_NAME;
    logger.info(String.format("Using %s class for Esri client", className));
    try {
      Class<? extends EsriClient> bindingClass =
          environment.classLoader().loadClass(className).asSubclass(EsriClient.class);
      bind(EsriClient.class).to(bindingClass);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(
          String.format("Failed to load esri client class: %s", className), e);
    }
  }
}
