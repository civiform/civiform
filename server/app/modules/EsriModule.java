package modules;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.inject.AbstractModule;
import com.typesafe.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Environment;
import services.geo.esri.EsriClient;

/**
 * Configures the class used for the Esri client. When the config value is set for
 * "esri_find_address_candidates_url" we use the RealEsriClient. If the value is not set we'll use
 * the FakeEsriClient. This allows the real client to be used whether we are using an actual Esri
 * endpoint or if using the Mock Web Services.
 */
public final class EsriModule extends AbstractModule {
  private static final String FAKE_ESRI_CLIENT_CLASS_NAME = "services.geo.esri.FakeEsriClient";
  private static final String REAL_ESRI_CLIENT_CLASS_NAME = "services.geo.esri.RealEsriClient";
  private static final Logger LOGGER = LoggerFactory.getLogger(EsriModule.class);

  private final Environment environment;
  private final Config config;

  public EsriModule(Environment environment, Config config) {
    this.environment = checkNotNull(environment);
    this.config = checkNotNull(config);
  }

  @Override
  protected void configure() {
    String url = config.getString("esri_find_address_candidates_url");
    String className = url.isEmpty() ? FAKE_ESRI_CLIENT_CLASS_NAME : REAL_ESRI_CLIENT_CLASS_NAME;

    LOGGER.info(String.format("Using %s class for Esri client", className));

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
